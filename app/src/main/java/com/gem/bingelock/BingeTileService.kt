package com.gem.bingelock

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.N)
class BingeTileService : TileService() {

    companion object {
        private const val TAG = "BingeLock"
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val prefs = PrefsHelper(this)
        val newState = !prefs.masterEnabled
        Log.d(TAG, "Tile clicked. New state: $newState")
        prefs.masterEnabled = newState
        prefs.faceDownPaused = false

        val intent = Intent(this, BingeService::class.java)
        if (newState) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } else {
            stopService(intent)
        }
        updateTileState()
    }

    private fun updateTileState() {
        val prefs = PrefsHelper(this)
        val tile = qsTile ?: return
        tile.state = if (prefs.masterEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "BingeLock"
        tile.updateTile()
    }
}
