package com.gem.bingelock

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.content.Context
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BingeAccessibilityService : AccessibilityService() {

    private val TAG = "BingeLock"
    private val targetPackages = setOf(
        "com.google.android.youtube",
        "app.morphe.android.youtube"
    )

    // The phrases that indicate a "Still Watching" dialog is visible
    private val promptPhrases = listOf(
        "Video paused. Continue watching?",
        "Are you still watching?",
        "Still there?",
        "Video paused",
        "Video Paused",
        "Still watching? Video will pause soon.",
        "Are you still there?",
        "Click to resume playback.",
        "Paused due to inactivity.",
        "Resume video?"
    )

    // The specific button labels to click once a prompt is detected
    private val actionButtons = listOf(
        "Yes",
        "Continue",
        "Continue watching",
        "CONTINUE"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in targetPackages) return

        val root = rootInActiveWindow ?: return

        try {
            // Check if ANY of our trigger phrases are visible on the screen right now
            var promptDetected = false
            var detectedText = ""
            
            for (phrase in promptPhrases) {
                val nodes = root.findAccessibilityNodeInfosByText(phrase)
                if (!nodes.isNullOrEmpty()) {
                    promptDetected = true
                    detectedText = phrase
                    // Clean up nodes
                    for (n in nodes) n?.recycle()
                    break
                }
            }

            if (promptDetected) {
                Log.d(TAG, "Prompt detected: \"$detectedText\". Searching for action button...")

                // 1. First attempt: Look for specific clickable buttons (Yes/Continue)
                for (btnLabel in actionButtons) {
                    val btnNodes = root.findAccessibilityNodeInfosByText(btnLabel)
                    if (!btnNodes.isNullOrEmpty()) {
                        for (node in btnNodes) {
                            if (node == null) continue
                            val target = findClickableTarget(node)
                            if (target != null) {
                                val ok = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                Log.d(TAG, "Clicked button \"$btnLabel\" - Success: $ok")
                                if (ok) {
                                    handleSuccess(btnLabel, pkg)
                                    // Cleanup all nodes before returning
                                    if (target != node) target.recycle()
                                    for (n in btnNodes) n?.recycle()
                                    return
                                }
                                if (target != node) target.recycle()
                            }
                            node.recycle()
                        }
                    }
                }

                // 2. Fallback: If no "Yes" button found, try clicking the prompt text area itself
                val promptNodes = root.findAccessibilityNodeInfosByText(detectedText)
                if (!promptNodes.isNullOrEmpty()) {
                    for (node in promptNodes) {
                        if (node == null) continue
                        val target = findClickableTarget(node)
                        if (target != null) {
                            val ok = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            Log.d(TAG, "Clicked prompt layout fallback - Success: $ok")
                            if (ok) {
                                handleSuccess(detectedText, pkg)
                                if (target != node) target.recycle()
                                for (n in promptNodes) n?.recycle()
                                return
                            }
                            if (target != node) target.recycle()
                        }
                        node.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event", e)
        } finally {
            root.recycle()
        }
    }

    private fun findClickableTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isClickable) return node
        val parent = node.parent
        if (parent != null) {
            if (parent.isClickable) return parent
            val grandParent = parent.parent
            if (grandParent != null && grandParent.isClickable) {
                parent.recycle()
                return grandParent
            }
            parent.recycle()
        }
        return null
    }

    private fun handleSuccess(text: String, pkg: String) {
        val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        saveLogToPrefs("[$ts] Auto-dismissed \"$text\" on $pkg")
        sendDismissalNotification(text, pkg)
    }

    private fun dumpNodeTree(node: AccessibilityNodeInfo, depth: Int) {
        if (depth > 8) return
        val indent = "  ".repeat(depth)
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""
        if (text.isNotEmpty() || desc.isNotEmpty()) {
            Log.d(TAG, "$indent[${node.className}] text=\"$text\" desc=\"$desc\" clickable=${node.isClickable}")
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            dumpNodeTree(child, depth + 1)
            child.recycle()
        }
    }

    override fun onInterrupt() {
        // Empty body
    }

    private fun saveLogToPrefs(message: String) {
        try {
            val prefs = getSharedPreferences("bingelock", Context.MODE_PRIVATE)
            val currentLogs = prefs.getString("savedLogs", "") ?: ""
            val lines = (message + "\n" + currentLogs).lines().take(100)
            prefs.edit().putString("savedLogs", lines.joinToString("\n")).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving history log to SharedPreferences", e)
        }
    }

    private fun sendDismissalNotification(promptText: String, packageName: String) {
        try {
            val appLabel = if (packageName.contains("morphe")) "YouTube Morphe" else "YouTube"
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            
            // Build temporary heads-up alert notification
            val notification = NotificationCompat.Builder(this, "bingelock_channel")
                .setContentTitle("BingeLock Triggered")
                .setContentText("Automatically dismissed \"$promptText\" prompt in $appLabel.")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            // Issue with a distinct ID (e.g. 2) so it doesn't overwrite the persistent foreground service notification (ID 1)
            manager.notify(2, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending dismissal notification alert", e)
        }
    }
}
