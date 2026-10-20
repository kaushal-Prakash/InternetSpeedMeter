package com.lightcraft.internetspeedmeter.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View

class SpeedGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val maxPoints = 30
    private val downloadHistory = ArrayList<Long>()
    private val uploadHistory = ArrayList<Long>()

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F808080")
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
    }

    private val downloadLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4CAF50")
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        pathEffect = CornerPathEffect(15f)
    }

    private val downloadFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        pathEffect = CornerPathEffect(15f)
    }

    private val uploadLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2196F3")
        strokeWidth = 4f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        pathEffect = CornerPathEffect(15f)
    }

    private val downloadPath = Path()
    private val downloadFillPath = Path()
    private val uploadPath = Path()

    fun addSample(downloadBytesPerSec: Long, uploadBytesPerSec: Long) {
        if (downloadHistory.size >= maxPoints) {
            downloadHistory.removeAt(0)
            uploadHistory.removeAt(0)
        }
        downloadHistory.add(downloadBytesPerSec)
        uploadHistory.add(uploadBytesPerSec)
        postInvalidate()
    }

    fun clearData() {
        downloadHistory.clear()
        uploadHistory.clear()
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        if (width <= 0 || height <= 0) return

        // Draw background grid lines (3 horizontal lines)
        val gridStep = height / 3f
        canvas.drawLine(0f, gridStep, width, gridStep, gridPaint)
        canvas.drawLine(0f, gridStep * 2, width, gridStep * 2, gridPaint)

        if (downloadHistory.isEmpty()) return

        var maxVal = 1024L * 10L // Minimum scale 10 KB/s
        for (i in downloadHistory.indices) {
            if (downloadHistory[i] > maxVal) maxVal = downloadHistory[i]
            if (uploadHistory[i] > maxVal) maxVal = uploadHistory[i]
        }

        val stepX = width / (maxPoints - 1).coerceAtLeast(1)
        val paddingBottom = 10f
        val availableHeight = height - paddingBottom - 10f

        downloadPath.reset()
        downloadFillPath.reset()
        uploadPath.reset()

        val startX = (maxPoints - downloadHistory.size) * stepX

        downloadFillPath.moveTo(startX, height - paddingBottom)

        for (i in downloadHistory.indices) {
            val x = startX + i * stepX
            val downRatio = (downloadHistory[i].toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
            val upRatio = (uploadHistory[i].toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)

            val downY = height - paddingBottom - (downRatio * availableHeight)
            val upY = height - paddingBottom - (upRatio * availableHeight)

            if (i == 0) {
                downloadPath.moveTo(x, downY)
                downloadFillPath.lineTo(x, downY)
                uploadPath.moveTo(x, upY)
            } else {
                downloadPath.lineTo(x, downY)
                downloadFillPath.lineTo(x, downY)
                uploadPath.lineTo(x, upY)
            }
        }

        val lastX = startX + (downloadHistory.size - 1) * stepX
        downloadFillPath.lineTo(lastX, height - paddingBottom)
        downloadFillPath.close()

        // Gradient shader for download fill
        downloadFillPaint.shader = LinearGradient(
            0f, 0f, 0f, height,
            Color.parseColor("#334CAF50"),
            Color.parseColor("#004CAF50"),
            Shader.TileMode.CLAMP
        )

        canvas.drawPath(downloadFillPath, downloadFillPaint)
        canvas.drawPath(downloadPath, downloadLinePaint)
        canvas.drawPath(uploadPath, uploadLinePaint)
    }
}
