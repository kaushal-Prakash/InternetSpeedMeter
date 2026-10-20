package com.lightcraft.internetspeedmeter.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import java.util.Locale

enum class NetworkType {
    WIFI,
    MOBILE,
    NONE
}

data class SpeedSample(
    val downloadSpeedBytesPerSec: Long,
    val uploadSpeedBytesPerSec: Long,
    val networkType: NetworkType
)

object NetworkUtils {

    private var lastRxBytes: Long = -1L
    private var lastTxBytes: Long = -1L
    private var lastSampleTimeMs: Long = -1L

    fun getActiveNetworkType(context: Context): NetworkType {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkType.NONE
        val activeNetwork = cm.activeNetwork ?: return NetworkType.NONE
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkType.NONE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.WIFI
            else -> NetworkType.NONE
        }
    }

    @Synchronized
    fun sampleSpeed(context: Context): SpeedSample {
        val currentRx = TrafficStats.getTotalRxBytes()
        val currentTx = TrafficStats.getTotalTxBytes()
        val currentTime = System.currentTimeMillis()

        if (lastRxBytes == -1L || lastTxBytes == -1L || lastSampleTimeMs == -1L) {
            lastRxBytes = currentRx
            lastTxBytes = currentTx
            lastSampleTimeMs = currentTime
            return SpeedSample(0L, 0L, getActiveNetworkType(context))
        }

        val timeDeltaMs = currentTime - lastSampleTimeMs
        if (timeDeltaMs <= 0) {
            return SpeedSample(0L, 0L, getActiveNetworkType(context))
        }

        val rxDelta = if (currentRx >= lastRxBytes) currentRx - lastRxBytes else 0L
        val txDelta = if (currentTx >= lastTxBytes) currentTx - lastTxBytes else 0L

        lastRxBytes = currentRx
        lastTxBytes = currentTx
        lastSampleTimeMs = currentTime

        val rxSpeed = (rxDelta * 1000L) / timeDeltaMs
        val txSpeed = (txDelta * 1000L) / timeDeltaMs

        return SpeedSample(
            downloadSpeedBytesPerSec = rxSpeed,
            uploadSpeedBytesPerSec = txSpeed,
            networkType = getActiveNetworkType(context)
        )
    }

    fun formatSpeed(bytesPerSec: Long, useBits: Boolean): String {
        if (bytesPerSec <= 0) {
            return if (useBits) "0 b/s" else "0 B/s"
        }

        return if (useBits) {
            val bitsPerSec = bytesPerSec * 8.0
            when {
                bitsPerSec >= 1_000_000_000 -> String.format(Locale.US, "%.1f Gbps", bitsPerSec / 1_000_000_000)
                bitsPerSec >= 1_000_000 -> String.format(Locale.US, "%.1f Mbps", bitsPerSec / 1_000_000)
                bitsPerSec >= 1_000 -> String.format(Locale.US, "%.0f Kbps", bitsPerSec / 1_000)
                else -> String.format(Locale.US, "%.0f bps", bitsPerSec)
            }
        } else {
            val bytes = bytesPerSec.toDouble()
            when {
                bytes >= 1_073_741_824 -> String.format(Locale.US, "%.2f GB/s", bytes / 1_073_741_824)
                bytes >= 1_048_576 -> String.format(Locale.US, "%.1f MB/s", bytes / 1_048_576)
                bytes >= 1024 -> String.format(Locale.US, "%.0f KB/s", bytes / 1024)
                else -> String.format(Locale.US, "%d B/s", bytesPerSec)
            }
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val b = bytes.toDouble()
        return when {
            b >= 1073741824.0 -> String.format(Locale.US, "%.2f GB", b / 1073741824.0)
            b >= 1048576.0 -> String.format(Locale.US, "%.1f MB", b / 1048576.0)
            b >= 1024.0 -> String.format(Locale.US, "%.0f KB", b / 1024.0)
            else -> String.format(Locale.US, "%d B", bytes)
        }
    }

    fun formatCompactSpeed(bytesPerSec: Long, useBits: Boolean): Pair<String, String> {
        if (bytesPerSec <= 0) return Pair("0", if (useBits) "b/s" else "B/s")

        if (useBits) {
            val bits = bytesPerSec * 8.0
            return when {
                bits >= 1_000_000_000 -> Pair(String.format(Locale.US, "%.1f", bits / 1_000_000_000), "Gbps")
                bits >= 1_000_000 -> Pair(String.format(Locale.US, "%.1f", bits / 1_000_000), "Mbps")
                bits >= 1000 -> Pair(String.format(Locale.US, "%.0f", bits / 1000), "Kbps")
                else -> Pair(String.format(Locale.US, "%d", (bits).toLong()), "b/s")
            }
        } else {
            val b = bytesPerSec.toDouble()
            return when {
                b >= 1073741824 -> Pair(String.format(Locale.US, "%.1f", b / 1073741824), "GB/s")
                b >= 1048576 -> Pair(String.format(Locale.US, "%.1f", b / 1048576), "MB/s")
                b >= 1024 -> Pair(String.format(Locale.US, "%.0f", b / 1024), "KB/s")
                else -> Pair(String.format(Locale.US, "%d", bytesPerSec), "B/s")
            }
        }
    }
}
