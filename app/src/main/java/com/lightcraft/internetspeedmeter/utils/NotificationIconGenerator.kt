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
        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
            isFilterBitmap = true
            isDither = true
            hinting = Paint.HINTING_ON
        }

        // Top line: Number (e.g. "1", "40", "2.5", "450") - High-Res Crisp & Bold
        textPaint.textSize = when {
            value.length > 3 -> 72f
            value.length == 3 -> 86f
            else -> 98f
        }
        canvas.drawText(value, size / 2f, 88f, textPaint)

        // Bottom line: Unit (e.g. "KB/s", "MB/s", "B/s") - High-Res Crisp & Bold
        textPaint.textSize = if (unit.length > 4) 50f else 58f
        textPaint.typeface = Typeface.create("sans-serif-bold", Typeface.BOLD)
        canvas.drawText(unit, size / 2f, 160f, textPaint)

        return bitmap
    }
}
