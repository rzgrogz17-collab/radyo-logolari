package com.globalradio.livetuneinogzapp.utils

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat
import com.globalradio.livetuneinogzapp.R

/**
 * Favori kalbi tek görsel kuralı:
 *  • Favoride     → kırmızı dolu kalp
 *  • Favori değil → boş beyaz kalp
 *
 * ImageButton XML/theme tint'i ile eski setColorFilter, dolu kalbi
 * beyaza boyayabildiği için her güncellemede renk açıkça yazılır.
 */
object FavoriteIcon {

    const val FILLED_RED = 0xFFE01B3B.toInt()
    const val EMPTY_WHITE = Color.WHITE

    fun apply(view: ImageView, favorite: Boolean) {
        view.clearColorFilter()
        view.setImageResource(
            if (favorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border
        )
        val color = if (favorite) FILLED_RED else EMPTY_WHITE
        ImageViewCompat.setImageTintList(view, ColorStateList.valueOf(color))
        ImageViewCompat.setImageTintMode(view, PorterDuff.Mode.SRC_IN)
        view.isSelected = favorite
        view.invalidate()
    }
}
