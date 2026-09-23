package com.gem.bingelock

import android.content.Context

class PrefsHelper(context: Context) {
    private val prefs = context.getSharedPreferences("bingelock", Context.MODE_PRIVATE)

    companion object {
        val DEFAULT_PROMPT_PHRASES = listOf(
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
        ).joinToString("\n")

        val DEFAULT_ACTION_BUTTONS = listOf(
            "Yes",
            "Continue",
            "Continue watching",
            "CONTINUE"
        ).joinToString("\n")
    }

    var masterEnabled: Boolean
        get() = prefs.getBoolean("masterEnabled", false)
        set(value) = prefs.edit().putBoolean("masterEnabled", value).apply()

    var batteryFloor: Int
        get() = prefs.getInt("batteryFloor", 15)
        set(value) = prefs.edit().putInt("batteryFloor", value).apply()

    var faceDownEnabled: Boolean
        get() = prefs.getBoolean("faceDownEnabled", true)
        set(value) = prefs.edit().putBoolean("faceDownEnabled", value).apply()

    var faceDownDurationMs: Long
        get() = prefs.getLong("faceDownDurationMs", 10_000L)
        set(value) = prefs.edit().putLong("faceDownDurationMs", value).apply()

    var targetPackages: Set<String>
        get() = prefs.getStringSet(
            "targetPackages",
            setOf("com.google.android.youtube", "app.morphe.android.youtube")
        ) ?: setOf("com.google.android.youtube", "app.morphe.android.youtube")
        set(value) = prefs.edit().putStringSet("targetPackages", value).apply()

    var promptPhrases: String
        get() = prefs.getString("promptPhrases", DEFAULT_PROMPT_PHRASES) ?: DEFAULT_PROMPT_PHRASES
        set(value) = prefs.edit().putString("promptPhrases", value).apply()

    var actionButtons: String
        get() = prefs.getString("actionButtons", DEFAULT_ACTION_BUTTONS) ?: DEFAULT_ACTION_BUTTONS
        set(value) = prefs.edit().putString("actionButtons", value).apply()

    var savedLogs: String
        get() = prefs.getString("savedLogs", "") ?: ""
        set(value) = prefs.edit().putString("savedLogs", value).apply()

    var faceDownPaused: Boolean
        get() = prefs.getBoolean("faceDownPaused", false)
        set(value) = prefs.edit().putBoolean("faceDownPaused", value).apply()

    fun resetToDefaults() {
        batteryFloor = 15
        faceDownEnabled = true
        faceDownDurationMs = 10_000L
        targetPackages = setOf("com.google.android.youtube", "app.morphe.android.youtube")
        faceDownPaused = false
        promptPhrases = DEFAULT_PROMPT_PHRASES
        actionButtons = DEFAULT_ACTION_BUTTONS
    }
}
