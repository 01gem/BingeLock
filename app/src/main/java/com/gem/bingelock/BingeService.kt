package com.gem.bingelock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class BingeService : Service() {

    companion object {
        private const val TAG = "BingeLock"
        private const val CHANNEL_ID = "bingelock_channel"
        private const val NOTIFICATION_ID = 1
        private const val BATTERY_FLOOR = 15
        private const val FACE_DOWN_THRESHOLD = -9.0f
        private const val FACE_DOWN_DURATION_MS = 10_000L
    }

    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var prefs: PrefsHelper? = null

    private var sensorManager: SensorManager? = null
    private var faceDownSince: Long = 0L

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level < 0 || scale <= 0) return
            val pct = (level * 100 / scale.toFloat()).toInt()
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0

            Log.d(TAG, "Battery: $pct% plugged=$plugged")

            if (pct < BATTERY_FLOOR && !plugged) {
                Log.w(TAG, "Battery floor reached ($pct%). Disabling BingeLock.")
                prefs?.masterEnabled = false
                prefs?.faceDownPaused = false
                stopSelf()
            }
        }
    }

    private val accelListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val z = event.values[2]
            val currentlyFaceDown = z < FACE_DOWN_THRESHOLD

            if (currentlyFaceDown) {
                if (faceDownSince == 0L) {
                    faceDownSince = System.currentTimeMillis()
                } else if (System.currentTimeMillis() - faceDownSince > FACE_DOWN_DURATION_MS) {
                    if (overlayView != null) {
                        Log.d(TAG, "Face-down sustained. Pausing overlay.")
                        removeOverlay()
                        prefs?.faceDownPaused = true
                        updateNotification()
                    }
                }
            } else {
                if (faceDownSince != 0L) {
                    Log.d(TAG, "Device is upright. Resuming.")
                    faceDownSince = 0L
                    if (overlayView == null) {
                        addOverlay()
                        prefs?.faceDownPaused = false
                        updateNotification()
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")

        prefs = PrefsHelper(this)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        addOverlay()

        // Battery receiver — sticky intent delivers the current level immediately
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Accelerometer — NORMAL rate is plenty for face-down detection
        val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accel != null) {
            sensorManager?.registerListener(accelListener, accel, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Accelerometer listener registered")
        } else {
            Log.w(TAG, "No accelerometer found on this device")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "Service onDestroy")
        try { unregisterReceiver(batteryReceiver) } catch (e: Exception) { /* already unregistered */ }
        sensorManager?.unregisterListener(accelListener)
        removeOverlay()
        prefs?.faceDownPaused = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun addOverlay() {
        if (overlayView != null) return
        try {
            val params = WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSPARENT
            )
            overlayView = View(this)
            windowManager?.addView(overlayView, params)
            Log.d(TAG, "Overlay added")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding overlay", e)
            overlayView = null
        }
    }

    private fun removeOverlay() {
        val view = overlayView ?: return
        try {
            windowManager?.removeView(view)
            Log.d(TAG, "Overlay removed")
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay", e)
        } finally {
            overlayView = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BingeLock Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val paused = prefs?.faceDownPaused == true
        val text = if (paused) "Paused — device is face-down" else "Preventing screen sleep"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BingeLock active")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }
}
