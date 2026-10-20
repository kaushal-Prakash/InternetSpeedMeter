package com.lightcraft.internetspeedmeter.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.drawable.IconCompat

object NotificationIconGenerator {

    fun createSpeedIcon(bytesPerSec: Long, useBits: Boolean): IconCompat {
        val (valueStr, unitStr) = NetworkUtils.formatCompactSpeed(bytesPerSec, useBits)
        val bitmap = drawSpeedBitmap(valueStr, unitStr)
        return IconCompat.createWithBitmap(bitmap)
    }

    fun drawSpeedBitmap(value: String, unit: String): Bitmap {
        val size = 96
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Top line: Number (e.g. "1", "40", "2.5", "450") - Extra Big & Bold
        textPaint.textSize = when {
            value.length > 3 -> 38f
            value.length == 3 -> 44f
            else -> 50f
        }
        canvas.drawText(value, size / 2f, 44f, textPaint)

        // Bottom line: Unit (e.g. "KB/s", "MB/s", "B/s") - Extra Big & Bold
        textPaint.textSize = if (unit.length > 4) 26f else 30f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(unit, size / 2f, 82f, textPaint)

        return bitmap
    }
}
