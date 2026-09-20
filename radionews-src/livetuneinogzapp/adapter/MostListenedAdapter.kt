package com.globalradio.livetuneinogzapp.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.globalradio.livetuneinogzapp.R
import com.globalradio.livetuneinogzapp.databinding.ItemMostListenedBinding
import com.globalradio.livetuneinogzapp.model.RadioStation
import com.globalradio.livetuneinogzapp.utils.FavoriteIcon
import com.globalradio.livetuneinogzapp.utils.StationImages

class MostListenedAdapter(
    private val onStationClick: (RadioStation) -> Unit,
    private val onFavoriteClick: (RadioStation) -> Unit
) : ListAdapter<RadioStation, MostListenedAdapter.ViewHolder>(DIFF) {

    private var playingStationId: String? = null
    private var playCounts: Map<String, Int> = emptyMap()

    fun setPlayCounts(counts: Map<String, Int>) {
        playCounts = counts
    }

    fun updatePlayingStation(stationId: String?) {
        val old = playingStationId
        playingStationId = stationId
        currentList.forEachIndexed { i, s ->
            if (s.id == old || s.id == stationId) notifyItemChanged(i)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            ItemMostListenedBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position), position + 1, playCounts[getItem(position).id] ?: 0)

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        when {
            payloads.contains(PAYLOAD_FAVORITE) -> holder.updateFavIcon(getItem(position).isFavorite)
            payloads.contains(PAYLOAD_PLAYCOUNT) -> holder.updatePlayCount(
                position + 1,
                playCounts[getItem(position).id] ?: 0
            )

            else -> holder.bind(getItem(position), position + 1, playCounts[getItem(position).id] ?: 0)
        }
    }

    /** Favori değişince sadece kalp ikonunu güncelle - tüm satırı rebind etme */
    fun updateFavorite(stationId: String, isFav: Boolean) {
        currentList.forEachIndexed { i, s ->
            if (s.id == stationId) {
                s.isFavorite = isFav
                notifyItemChanged(i, PAYLOAD_FAVORITE)
            }
        }
    }

    /** Dinlenme sayıları değişince tüm satırları rebind etmeden sadece rozet/sayacı günceller */
    fun refreshPlayCounts(counts: Map<String, Int>) {
        playCounts = counts
        if (itemCount > 0) notifyItemRangeChanged(0, itemCount, PAYLOAD_PLAYCOUNT)
    }

    inner class ViewHolder(private val b: ItemMostListenedBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(station: RadioStation, rank: Int, playCount: Int) {
            val ctx = b.root.context
            val isPlaying = station.id == playingStationId

            // Sıra rozeti
            b.tvRank.text = rank.toString()
            b.tvRank.setBackgroundColor(
                when (rank) {
                    1 -> Color.parseColor("#FFD700")
                    2 -> Color.parseColor("#C0C0C0")
                    3 -> Color.parseColor("#CD7F32")
                    else -> Color.parseColor("#1E2A3A")
                }
            )
            b.tvRank.setTextColor(if (rank <= 3) Color.parseColor("#0A0A1A") else Color.WHITE)

            b.tvStationName.text = station.name
            b.tvCountry.text = if (station.country.isNotBlank()) "🌍 ${station.country}" else ""

            b.tvPlayCount.text = if (playCount > 0)
                ctx.getString(R.string.play_count_times, playCount)
            else
                ctx.getString(R.string.play_count_first_time)

            // Logo
            if (station.hasValidFavicon()) {
                b.ivLogo.visibility = View.VISIBLE
                b.cvAvatar.visibility = View.GONE
                StationImages.loadLogo(b.ivLogo, station.favicon, circle = true)
            } else {
                b.ivLogo.visibility = View.GONE
                b.cvAvatar.visibility = View.VISIBLE
                val colors = listOf(
                    "#E94560",
                    "#533483",
                    "#0F3460",
                    "#1B4332",
                    "#6D2B3D",
                    "#1A5276",
                    "#784212",
                    "#16213E"
                )
                b.cvAvatar.setCardBackgroundColor(
                    Color.parseColor(colors[station.name.hashCode().and(0x7FFFFFFF) % colors.size])
                )
                b.tvAvatar.text = station.getAvatarText()
            }

            // Favori
            updateFavIcon(station.isFavorite)

            // Oynatılıyor vurgusu
            b.root.setCardBackgroundColor(ctx.getColor(R.color.bg_card))
            b.rowHighlight.setBackgroundColor(
                ctx.getColor(if (isPlaying) R.color.bg_card_playing else R.color.bg_card)
            )
            b.tvStationName.setTextColor(
                ctx.getColor(if (isPlaying) R.color.text_playing else R.color.text_primary)
            )

            if (isPlaying) {
                b.playingDot.visibility = View.VISIBLE
                b.playingDot.startAnimation(AnimationUtils.loadAnimation(ctx, R.anim.playing_pulse))
            } else {
                b.playingDot.clearAnimation()
                b.playingDot.visibility = View.INVISIBLE
            }

            b.root.setOnClickListener { onStationClick(station) }
            b.ivFavorite.setOnClickListener { v ->
                v.isEnabled = false
                onFavoriteClick(station)
                v.postDelayed({ v.isEnabled = true }, 500)
            }
        }

        fun updateFavIcon(isFav: Boolean) {
            FavoriteIcon.apply(b.ivFavorite, isFav)
        }

        /** Sadece rütbe rozetini ve dinlenme sayısını günceller (tam rebind yapmadan) */
        fun updatePlayCount(rank: Int, playCount: Int) {
            val ctx = b.root.context
            b.tvRank.text = rank.toString()
            b.tvRank.setBackgroundColor(
                when (rank) {
                    1 -> Color.parseColor("#FFD700")
                    2 -> Color.parseColor("#C0C0C0")
                    3 -> Color.parseColor("#CD7F32")
                    else -> Color.parseColor("#1E2A3A")
                }
            )
            b.tvRank.setTextColor(if (rank <= 3) Color.parseColor("#0A0A1A") else Color.WHITE)
            b.tvPlayCount.text = if (playCount > 0)
                ctx.getString(R.string.play_count_times, playCount)
            else
                ctx.getString(R.string.play_count_first_time)
        }
    }

    companion object {
        private const val PAYLOAD_FAVORITE = "favorite"
        private const val PAYLOAD_PLAYCOUNT = "playcount"

        val DIFF = object : DiffUtil.ItemCallback<RadioStation>() {
            override fun areItemsTheSame(a: RadioStation, b: RadioStation) = a.id == b.id
            override fun areContentsTheSame(a: RadioStation, b: RadioStation) = a == b
        }
    }
}