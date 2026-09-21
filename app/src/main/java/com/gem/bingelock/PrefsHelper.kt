package com.gem.bingelock

import android.content.Context

class PrefsHelper(context: Context) {
    private val prefs = context.getSharedPreferences("bingelock", Context.MODE_PRIVATE)

    var masterEnabled: Boolean
        get() = prefs.getBoolean("masterEnabled", false)
        set(value) = prefs.edit().putBoolean("masterEnabled", value).apply()

    var batteryFloor: Int
        get() = prefs.getInt("batteryFloor", 15)
        set(value) = prefs.edit().putInt("batteryFloor", value).apply()

    var targetPackages: Set<String>
        get() = prefs.getStringSet("targetPackages", setOf("com.google.android.youtube")) ?: setOf("com.google.android.youtube")
        set(value) = prefs.edit().putStringSet("targetPackages", value).apply()

    var savedLogs: String
        get() = prefs.getString("savedLogs", "") ?: ""
        set(value) = prefs.edit().putString("savedLogs", value).apply()

    var faceDownPaused: Boolean
        get() = prefs.getBoolean("faceDownPaused", false)
        set(value) = prefs.edit().putBoolean("faceDownPaused", value).apply()
}
