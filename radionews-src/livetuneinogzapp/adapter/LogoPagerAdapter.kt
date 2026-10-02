package com.globalradio.livetuneinogzapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.globalradio.livetuneinogzapp.utils.StationImages
import com.globalradio.livetuneinogzapp.databinding.ItemLogoPagerBinding
import com.globalradio.livetuneinogzapp.model.RadioStation

class LogoPagerAdapter(
    private val onStationVisible: (RadioStation) -> Unit
) : RecyclerView.Adapter<LogoPagerAdapter.VH>() {

    private var stations: List<RadioStation> = emptyList()

    /**
     * Listeyi DiffUtil ile günceller. calculateDiff senkron çalıştığı için
     * bu fonksiyon döndüğünde `stations` zaten güncel olur — çağıran taraf
     * (PlayerBottomSheet) hemen ardından indexOf() ile doğru sonucu alabilir.
     */
    fun setStations(list: List<RadioStation>) {
        val old = stations
        val diffResult = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = old.size
            override fun getNewListSize() = list.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                old[oldPos].id == list[newPos].id

            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                old[oldPos] == list[newPos]
        })
        stations = list
        diffResult.dispatchUpdatesTo(this)
    }

    fun getStation(position: Int): RadioStation? = stations.getOrNull(position)
    fun indexOf(station: RadioStation) = stations.indexOfFirst { it.id == station.id }
    override fun getItemCount() = stations.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemLogoPagerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(stations[position])
    }

    inner class VH(private val binding: ItemLogoPagerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(station: RadioStation) {
            if (station.hasValidFavicon()) {
                StationImages.loadLogo(binding.ivPagerLogo, station.favicon)
            } else {
                binding.ivPagerLogo.setImageResource(
                    com.globalradio.livetuneinogzapp.R.drawable.ic_radio_placeholder
                )
            }
        }
    }
}