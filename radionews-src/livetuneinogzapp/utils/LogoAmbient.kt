package com.globalradio.livetuneinogzapp.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable

/** Logodaki belirgin renkleri, en çok olandan başlayarak seçer. */
object LogoColorRange {

    fun extract(source: Bitmap): IntArray {
        val maxSide = 48
        val scale = maxOf(source.width, source.height).coerceAtLeast(1)
        val bmp = if (scale > maxSide) {
            val w = (source.width * maxSide / scale).coerceAtLeast(1)
            val h = (source.height * maxSide / scale).coerceAtLeast(1)
            Bitmap.createScaledBitmap(source, w, h, true)
        } else {
            source
        }
        val buckets = HashMap<Int, Int>()
        val pixels = IntArray(bmp.width * bmp.height)
        bmp.getPixels(pixels, 0, bmp.width, 0, 0, bmp.width, bmp.height)
        for (c in pixels) {
            if (Color.alpha(c) < 170) continue
            val r = Color.red(c)
            val g = Color.green(c)
            val b = Color.blue(c)
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            if (max < 36 || min > 232 || max - min < 26) continue
            val key = Color.rgb(r / 28 * 28, g / 28 * 28, b / 28 * 28)
            buckets[key] = (buckets[key] ?: 0) + 1
        }
        if (bmp !== source) bmp.recycle()
        val top = buckets.entries.sortedByDescending { it.value }.take(3).map { it.key }
        return if (top.isEmpty()) intArrayOf(Color.parseColor("#E01B3B")) else top.toIntArray()
    }
}

/** Logo renk aralığını logonun arkasından ekrana doğru dağıtır. */
class LogoAmbientDrawable(colors: IntArray) : Drawable() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stops = intArrayOf(
        withAlpha(colors[0], 168),
        withAlpha(colors[0], 96),
        withAlpha(colors.getOrElse(1) { colors[0] }, 64),
        withAlpha(colors.getOrElse(2) { colors.getOrElse(1) { colors[0] } }, 28),
        Color.TRANSPARENT
    )
    private val positions = floatArrayOf(0f, 0.22f, 0.46f, 0.7f, 1f)

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val cx = b.exactCenterX()
        val cy = b.top + b.height() * 0.34f
        val radius = maxOf(b.width(), b.height()) * 0.78f
        paint.shader = RadialGradient(cx, cy, radius, stops, positions, Shader.TileMode.CLAMP)
        canvas.drawRect(b, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}
