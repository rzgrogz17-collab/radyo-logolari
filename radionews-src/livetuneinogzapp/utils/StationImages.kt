package com.globalradio.livetuneinogzapp.utils

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.globalradio.livetuneinogzapp.R

object StationImages {

    fun loadLogo(view: ImageView, url: String?, circle: Boolean = false) {
        val ctx = view.context
        if (url.isNullOrBlank() || AppSettings(ctx).dataSaver) {
            view.setImageResource(R.drawable.ic_radio_placeholder)
            return
        }
        val req = Glide.with(ctx)
            .load(url)
            .placeholder(R.drawable.ic_radio_placeholder)
            .error(R.drawable.ic_radio_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
        if (circle) req.circleCrop().into(view)
        else req.centerCrop().into(view)
    }

    fun loadFlag(
        view: ImageView,
        url: String?,
        placeholder: Int,
        onFail: () -> Unit,
        onReady: () -> Unit
    ) {
        val ctx = view.context
        if (url.isNullOrBlank() || AppSettings(ctx).dataSaver) {
            onFail()
            return
        }
        Glide.with(ctx)
            .load(url)
            .placeholder(placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop()
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    onFail()
                    return true
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    onReady()
                    return false
                }
            })
            .into(view)
    }
}
