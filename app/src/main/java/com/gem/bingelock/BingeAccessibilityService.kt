package com.gem.bingelock

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("AccessibilityService")
class BingeAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "BingeLock"
    }

    private var prefsHelper: PrefsHelper? = null
    private var lastDismissTime: Long = 0L
    private val DISMISS_DEBOUNCE_MS = 2000L

    override fun onCreate() {
        super.onCreate()
        prefsHelper = PrefsHelper(this)
    }

    private fun getTargetPackages(): Set<String> {
        val helper = prefsHelper ?: PrefsHelper(this).also { prefsHelper = it }
        return helper.targetPackages
    }

    private fun getPromptPhrases(): List<String> {
        val helper = prefsHelper ?: PrefsHelper(this).also { prefsHelper = it }
        return helper.promptPhrases
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun getActionButtons(): List<String> {
        val helper = prefsHelper ?: PrefsHelper(this).also { prefsHelper = it }
        return helper.actionButtons
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun dispatchMediaPlay() {
        try {
            val am = getSystemService(AUDIO_SERVICE) as AudioManager
            val down = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY)
            val up = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY)
            am.dispatchMediaKeyEvent(down)
            am.dispatchMediaKeyEvent(up)
            Log.d(TAG, "Dispatched KEYCODE_MEDIA_PLAY")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch media key", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in getTargetPackages()) return

        val root = rootInActiveWindow ?: return

        try {
            // Check if ANY of our trigger phrases are visible on the screen right now
            var promptDetected = false
            var detectedText = ""
            
            for (phrase in getPromptPhrases()) {
                val nodes = root.findAccessibilityNodeInfosByText(phrase)
                if (!nodes.isNullOrEmpty()) {
                    promptDetected = true
                    detectedText = phrase
                    break
                }
            }

            if (promptDetected) {
                Log.d(TAG, "Prompt detected: \"$detectedText\". Searching for action button...")

                // 1. First attempt: Look for specific clickable buttons (Yes/Continue)
                for (btnLabel in getActionButtons()) {
                    val btnNodes = root.findAccessibilityNodeInfosByText(btnLabel)
                    if (!btnNodes.isNullOrEmpty()) {
                        for (node in btnNodes) {
                            if (node == null) continue
                            val target = findClickableTarget(node)
                            if (target != null) {
                                val ok = target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                Log.d(TAG, "Clicked button \"$btnLabel\" - Success: $ok")

                                // Safety net: always send a media-play key
                                if (System.currentTimeMillis() - lastDismissTime > DISMISS_DEBOUNCE_MS) {
                                    lastDismissTime = System.currentTimeMillis()
                                    dispatchMediaPlay()
                                }

                                if (ok) {
                                    handleSuccess(btnLabel, pkg)
                                    return
                                }
                            }
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

                            // Safety net
                            if (System.currentTimeMillis() - lastDismissTime > DISMISS_DEBOUNCE_MS) {
                                lastDismissTime = System.currentTimeMillis()
                                dispatchMediaPlay()
                            }

                            if (ok) {
                                handleSuccess(detectedText, pkg)
                                return
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing accessibility event", e)
        }
    }

    private fun findClickableTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isClickable) return node
        val parent = node.parent
        if (parent != null) {
            if (parent.isClickable) return parent
            val grandParent = parent.parent
            if (grandParent != null && grandParent.isClickable) {
                return grandParent
            }
        }
        return null
    }

    private fun handleSuccess(text: String, pkg: String) {
        val ts = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault()).format(Date())
        saveLogToPrefs("[$ts] Auto-dismissed \"$text\" on $pkg")
        sendDismissalNotification(text, pkg)
    }

    override fun onInterrupt() {
        // Empty body
    }

    private fun saveLogToPrefs(message: String) {
        try {
            val prefs = getSharedPreferences("bingelock", MODE_PRIVATE)
            val currentLogs = prefs.getString("savedLogs", "") ?: ""
            val lines = (message + "\n" + currentLogs).lines().take(100)
            prefs.edit {
                putString("savedLogs", lines.joinToString("\n"))
            }
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
