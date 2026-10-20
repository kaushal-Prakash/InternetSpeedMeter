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
        val fullText = "$value $unit"
        val width = 160
        val height = 64
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        textPaint.textSize = when {
            fullText.length > 8 -> 24f
            fullText.length > 6 -> 28f
            else -> 32f
        }

        canvas.drawText(fullText, width / 2f, 42f, textPaint)

        return bitmap
    }
}
