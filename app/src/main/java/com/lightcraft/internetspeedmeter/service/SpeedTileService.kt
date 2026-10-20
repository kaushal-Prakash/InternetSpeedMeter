package com.lightcraft.internetspeedmeter.service

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.lightcraft.internetspeedmeter.utils.AppPreferences

class SpeedTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val prefs = AppPreferences(this)
        val newState = !prefs.isServiceEnabled
        prefs.isServiceEnabled = newState

        if (newState) {
            SpeedMeterService.startService(this)
        } else {
            SpeedMeterService.stopService(this)
        }

        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val prefs = AppPreferences(this)
        val isEnabled = prefs.isServiceEnabled

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tile.state = if (isEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.updateTile()
        }
    }
}
