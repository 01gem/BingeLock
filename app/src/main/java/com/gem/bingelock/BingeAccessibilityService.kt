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
    private val promptStrings = listOf(
        "Video paused. Continue watching?",
        "Are you still watching?",
        "Still there?"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in targetPackages) return

        Log.d(TAG, "EVENT from $pkg type=${event.eventType} class=${event.className}")

        val root = rootInActiveWindow ?: run {
            Log.d(TAG, "  rootInActiveWindow was null")
            return
        }

        try {
            // Disabled log flooding tree walk to optimize performance on 2GB RAM
            // dumpNodeTree(root, 0)

            for (text in promptStrings) {
                val nodes = root.findAccessibilityNodeInfosByText(text) ?: continue
                if (nodes.isEmpty()) continue

                Log.d(TAG, "  MATCH for \"$text\" — ${nodes.size} node(s)")
                for (node in nodes) {
                    if (node == null) continue
                    Log.d(TAG, "    node text=${node.text} clickable=${node.isClickable} class=${node.className}")

                    val target = when {
                        node.isClickable -> node
                        node.parent?.isClickable == true -> node.parent
                        node.parent?.parent?.isClickable == true -> node.parent.parent
                        else -> null
                    }

                    if (target != null) {
                        val ok = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        Log.d(TAG, "    CLICK result=$ok")
                        if (ok) {
                            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                            saveLogToPrefs("[$ts] Auto-dismissed \"$text\" on $pkg")
                            sendDismissalNotification(text, pkg)
                        }
                    } else {
                        Log.d(TAG, "    no clickable target found in 3 levels")
                    }
                }
                for (n in nodes) n?.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing event", e)
        } finally {
            root.recycle()
        }
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
