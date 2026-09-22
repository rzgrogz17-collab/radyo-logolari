package com.globalradio.livetuneinogzapp.utils

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.globalradio.livetuneinogzapp.R

/**
 * Favori kalbi tek görsel kuralı:
 *  • Favoride     → kırmızı dolu kalp
 *  • Favori değil → boş kalp (listede temaya göre; büyük player’da beyaz)
 */
object FavoriteIcon {

    const val FILLED_RED = 0xFFE01B3B.toInt()
    private val EMPTY_ON_LIGHT = Color.parseColor("#2C2C34")

    fun apply(
        view: ImageView,
        favorite: Boolean,
        onLightSurface: Boolean = false,
        emptyColor: Int? = null
    ) {
        view.clearColorFilter()
        view.setImageResource(
            if (favorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
        )
        val empty = emptyColor ?: if (onLightSurface) {
            EMPTY_ON_LIGHT
        } else {
            ContextCompat.getColor(view.context, R.color.favorite_empty)
        }
        val color = if (favorite) FILLED_RED else empty
        ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(color))
        ImageViewCompat.setImageTintMode(view, PorterDuff.Mode.SRC_IN)
        view.isSelected = favorite
        view.invalidate()
    }
}
