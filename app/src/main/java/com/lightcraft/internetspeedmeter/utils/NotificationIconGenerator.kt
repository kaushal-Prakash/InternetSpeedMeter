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
        val size = 72
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Top value string (e.g. "1", "40", "2.5", "450")
        textPaint.textSize = when {
            value.length > 3 -> 26f
            value.length == 3 -> 30f
            else -> 36f
        }
        canvas.drawText(value, size / 2f, 32f, textPaint)

        // Bottom unit string (e.g. "KB/s", "MB/s", "B/s")
        textPaint.textSize = if (unit.length > 4) 18f else 22f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(unit, size / 2f, 58f, textPaint)

        return bitmap
    }
}
