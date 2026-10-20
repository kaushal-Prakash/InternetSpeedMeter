package com.lightcraft.internetspeedmeter

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.lightcraft.internetspeedmeter.service.SpeedMeterService
import com.lightcraft.internetspeedmeter.ui.SpeedGraphView
import com.lightcraft.internetspeedmeter.utils.AppPreferences
import com.lightcraft.internetspeedmeter.utils.NetworkType
import com.lightcraft.internetspeedmeter.utils.NetworkUtils

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: AppPreferences

    private lateinit var switchService: SwitchMaterial
    private lateinit var ivNetworkType: ImageView
    private lateinit var tvNetworkStatus: TextView
    private lateinit var tvDownloadSpeed: TextView
    private lateinit var tvUploadSpeed: TextView
    private lateinit var speedGraph: SpeedGraphView

    private lateinit var cardBatteryWarning: MaterialCardView
    private lateinit var btnFixBattery: MaterialButton
    private lateinit var btnAutostartGuide: MaterialButton

    private lateinit var tvTodayWifi: TextView
    private lateinit var tvTodayMobile: TextView
    private lateinit var tvTodayTotal: TextView

    private lateinit var tvMonthWifi: TextView
    private lateinit var tvMonthMobile: TextView
    private lateinit var tvMonthTotal: TextView

    private lateinit var btnResetStats: MaterialButton

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeedServiceIfNeeded()
        } else {
            Toast.makeText(this, "Notification permission is required to show speed meter", Toast.LENGTH_LONG).show()
        }
    }

    private val speedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SpeedMeterService.ACTION_SPEED_UPDATED) {
                val downSpeed = intent.getLongExtra(SpeedMeterService.EXTRA_DOWNLOAD_SPEED, 0L)
                val upSpeed = intent.getLongExtra(SpeedMeterService.EXTRA_UPLOAD_SPEED, 0L)
                val netTypeStr = intent.getStringExtra(SpeedMeterService.EXTRA_NETWORK_TYPE) ?: NetworkType.NONE.name

                val netType = try {
                    NetworkType.valueOf(netTypeStr)
                } catch (_: Exception) {
                    NetworkType.NONE
                }

                updateLiveSpeedUI(downSpeed, upSpeed, netType)
                updateDataUsageUI()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = AppPreferences(this)

        initViews()
        setupListeners()
        checkNotificationPermissionAndStart()
    }

    override fun onResume() {
        super.onResume()
        updateServiceSwitchState()
        updateBatteryWarningVisibility()
        updateDataUsageUI()
        registerSpeedReceiver()
    }

    override fun onPause() {
        super.onPause()
        unregisterSpeedReceiver()
    }

    private fun initViews() {
        val toolbar = findViewById<MaterialToolbar>(R.id.topToolbar)
        toolbar.setOnMenuItemClickListener { menuItem ->
            if (menuItem.itemId == R.id.action_settings) {
                showSettingsBottomSheet()
                true
            } else {
                false
            }
        }

        switchService = findViewById(R.id.switchService)
        ivNetworkType = findViewById(R.id.ivNetworkType)
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus)
        tvDownloadSpeed = findViewById(R.id.tvDownloadSpeed)
        tvUploadSpeed = findViewById(R.id.tvUploadSpeed)
        speedGraph = findViewById(R.id.speedGraph)

        cardBatteryWarning = findViewById(R.id.cardBatteryWarning)
        btnFixBattery = findViewById(R.id.btnFixBattery)
        btnAutostartGuide = findViewById(R.id.btnAutostartGuide)

        tvTodayWifi = findViewById(R.id.tvTodayWifi)
        tvTodayMobile = findViewById(R.id.tvTodayMobile)
        tvTodayTotal = findViewById(R.id.tvTodayTotal)

        tvMonthWifi = findViewById(R.id.tvMonthWifi)
        tvMonthMobile = findViewById(R.id.tvMonthMobile)
        tvMonthTotal = findViewById(R.id.tvMonthTotal)

        btnResetStats = findViewById(R.id.btnResetStats)
    }

    private fun setupListeners() {
        switchService.setOnCheckedChangeListener { _, isChecked ->
            prefs.isServiceEnabled = isChecked
            if (isChecked) {
                SpeedMeterService.startService(this)
            } else {
                SpeedMeterService.stopService(this)
                speedGraph.clearData()
                tvDownloadSpeed.text = getString(R.string.zero_speed)
                tvUploadSpeed.text = getString(R.string.zero_speed)
            }
        }

        btnFixBattery.setOnClickListener {
            requestBatteryOptimizationExemption()
        }

        btnAutostartGuide.setOnClickListener {
            showAutostartGuideDialog()
        }

        btnResetStats.setOnClickListener {
            showResetConfirmDialog()
        }
    }

    private fun updateServiceSwitchState() {
        switchService.isChecked = prefs.isServiceEnabled
    }

    private fun checkNotificationPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                startSpeedServiceIfNeeded()
            }
        } else {
            startSpeedServiceIfNeeded()
        }
    }

    private fun startSpeedServiceIfNeeded() {
        if (prefs.isServiceEnabled) {
            SpeedMeterService.startService(this)
        }
    }

    private fun updateLiveSpeedUI(downSpeed: Long, upSpeed: Long, networkType: NetworkType) {
        val useBits = prefs.useBitsPerSecond
        tvDownloadSpeed.text = NetworkUtils.formatSpeed(downSpeed, useBits)
        tvUploadSpeed.text = NetworkUtils.formatSpeed(upSpeed, useBits)

        speedGraph.addSample(downSpeed, upSpeed)

        when (networkType) {
            NetworkType.WIFI -> {
                ivNetworkType.setImageResource(R.drawable.ic_wifi)
                tvNetworkStatus.text = getString(R.string.network_wifi)
            }
            NetworkType.MOBILE -> {
                ivNetworkType.setImageResource(R.drawable.ic_cellular)
                tvNetworkStatus.text = getString(R.string.network_mobile)
            }
            NetworkType.NONE -> {
                ivNetworkType.setImageResource(R.drawable.ic_speed_meter)
                tvNetworkStatus.text = getString(R.string.network_none)
            }
        }
    }

    private fun updateDataUsageUI() {
        prefs.checkAndResetUsageIfNeeded()

        tvTodayWifi.text = NetworkUtils.formatBytes(prefs.todayWifiTotal)
        tvTodayMobile.text = NetworkUtils.formatBytes(prefs.todayMobileTotal)
        tvTodayTotal.text = NetworkUtils.formatBytes(prefs.todayTotal)

        tvMonthWifi.text = NetworkUtils.formatBytes(prefs.monthWifiTotal)
        tvMonthMobile.text = NetworkUtils.formatBytes(prefs.monthMobileTotal)
        tvMonthTotal.text = NetworkUtils.formatBytes(prefs.monthTotal)
    }

    private fun updateBatteryWarningVisibility() {
        val pm = getSystemService(POWER_SERVICE) as? PowerManager
        val isIgnoring = pm?.isIgnoringBatteryOptimizations(packageName) ?: true

        if (isIgnoring) {
            cardBatteryWarning.visibility = View.GONE
        } else {
            cardBatteryWarning.visibility = View.VISIBLE
        }
    }

    @SuppressLint("BatteryLife")
    private fun requestBatteryOptimizationExemption() {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {
                Toast.makeText(this, "Could not open battery optimization settings", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAutostartGuideDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Auto-Start & Background Protection")
        builder.setMessage(
            "To prevent phone manufacturers (Xiaomi/MIUI/HyperOS, Samsung, Huawei, Oppo, Vivo, OnePlus, Realme) " +
            "from killing background services:\n\n" +
            "1. Enable Auto-Start permission for Internet Speed Meter.\n" +
            "2. Set Battery Usage to 'No Restrictions' / 'Unrestricted'.\n" +
            "3. Lock this app in recent apps screen."
        )
        builder.setPositiveButton("Open OEM Settings") { _, _ ->
            openOemAutostartSettings()
        }
        builder.setNegativeButton(R.string.cancel, null)
        builder.show()
    }

    private fun openOemAutostartSettings() {
        val brand = Build.MANUFACTURER.lowercase()
        val intentList = ArrayList<Intent>()

        when {
            brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") -> {
                intentList.add(Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")))
            }
            brand.contains("samsung") -> {
                intentList.add(Intent().setComponent(ComponentName("com.samsung.android.looper", "com.samsung.android.sm.ui.battery.BatteryActivity")))
            }
            brand.contains("huawei") || brand.contains("honor") -> {
                intentList.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")))
                intentList.add(Intent().setComponent(ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")))
            }
            brand.contains("oppo") || brand.contains("realme") -> {
                intentList.add(Intent().setComponent(ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")))
                intentList.add(Intent().setComponent(ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")))
            }
            brand.contains("vivo") -> {
                intentList.add(Intent().setComponent(ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")))
                intentList.add(Intent().setComponent(ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")))
            }
        }

        // App Details fallback
        intentList.add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        })

        for (intent in intentList) {
            try {
                startActivity(intent)
                return
            } catch (_: Exception) {}
        }
    }

    private fun showResetConfirmDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.reset_stats)
            .setMessage(R.string.reset_stats_confirm)
            .setPositiveButton(R.string.confirm) { _, _ ->
                prefs.resetAllStats()
                updateDataUsageUI()
                Toast.makeText(this, "Data usage stats reset successfully", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSettingsBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_settings, findViewById(android.R.id.content), false)
        dialog.setContentView(view)

        val rbUnitBytes = view.findViewById<RadioButton>(R.id.rbUnitBytes)
        val rbUnitBits = view.findViewById<RadioButton>(R.id.rbUnitBits)

        if (prefs.useBitsPerSecond) rbUnitBits.isChecked = true else rbUnitBytes.isChecked = true

        val rgInterval = view.findViewById<RadioGroup>(R.id.rgInterval)
        when (prefs.updateIntervalMs) {
            1000L -> view.findViewById<RadioButton>(R.id.rbInterval1s).isChecked = true
            2000L -> view.findViewById<RadioButton>(R.id.rbInterval2s).isChecked = true
            3000L -> view.findViewById<RadioButton>(R.id.rbInterval3s).isChecked = true
            5000L -> view.findViewById<RadioButton>(R.id.rbInterval5s).isChecked = true
            else -> view.findViewById<RadioButton>(R.id.rbInterval1s).isChecked = true
        }

        val rgScreenOff = view.findViewById<RadioGroup>(R.id.rgScreenOff)
        when (prefs.screenOffMode) {
            AppPreferences.SCREEN_OFF_KEEP -> view.findViewById<RadioButton>(R.id.rbScreenOffKeep).isChecked = true
            AppPreferences.SCREEN_OFF_SLOW -> view.findViewById<RadioButton>(R.id.rbScreenOffSlow).isChecked = true
            AppPreferences.SCREEN_OFF_PAUSE -> view.findViewById<RadioButton>(R.id.rbScreenOffPause).isChecked = true
            else -> view.findViewById<RadioButton>(R.id.rbScreenOffSlow).isChecked = true
        }

        val rgIconMode = view.findViewById<RadioGroup>(R.id.rgIconMode)
        when (prefs.statusBarIconMode) {
            AppPreferences.ICON_MODE_NUMBER -> view.findViewById<RadioButton>(R.id.rbIconNumber).isChecked = true
            AppPreferences.ICON_MODE_APP -> view.findViewById<RadioButton>(R.id.rbIconApp).isChecked = true
            else -> view.findViewById<RadioButton>(R.id.rbIconNumber).isChecked = true
        }

        val switchAutostart = view.findViewById<SwitchMaterial>(R.id.switchAutostart)
        switchAutostart.isChecked = prefs.autostartOnBoot

        view.findViewById<MaterialButton>(R.id.btnCloseSettings).setOnClickListener {
            prefs.useBitsPerSecond = rbUnitBits.isChecked

            prefs.updateIntervalMs = when (rgInterval.checkedRadioButtonId) {
                R.id.rbInterval1s -> 1000L
                R.id.rbInterval2s -> 2000L
                R.id.rbInterval3s -> 3000L
                R.id.rbInterval5s -> 5000L
                else -> 1000L
            }

            prefs.screenOffMode = when (rgScreenOff.checkedRadioButtonId) {
                R.id.rbScreenOffKeep -> AppPreferences.SCREEN_OFF_KEEP
                R.id.rbScreenOffSlow -> AppPreferences.SCREEN_OFF_SLOW
                R.id.rbScreenOffPause -> AppPreferences.SCREEN_OFF_PAUSE
                else -> AppPreferences.SCREEN_OFF_SLOW
            }

            prefs.statusBarIconMode = when (rgIconMode.checkedRadioButtonId) {
                R.id.rbIconNumber -> AppPreferences.ICON_MODE_NUMBER
                R.id.rbIconApp -> AppPreferences.ICON_MODE_APP
                else -> AppPreferences.ICON_MODE_NUMBER
            }

            prefs.autostartOnBoot = switchAutostart.isChecked

            if (prefs.isServiceEnabled) {
                SpeedMeterService.startService(this)
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun registerSpeedReceiver() {
        val filter = IntentFilter(SpeedMeterService.ACTION_SPEED_UPDATED)
        ContextCompat.registerReceiver(this, speedReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    private fun unregisterSpeedReceiver() {
        try {
            unregisterReceiver(speedReceiver)
        } catch (_: Exception) {}
    }
}
