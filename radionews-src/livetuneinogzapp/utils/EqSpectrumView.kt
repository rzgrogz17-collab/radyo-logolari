package com.globalradio.livetuneinogzapp.utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

/** EQ açıkken çalışan sahte spektrum (mikrofon izni gerektirmez). */
class EqSpectrumView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val barCount = 14
    private val targets = FloatArray(barCount) { 0.25f }
    private val shown = FloatArray(barCount) { 0.2f }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFE01B3B.toInt()
        style = Paint.Style.FILL
    }
    private var animator: ValueAnimator? = null
    var active: Boolean = false
        set(value) {
            field = value
            if (value) start() else stop()
            invalidate()
        }

    private fun start() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 180
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                for (i in 0 until barCount) {
                    if (Random.nextFloat() > 0.55f) {
                        targets[i] = 0.15f + Random.nextFloat() * 0.85f
                    }
                    shown[i] += (targets[i] - shown[i]) * 0.28f
                }
                invalidate()
            }
            start()
        }
    }

    private fun stop() {
        animator?.cancel()
        animator = null
        for (i in shown.indices) shown[i] = 0.12f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val gap = 3f * d
        val w = (width - gap * (barCount + 1)) / barCount
        val h = height.toFloat()
        val radius = 2.5f * d
        for (i in 0 until barCount) {
            val bh = (0.08f + shown[i] * 0.92f) * h
            val left = gap + i * (w + gap)
            paint.alpha = if (active) 220 else 70
            canvas.drawRoundRect(left, h - bh, left + w, h, radius, radius, paint)
        }
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }
}
