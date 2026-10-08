package com.globalradio.livetuneinogzapp.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * Çalan satırın krem zemini: hafif gölgeyle havada durur,
 * sağ ve sol uçlar yuvarlaktır ve alta doğru içe kıvrılır.
 */
class PlayingRowDrawable(
    private val fillColor: Int,
    private val radiusPx: Float,
    private val tuckPx: Float
) : Drawable() {

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val l = b.left.toFloat() + 1f
        val t = b.top.toFloat() + 2f
        val r = b.right.toFloat() - 1f
        val bottom = b.bottom.toFloat() - 5f
        drawShape(canvas, l, t + 3f, r, bottom + 3f, shadow.apply { color = 0x33000000 })
        drawShape(canvas, l, t + 1.5f, r, bottom + 1.5f, shadow.apply { color = 0x1A000000 })
        drawShape(canvas, l, t, r, bottom, fill.apply { color = fillColor })
    }

    private fun drawShape(canvas: Canvas, l: Float, t: Float, r: Float, b: Float, paint: Paint) {
        val rad = radiusPx.coerceAtMost((b - t) / 2f).coerceAtMost((r - l) / 4f)
        val tuck = tuckPx.coerceAtMost((r - l) / 5f)
        path.reset()
        path.moveTo(l + rad, t)
        path.lineTo(r - rad, t)
        path.quadTo(r, t, r, t + rad)
        path.lineTo(r, b - rad)
        path.quadTo(r - tuck * 0.15f, b, r - tuck, b)
        path.lineTo(l + tuck, b)
        path.quadTo(l + tuck * 0.15f, b, l, b - rad)
        path.lineTo(l, t + rad)
        path.quadTo(l, t, l + rad, t)
        path.close()
        canvas.drawPath(path, paint)
    }

    override fun setAlpha(alpha: Int) {
        fill.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fill.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    companion object {
        fun create(context: Context, fillColor: Int): PlayingRowDrawable {
            val d = context.resources.displayMetrics.density
            return PlayingRowDrawable(
                fillColor = fillColor,
                radiusPx = 16f * d,
                tuckPx = 10f * d
            )
        }
    }
}
