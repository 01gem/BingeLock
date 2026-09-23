package com.gem.bingelock

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {

    private lateinit var prefsHelper: PrefsHelper
    private lateinit var masterSwitch: SwitchCompat
    private lateinit var statusText: TextView
    private lateinit var btnSettings: Button
    private lateinit var historyLogText: TextView
    private lateinit var btnClearLogs: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefsHelper = PrefsHelper(this)

        masterSwitch = findViewById(R.id.masterSwitch)
        statusText = findViewById(R.id.statusText)
        btnSettings = findViewById(R.id.btnSettings)
        historyLogText = findViewById(R.id.historyLogText)
        btnClearLogs = findViewById(R.id.btnClearLogs)

        // Display current logs if they exist
        val logs = prefsHelper.savedLogs
        if (logs.isNotEmpty()) {
            historyLogText.text = logs
        }

        btnClearLogs.setOnClickListener {
            Log.d("BingeLock", "Clear logs button clicked")
            prefsHelper.savedLogs = ""
            historyLogText.text = "No dismissal actions executed yet."
        }

        // Read PrefsHelper to set the initial state of the master Switch
        masterSwitch.isChecked = prefsHelper.masterEnabled
        updateStatusText()

        masterSwitch.setOnCheckedChangeListener { _, isChecked ->
            Log.d("BingeLock", "Master switch toggled: $isChecked")
            if (isChecked) {
                if (!Settings.canDrawOverlays(this)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                }
                prefsHelper.masterEnabled = true
                prefsHelper.faceDownPaused = false

                val serviceIntent = Intent(this, BingeService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            } else {
                prefsHelper.masterEnabled = false
                prefsHelper.faceDownPaused = false
                stopService(Intent(this, BingeService::class.java))
            }
            updateStatusText()
        }

        btnSettings.setOnClickListener {
            Log.d("BingeLock", "Settings button clicked")
        }
    }

    private fun updateStatusText() {
        statusText.text = when {
            !prefsHelper.masterEnabled -> "BingeLock is OFF"
            prefsHelper.faceDownPaused -> "BingeLock is ON (paused — face-down)"
            else -> "BingeLock is ON"
        }
    }

    override fun onResume() {
        super.onResume()
        masterSwitch.isChecked = prefsHelper.masterEnabled
        updateStatusText()
        val logs = prefsHelper.savedLogs
        if (logs.isNotEmpty()) {
            historyLogText.text = logs
        } else {
            historyLogText.text = "No dismissal actions executed yet."
        }
    }
}
