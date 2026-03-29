package com.example.calc.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.calc.data.model.TelephonyData
import java.util.ArrayDeque

class ImPlotSignalChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val maxPoints = 120

    private val signalData = mapOf(
        "GSM" to ArrayDeque<Float>(maxPoints),
        "LTE" to ArrayDeque<Float>(maxPoints),
        "NR" to ArrayDeque<Float>(maxPoints)
    )

    private val gridPaint = Paint().apply {
        color = Color.DKGRAY
        strokeWidth = 1f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.LTGRAY
        textSize = 28f
        isAntiAlias = true
    }

    private val colorMap = mapOf(
        "GSM" to Color.GREEN,
        "LTE" to Color.CYAN,
        "NR" to Color.MAGENTA
    )

    private val linePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    @Synchronized
    fun updateFromTelephonyData(data: TelephonyData) {
        if (data.cellInfo.isEmpty()) {
            return
        }

        val bestValues = mutableMapOf("GSM" to Float.MIN_VALUE, "LTE" to Float.MIN_VALUE, "NR" to Float.MIN_VALUE)
        
        data.cellInfo.forEach { info ->
            val type = info.type
            val signalMap = info.signal
            
            val value = when (type) {
                "LTE" -> {
                    (signalMap["rsrp"] as? Number)?.toFloat()
                        ?: (signalMap["rssi"] as? Number)?.toFloat()
                        ?: Float.MIN_VALUE
                }
                "GSM" -> {
                    (signalMap["dbm"] as? Number)?.toFloat()
                        ?: (signalMap["asuLevel"] as? Number)?.toFloat()
                        ?: Float.MIN_VALUE
                }
                "NR" -> {
                    (signalMap["ssRsrp"] as? Number)?.toFloat()
                        ?: Float.MIN_VALUE
                }
                else -> Float.MIN_VALUE
            }
            
            if (value != Float.MIN_VALUE && value > -200f && value < 0f) {
                val current = bestValues[type] ?: Float.MIN_VALUE
                bestValues[type] = maxOf(current, value)
            }
        }

        var hasData = false
        bestValues.forEach { (type, valNum) ->
            if (valNum != Float.MIN_VALUE) {
                pushSample(type, valNum)
                hasData = true
            }
        }

        if (hasData) {
            // Force immediate redraw, not just on next animation frame
            invalidate()
            postInvalidateOnAnimation()
        }
    }

    @Synchronized
    private fun pushSample(type: String, value: Float) {
        (signalData[type] ?: return).let { dq ->
            if (dq.size >= maxPoints) dq.removeFirst()
            dq.addLast(value)
        }
    }

    @Synchronized
    private fun copyData(): Map<String, List<Float>> {
        return signalData.mapValues { it.value.toList() }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawColor(Color.BLACK)

        val width = width.toFloat()
        val height = height.toFloat()

        for (i in 0..4) {
            val y = paddingTop + i * (height - paddingTop - paddingBottom) / 4f
            canvas.drawLine(paddingLeft.toFloat(), y, width - paddingRight, y, gridPaint)
            canvas.drawText("${-140 + i*20} dBm", 10f, y - 8f, textPaint)
        }

        val chartLeft = paddingLeft.toFloat() + 64f
        val chartRight = width - paddingRight
        val chartTop = paddingTop.toFloat() + 20f
        val chartBottom = height - paddingBottom - 20f
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        val data = copyData()
        val yMin = -140f
        val yMax = -40f

        data.forEach { (type, list) ->
            if (list.isEmpty()) return@forEach
            linePaint.color = colorMap[type] ?: Color.WHITE

            val stepX = chartWidth / maxOf(list.size - 1, 1)
            var lastX = chartLeft
            var lastY = chartBottom - ((list.first() - yMin) / (yMax - yMin)) * chartHeight
            list.forEachIndexed { index, value ->
                val x = chartLeft + stepX * index
                val y = chartBottom - ((value - yMin) / (yMax - yMin)) * chartHeight
                if (index > 0) {
                    canvas.drawLine(lastX, lastY, x, y, linePaint)
                }
                lastX = x
                lastY = y
            }
            canvas.drawText(type, chartLeft + 8f, chartTop + 30f + 30f * (when (type) {"LTE" -> 0; "NR" -> 1; else -> 2}), textPaint)
        }

        colorMap.entries.forEachIndexed { idx, entry ->
            val text = entry.key
            linePaint.color = entry.value
            linePaint.strokeWidth = 6f
            val x0 = chartLeft + 10f
            val y0 = chartBottom + 30f + idx * 26f
            canvas.drawLine(x0, y0, x0 + 30f, y0, linePaint)
            canvas.drawText(text, x0 + 38f, y0 + 8f, textPaint)
        }

        canvas.drawText("ImPlot-style CellSignalStrength (dBm)", chartLeft, paddingTop.toFloat() + 15f, textPaint)
    }
}
