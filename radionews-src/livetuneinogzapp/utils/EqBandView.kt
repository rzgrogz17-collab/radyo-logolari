package com.globalradio.livetuneinogzapp.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.roundToInt

/** Dikey ekolayzer bandı — döndürülmüş SeekBar yerine dokunmatik sütun. */
class EqBandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var minLevel: Int = -1500
    var maxLevel: Int = 1500
    var interactive: Boolean = true
        set(value) {
            field = value
            alpha = if (value) 1f else 0.38f
            invalidate()
        }

    var level: Int = 0
        set(value) {
            field = value.coerceIn(minLevel, maxLevel)
            invalidate()
        }

    var onLevelChanged: ((level: Int, fromUser: Boolean) -> Unit)? = null

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val cx = width / 2f
        val pad = 18f * d
        val top = pad
        val bot = height - pad
        val span = (bot - top).coerceAtLeast(1f)
        val stroke = 6f * d
        trackPaint.strokeWidth = stroke
        fillPaint.strokeWidth = stroke
        trackPaint.color = 0xFF3A3A44.toInt()
        fillPaint.color = 0xFFE01B3B.toInt()
        thumbPaint.color = 0xFFE01B3B.toInt()
        centerPaint.color = 0xFFAABBCC.toInt()
        centerPaint.strokeWidth = 1.2f * d

        canvas.drawLine(cx, top, cx, bot, trackPaint)
        val midY = top + span / 2f
        canvas.drawLine(cx - 10f * d, midY, cx + 10f * d, midY, centerPaint)

        val t = (level - minLevel).toFloat() / (maxLevel - minLevel).coerceAtLeast(1)
        val y = bot - t * span
        canvas.drawLine(cx, midY, cx, y, fillPaint)
        canvas.drawCircle(cx, y, 8f * d, thumbPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!interactive) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(true)
                applyTouch(event.y, fromUser = true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                applyTouch(event.y, fromUser = true)
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun applyTouch(yRaw: Float, fromUser: Boolean) {
        val d = resources.displayMetrics.density
        val pad = 18f * d
        val top = pad
        val bot = height - pad
        val span = (bot - top).coerceAtLeast(1f)
        val y = yRaw.coerceIn(top, bot)
        val t = 1f - (y - top) / span
        val newLevel = (minLevel + t * (maxLevel - minLevel)).roundToInt()
            .coerceIn(minLevel, maxLevel)
        if (newLevel != level) {
            level = newLevel
            onLevelChanged?.invoke(level, fromUser)
        }
    }
}
