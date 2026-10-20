package com.lightcraft.internetspeedmeter.utils

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("speed_meter_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_SERVICE_ENABLED = "key_service_enabled"
        private const val KEY_SPEED_UNIT_BITS = "key_speed_unit_bits"
        private const val KEY_UPDATE_INTERVAL_MS = "key_update_interval_ms"
        private const val KEY_SCREEN_OFF_MODE = "key_screen_off_mode"
        private const val KEY_STATUS_BAR_ICON_MODE = "key_status_bar_icon_mode"
        private const val KEY_AUTOSTART_ON_BOOT = "key_autostart_on_boot"

        // Screen Off Modes
        const val SCREEN_OFF_KEEP = 0
        const val SCREEN_OFF_SLOW = 1
        const val SCREEN_OFF_PAUSE = 2

        // Icon Modes
        const val ICON_MODE_NUMBER = 0
        const val ICON_MODE_APP = 1
        const val ICON_MODE_NONE = 2

        // Stats keys
        private const val KEY_LAST_DATE = "key_last_date"
        private const val KEY_LAST_MONTH = "key_last_month"

        private const val KEY_TODAY_WIFI_RX = "key_today_wifi_rx"
        private const val KEY_TODAY_WIFI_TX = "key_today_wifi_tx"
        private const val KEY_TODAY_MOBILE_RX = "key_today_mobile_rx"
        private const val KEY_TODAY_MOBILE_TX = "key_today_mobile_tx"

        private const val KEY_MONTH_WIFI_RX = "key_month_wifi_rx"
        private const val KEY_MONTH_WIFI_TX = "key_month_wifi_tx"
        private const val KEY_MONTH_MOBILE_RX = "key_month_mobile_rx"
        private const val KEY_MONTH_MOBILE_TX = "key_month_mobile_tx"
    }

    var isServiceEnabled: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SERVICE_ENABLED, value).apply()

    var useBitsPerSecond: Boolean
        get() = prefs.getBoolean(KEY_SPEED_UNIT_BITS, false)
        set(value) = prefs.edit().putBoolean(KEY_SPEED_UNIT_BITS, value).apply()

    var updateIntervalMs: Long
        get() = prefs.getLong(KEY_UPDATE_INTERVAL_MS, 1000L)
        set(value) = prefs.edit().putLong(KEY_UPDATE_INTERVAL_MS, value).apply()

    var screenOffMode: Int
        get() = prefs.getInt(KEY_SCREEN_OFF_MODE, SCREEN_OFF_SLOW)
        set(value) = prefs.edit().putInt(KEY_SCREEN_OFF_MODE, value).apply()

    var statusBarIconMode: Int
        get() = prefs.getInt(KEY_STATUS_BAR_ICON_MODE, ICON_MODE_NUMBER)
        set(value) = prefs.edit().putInt(KEY_STATUS_BAR_ICON_MODE, value).apply()

    var autostartOnBoot: Boolean
        get() = prefs.getBoolean(KEY_AUTOSTART_ON_BOOT, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTOSTART_ON_BOOT, value).apply()

    // Date Verification & Usage Increments
    fun checkAndResetUsageIfNeeded() {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)

        val lastDate = prefs.getString(KEY_LAST_DATE, "")
        val lastMonth = prefs.getString(KEY_LAST_MONTH, "")

        val editor = prefs.edit()

        if (lastDate != currentDate) {
            editor.putString(KEY_LAST_DATE, currentDate)
            editor.putLong(KEY_TODAY_WIFI_RX, 0L)
            editor.putLong(KEY_TODAY_WIFI_TX, 0L)
            editor.putLong(KEY_TODAY_MOBILE_RX, 0L)
            editor.putLong(KEY_TODAY_MOBILE_TX, 0L)
        }

        if (lastMonth != currentMonth) {
            editor.putString(KEY_LAST_MONTH, currentMonth)
            editor.putLong(KEY_MONTH_WIFI_RX, 0L)
            editor.putLong(KEY_MONTH_WIFI_TX, 0L)
            editor.putLong(KEY_MONTH_MOBILE_RX, 0L)
            editor.putLong(KEY_MONTH_MOBILE_TX, 0L)
        }

        editor.apply()
    }

    fun addWifiUsage(rxDelta: Long, txDelta: Long) {
        checkAndResetUsageIfNeeded()
        if (rxDelta <= 0 && txDelta <= 0) return

        val editor = prefs.edit()
        if (rxDelta > 0) {
            editor.putLong(KEY_TODAY_WIFI_RX, todayWifiRx + rxDelta)
            editor.putLong(KEY_MONTH_WIFI_RX, monthWifiRx + rxDelta)
        }
        if (txDelta > 0) {
            editor.putLong(KEY_TODAY_WIFI_TX, todayWifiTx + txDelta)
            editor.putLong(KEY_MONTH_WIFI_TX, monthWifiTx + txDelta)
        }
        editor.apply()
    }

    fun addMobileUsage(rxDelta: Long, txDelta: Long) {
        checkAndResetUsageIfNeeded()
        if (rxDelta <= 0 && txDelta <= 0) return

        val editor = prefs.edit()
        if (rxDelta > 0) {
            editor.putLong(KEY_TODAY_MOBILE_RX, todayMobileRx + rxDelta)
            editor.putLong(KEY_MONTH_MOBILE_RX, monthMobileRx + rxDelta)
        }
        if (txDelta > 0) {
            editor.putLong(KEY_TODAY_MOBILE_TX, todayMobileTx + txDelta)
            editor.putLong(KEY_MONTH_MOBILE_TX, monthMobileTx + txDelta)
        }
        editor.apply()
    }

    val todayWifiRx: Long get() = prefs.getLong(KEY_TODAY_WIFI_RX, 0L)
    val todayWifiTx: Long get() = prefs.getLong(KEY_TODAY_WIFI_TX, 0L)
    val todayMobileRx: Long get() = prefs.getLong(KEY_TODAY_MOBILE_RX, 0L)
    val todayMobileTx: Long get() = prefs.getLong(KEY_TODAY_MOBILE_TX, 0L)

    val monthWifiRx: Long get() = prefs.getLong(KEY_MONTH_WIFI_RX, 0L)
    val monthWifiTx: Long get() = prefs.getLong(KEY_MONTH_WIFI_TX, 0L)
    val monthMobileRx: Long get() = prefs.getLong(KEY_MONTH_MOBILE_RX, 0L)
    val monthMobileTx: Long get() = prefs.getLong(KEY_MONTH_MOBILE_TX, 0L)

    val todayWifiTotal: Long get() = todayWifiRx + todayWifiTx
    val todayMobileTotal: Long get() = todayMobileRx + todayMobileTx
    val todayTotal: Long get() = todayWifiTotal + todayMobileTotal

    val monthWifiTotal: Long get() = monthWifiRx + monthWifiTx
    val monthMobileTotal: Long get() = monthMobileRx + monthMobileTx
    val monthTotal: Long get() = monthWifiTotal + monthMobileTotal

    fun resetAllStats() {
        prefs.edit()
            .putLong(KEY_TODAY_WIFI_RX, 0L)
            .putLong(KEY_TODAY_WIFI_TX, 0L)
            .putLong(KEY_TODAY_MOBILE_RX, 0L)
            .putLong(KEY_TODAY_MOBILE_TX, 0L)
            .putLong(KEY_MONTH_WIFI_RX, 0L)
            .putLong(KEY_MONTH_WIFI_TX, 0L)
            .putLong(KEY_MONTH_MOBILE_RX, 0L)
            .putLong(KEY_MONTH_MOBILE_TX, 0L)
            .apply()
    }
}
