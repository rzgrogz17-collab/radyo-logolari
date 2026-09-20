package com.globalradio.livetuneinogzapp.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.globalradio.livetuneinogzapp.R

/** Uyku zamanlayıcısı dairesel geri sayım halkası. */
class TimerRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var label: String = "0:00"
        set(value) {
            field = value
            invalidate()
        }

    var caption: String = ""
        set(value) {
            field = value
            invalidate()
        }

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        color = 0xFFE01B3B.toInt()
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = 0xFFFFFFFF.toInt()
        isFakeBoldText = true
    }
    private val capPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = 0xFF8899AA.toInt()
    }
    private val oval = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val stroke = 11f * d
        track.strokeWidth = stroke
        fill.strokeWidth = stroke
        track.color = ContextCompat.getColor(context, R.color.divider)
        textPaint.color = ContextCompat.getColor(context, R.color.text_primary)
        capPaint.color = ContextCompat.getColor(context, R.color.text_secondary)
        val pad = stroke / 2f + 8f * d
        oval.set(pad, pad, width - pad, height - pad)
        canvas.drawArc(oval, -90f, 360f, false, track)
        if (progress > 0f) {
            canvas.drawArc(oval, -90f, 360f * progress, false, fill)
        }
        textPaint.textSize = 28f * d
        val cy = height / 2f
        if (caption.isBlank()) {
            canvas.drawText(label, width / 2f, cy - (textPaint.ascent() + textPaint.descent()) / 2f, textPaint)
        } else {
            canvas.drawText(label, width / 2f, cy - 6f * d, textPaint)
            capPaint.textSize = 12f * d
            canvas.drawText(caption, width / 2f, cy + 18f * d, capPaint)
        }
    }
}
