package com.lightcraft.internetspeedmeter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.lightcraft.internetspeedmeter.R
import com.lightcraft.internetspeedmeter.utils.AppPreferences
import com.lightcraft.internetspeedmeter.utils.NetworkType
import com.lightcraft.internetspeedmeter.utils.NetworkUtils
import com.lightcraft.internetspeedmeter.utils.NotificationIconGenerator

class SpeedMeterService : Service() {

    companion object {
        const val CHANNEL_ID = "speed_meter_channel_v1"
        const val NOTIFICATION_ID = 1001
        const val ACTION_SPEED_UPDATED = "com.lightcraft.internetspeedmeter.SPEED_UPDATED"
        const val EXTRA_DOWNLOAD_SPEED = "extra_download_speed"
        const val EXTRA_UPLOAD_SPEED = "extra_upload_speed"
        const val EXTRA_NETWORK_TYPE = "extra_network_type"

        const val ACTION_STOP_SERVICE = "com.lightcraft.internetspeedmeter.STOP_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, SpeedMeterService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SpeedMeterService::class.java)
            context.stopService(intent)
        }
    }

    private lateinit var prefs: AppPreferences
    private lateinit var notificationManager: NotificationManager
    private val handler = Handler(Looper.getMainLooper())

    private var isScreenOn = true
    private var isSampling = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    isScreenOn = false
                    handleScreenStateChange()
                }
                Intent.ACTION_SCREEN_ON -> {
                    isScreenOn = true
                    handleScreenStateChange()
                }
            }
        }
    }

    private val sampleRunnable = object : Runnable {
        override fun run() {
            if (!isSampling) return

            performSampleAndUpdate()

            val interval = getNextInterval()
            if (interval > 0) {
                handler.postDelayed(this, interval)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefs = AppPreferences(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        registerScreenReceiver()

        startForegroundServiceWithNotification(
            buildNotification("0 B/s", "0 B/s", NetworkType.NONE, 0L, 0L)
        )

        startSampling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            prefs.isServiceEnabled = false
            stopSelf()
            return START_NOT_STICKY
        }

        if (!prefs.isServiceEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }

        startSampling()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopSampling()
        unregisterScreenReceiver()
        super.onDestroy()
    }

    private fun startSampling() {
        if (!isSampling) {
            isSampling = true
            handler.removeCallbacks(sampleRunnable)
            handler.post(sampleRunnable)
        }
    }

    private fun stopSampling() {
        isSampling = false
        handler.removeCallbacks(sampleRunnable)
    }

    private fun handleScreenStateChange() {
        when (prefs.screenOffMode) {
            AppPreferences.SCREEN_OFF_PAUSE -> {
                if (isScreenOn) startSampling() else stopSampling()
            }
            AppPreferences.SCREEN_OFF_SLOW -> {
                handler.removeCallbacks(sampleRunnable)
                handler.post(sampleRunnable)
            }
            AppPreferences.SCREEN_OFF_KEEP -> {
                handler.removeCallbacks(sampleRunnable)
                handler.post(sampleRunnable)
            }
        }
    }

    private fun getNextInterval(): Long {
        if (!isScreenOn) {
            return when (prefs.screenOffMode) {
                AppPreferences.SCREEN_OFF_PAUSE -> -1L
                AppPreferences.SCREEN_OFF_SLOW -> 10000L
                else -> prefs.updateIntervalMs
            }
        }
        return prefs.updateIntervalMs
    }

    private fun performSampleAndUpdate() {
        val sample = NetworkUtils.sampleSpeed(this)

        // Update Usage Tracker
        val intervalSec = (prefs.updateIntervalMs / 1000L).coerceAtLeast(1L)
        val rxBytesDelta = sample.downloadSpeedBytesPerSec * intervalSec
        val txBytesDelta = sample.uploadSpeedBytesPerSec * intervalSec

        when (sample.networkType) {
            NetworkType.WIFI -> prefs.addWifiUsage(rxBytesDelta, txBytesDelta)
            NetworkType.MOBILE -> prefs.addMobileUsage(rxBytesDelta, txBytesDelta)
            NetworkType.NONE -> {}
        }

        val useBits = prefs.useBitsPerSecond
        val downFormatted = NetworkUtils.formatSpeed(sample.downloadSpeedBytesPerSec, useBits)
        val upFormatted = NetworkUtils.formatSpeed(sample.uploadSpeedBytesPerSec, useBits)

        // Update Notification
        val notification = buildNotification(downFormatted, upFormatted, sample.networkType, prefs.todayWifiTotal, prefs.todayMobileTotal)
        notificationManager.notify(NOTIFICATION_ID, notification)

        // Broadcast to MainActivity if active
        val broadcastIntent = Intent(ACTION_SPEED_UPDATED).apply {
            putExtra(EXTRA_DOWNLOAD_SPEED, sample.downloadSpeedBytesPerSec)
            putExtra(EXTRA_UPLOAD_SPEED, sample.uploadSpeedBytesPerSec)
            putExtra(EXTRA_NETWORK_TYPE, sample.networkType.name)
            setPackage(packageName)
        }
        sendBroadcast(broadcastIntent)
    }

    private fun buildNotification(
        downStr: String,
        upStr: String,
        networkType: NetworkType,
        wifiBytes: Long,
        mobileBytes: Long
    ): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent ?: Intent(),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val netTypeName = when (networkType) {
            NetworkType.WIFI -> "Wi-Fi"
            NetworkType.MOBILE -> "Mobile"
            NetworkType.NONE -> "Offline"
        }

        val wifiStr = NetworkUtils.formatBytes(wifiBytes)
        val mobileStr = NetworkUtils.formatBytes(mobileBytes)

        val title = "Down: $downStr  |  Up: $upStr"
        val content = "Today: Mobile $mobileStr, Wi-Fi $wifiStr ($netTypeName)"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_speed)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (prefs.statusBarIconMode == AppPreferences.ICON_MODE_NUMBER) {
            val bytesForIcon = NetworkUtils.sampleSpeed(this).downloadSpeedBytesPerSec
            val icon = NotificationIconGenerator.createSpeedIcon(bytesForIcon, prefs.useBitsPerSecond)
            builder.setSmallIcon(icon)
        }

        return builder.build()
    }

    private fun startForegroundServiceWithNotification(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            startForeground(NOTIFICATION_ID, notification, fgsType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)
    }

    private fun unregisterScreenReceiver() {
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {}
    }
}
