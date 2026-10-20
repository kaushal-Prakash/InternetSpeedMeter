package com.lightcraft.internetspeedmeter.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lightcraft.internetspeedmeter.service.SpeedMeterService
import com.lightcraft.internetspeedmeter.utils.AppPreferences

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val prefs = AppPreferences(context)
            if (prefs.isServiceEnabled && prefs.autostartOnBoot) {
                SpeedMeterService.startService(context)
            }
        }
    }
}
