package com.globalradio.livetuneinogzapp.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.globalradio.livetuneinogzapp.R
import com.globalradio.livetuneinogzapp.databinding.ItemStationBinding
import com.globalradio.livetuneinogzapp.model.RadioStation
import com.globalradio.livetuneinogzapp.utils.FavoriteIcon
import com.globalradio.livetuneinogzapp.utils.StationImages

class StationAdapter(
    private val onStationClick: (RadioStation) -> Unit,
    private val onFavoriteClick: (RadioStation) -> Unit
) : ListAdapter<RadioStation, StationAdapter.VH>(DIFF) {

    private var playingId: String? = null

    private val tagColors = listOf(
        "#7B2FBE", "#1A6B3C", "#841162", "#1A5276",
        "#7D6608", "#2E4057", "#d78126", "#1A3A4A"
    )

    fun updatePlayingStation(id: String?) {
        val old = playingId
        playingId = id
        currentList.forEachIndexed { i, s ->
            if (s.id == old || s.id == id) notifyItemChanged(i, "playing")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemStationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(getItem(position))

    override fun onBindViewHolder(holder: VH, position: Int, payloads: MutableList<Any>) {
        when {
            payloads.isEmpty() -> holder.bind(getItem(position))
            payloads.contains("playing") -> holder.updatePlaying(
                getItem(position),
                getItem(position).id == playingId
            )

            payloads.contains("favorite") -> holder.updateFavIconPublic(getItem(position).isFavorite)
            else -> holder.bind(getItem(position))
        }
    }

    /** Favori değişince sadece kalp ikonunu güncelle - tüm satırı rebind etme */
    fun updateFavorite(stationId: String, isFav: Boolean) {
        currentList.forEachIndexed { i, s ->
            if (s.id == stationId) {
                s.isFavorite = isFav
                notifyItemChanged(i, "favorite")
            }
        }
    }

    inner class VH(private val b: ItemStationBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(s: RadioStation) {
            b.tvStationName.text = s.name
            b.tvCountry.text = if (s.country.isNotBlank()) s.country else ""
            buildTagPills(s)
            updateFavIconPublic(s.isFavorite)
            loadLogo(s)
            updatePlaying(s, s.id == playingId)

            b.root.setOnClickListener { onStationClick(s) }
            b.ivFavorite.setOnClickListener { v ->
                v.isEnabled = false
                onFavoriteClick(s)
                v.postDelayed({ v.isEnabled = true }, 500)
            }
        }

        fun updatePlaying(s: RadioStation, playing: Boolean) {
            val ctx = b.root.context
            b.tvStationName.setTextColor(
                if (playing) ctx.getColor(R.color.accent) else ctx.getColor(R.color.text_primary)
            )
            b.cardRoot.setCardBackgroundColor(
                ctx.getColor(if (playing) R.color.bg_card_playing else R.color.bg_card)
            )
        }

        private fun buildTagPills(s: RadioStation) {
            val ctx = b.root.context
            // MAX 2 tag, kısa isimler
            val tags = s.getTagList().take(2).map { tag ->
                if (tag.length > 14) tag.substring(0, 12) + "…" else tag
            }
            b.tagContainer.removeAllViews()

            if (tags.isEmpty()) {
                b.tagContainer.visibility = View.GONE
                return
            }

            b.tagContainer.visibility = View.VISIBLE
            tags.forEachIndexed { i, tag ->
                val colorHex = tagColors[tag.hashCode().and(0x7FFFFFFF) % tagColors.size]
                val tv = TextView(ctx).apply {
                    text = tag
                    textSize = 10f
                    maxLines = 1
                    isSingleLine = true
                    setTextColor(Color.WHITE)
                    setPadding(12, 2, 12, 2)
                    background = GradientDrawable().apply {
                        setColor(Color.parseColor(colorHex))
                        cornerRadius = 10f
                    }
                    val lp = ViewGroup.MarginLayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    lp.setMargins(0, 0, if (i < tags.size - 1) 6 else 0, 0)
                    layoutParams = lp
                }
                b.tagContainer.addView(tv)
            }
        }

        fun updateFavIconPublic(fav: Boolean) {
            FavoriteIcon.apply(b.ivFavorite, fav)
        }

        private fun loadLogo(s: RadioStation) {
            if (s.hasValidFavicon()) {
                b.tvAvatar.visibility = View.GONE
                b.ivLogo.visibility = View.VISIBLE
                StationImages.loadLogo(b.ivLogo, s.favicon)
            } else {
                b.ivLogo.visibility = View.GONE
                b.tvAvatar.visibility = View.VISIBLE
                val colorHex = tagColors[s.name.hashCode().and(0x7FFFFFFF) % tagColors.size]
                b.cvAvatar.setCardBackgroundColor(Color.parseColor(colorHex))
                b.tvAvatar.text = s.getAvatarText()
            }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<RadioStation>() {
            override fun areItemsTheSame(a: RadioStation, b: RadioStation) = a.id == b.id
            override fun areContentsTheSame(a: RadioStation, b: RadioStation) =
                a.isFavorite == b.isFavorite && a.name == b.name && a.favicon == b.favicon
        }
    }
}