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
        val size = 256
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 4f
            isFilterBitmap = true
            isDither = true
            hinting = Paint.HINTING_ON
        }

        // Top line: Number (e.g. "1", "40", "2.5", "450") - Ultra-Thick, Bold & Sharp
        textPaint.textSize = when {
            value.length > 3 -> 90f
            value.length == 3 -> 108f
            else -> 126f
        }
        textPaint.strokeWidth = 5f
        canvas.drawText(value, size / 2f, 115f, textPaint)

        // Bottom line: Unit (e.g. "KB/s", "MB/s", "B/s") - Ultra-Thick, Bold & Sharp
        textPaint.textSize = if (unit.length > 4) 62f else 74f
        textPaint.strokeWidth = 4f
        canvas.drawText(unit, size / 2f, 212f, textPaint)

        return bitmap
    }
}
