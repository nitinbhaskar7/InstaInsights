// ui/ReachChartView.kt
package com.example.instainsights.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.example.instainsights.R
import com.example.instainsights.models.SeriesPoint
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReachChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private var data: List<SeriesPoint> = emptyList()

    // ── Paints ────────────────────────────────────────────────────────────────

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeWidth = 2.5f
        strokeJoin  = Paint.Join.ROUND
        strokeCap   = Paint.Cap.ROUND
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val dotBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeWidth = 2f
        color       = Color.WHITE
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style     = Paint.Style.STROKE
        strokeWidth = 1f
        color     = Color.parseColor("#F0F0F0")
        pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize  = 28f      // ~10sp in px
        textAlign = Paint.Align.CENTER
        color     = Color.parseColor("#888888")
    }

    // ── Peak tracking ─────────────────────────────────────────────────────────
    private var peakIndex = -1

    // ── Public API ────────────────────────────────────────────────────────────

    fun setData(points: List<SeriesPoint>) {
        data      = points
        peakIndex = points.indices.maxByOrNull { points[it].value } ?: -1
        invalidate()
    }

    // ── Drawing ───────────────────────────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.size < 2) return

        val primaryColor = ContextCompat.getColor(context, R.color.colorPrimary)
        linePaint.color  = primaryColor
        dotPaint.color   = primaryColor

        val w     = width.toFloat()
        val h     = height.toFloat()
        val padL  = 8f
        val padR  = 8f
        val padT  = 16f
        val padB  = 28f    // space for date labels at bottom

        val chartW = w - padL - padR
        val chartH = h - padT - padB

        val maxVal = data.maxOf { it.value }.toFloat().coerceAtLeast(1f)
        val minVal = data.minOf { it.value }.toFloat()
        val range  = (maxVal - minVal).coerceAtLeast(1f)

        val stepX = chartW / (data.size - 1)

        // ── X coordinate for index i
        fun xAt(i: Int) = padL + i * stepX

        // ── Y coordinate for value v (0 = bottom)
        fun yAt(v: Int) = padT + chartH - ((v - minVal) / range) * chartH

        // ── 3 horizontal grid lines ──
        for (level in 1..3) {
            val y = padT + chartH * (1f - level / 4f)
            canvas.drawLine(padL, y, w - padR, y, gridPaint)
        }

        // ── Build filled area path ──
        val fillPath = Path().apply {
            moveTo(xAt(0), h - padB)
            data.forEachIndexed { i, pt -> lineTo(xAt(i), yAt(pt.value)) }
            lineTo(xAt(data.size - 1), h - padB)
            close()
        }

        // Gradient fill using a LinearGradient shader
        fillPaint.shader = LinearGradient(
            0f, padT, 0f, h - padB,
            intArrayOf(
                Color.argb(60,  primaryColor.red, primaryColor.green, primaryColor.blue),
                Color.argb(0,   primaryColor.red, primaryColor.green, primaryColor.blue)
            ),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawPath(fillPath, fillPaint)

        // ── Build line path ──
        val linePath = Path().apply {
            moveTo(xAt(0), yAt(data[0].value))
            data.forEachIndexed { i, pt -> if (i > 0) lineTo(xAt(i), yAt(pt.value)) }
        }
        canvas.drawPath(linePath, linePaint)

        // ── Dots on every 5th point + peak ──
        data.forEachIndexed { i, pt ->
            val showDot = (i % 5 == 0) || i == peakIndex ||
                    i == 0 || i == data.size - 1
            if (showDot) {
                val cx = xAt(i)
                val cy = yAt(pt.value)
                val r  = if (i == peakIndex) 5f else 3.5f
                canvas.drawCircle(cx, cy, r, dotPaint)
                canvas.drawCircle(cx, cy, r, dotBorderPaint)
            }
        }

        // ── Date labels: first, middle, last ──
        val labelIndices = listOf(0, data.size / 2, data.size - 1)
        Log.i("LOL" , data.toString()) ;
        labelIndices.forEach { i ->
            val label = getMmDdDaysAgo(i)  // "MM-dd"
            canvas.drawText(label, xAt(i), h - 4f, labelPaint)
        }

    }
}
fun getMmDdDaysAgo(daysAgo: Int): String {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.DAY_OF_YEAR, -daysAgo)

    val formatter = SimpleDateFormat("MM-dd", Locale.getDefault())
    return formatter.format(calendar.time)
}
// Extension to extract RGB components from a packed Int color
private val Int.red   get() = (this shr 16) and 0xFF
private val Int.green get() = (this shr 8)  and 0xFF
private val Int.blue  get() = this and 0xFF