package com.globalradio.livetuneinogzapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.globalradio.livetuneinogzapp.utils.StationImages
import com.globalradio.livetuneinogzapp.R
import com.globalradio.livetuneinogzapp.databinding.ItemCountryBinding
import com.globalradio.livetuneinogzapp.model.CountrySummary

class CountryAdapter(
    private val onCountryClick: (CountrySummary) -> Unit
) : ListAdapter<CountrySummary, CountryAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemCountryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ViewHolder) {
        holder.clear()
        super.onViewRecycled(holder)
    }

    inner class ViewHolder(private val b: ItemCountryBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(summary: CountrySummary) {
            b.tvCountryName.text = summary.name
            b.tvStationCount.text =
                b.root.context.getString(R.string.station_count, summary.stationCount)

            val flagUrl = summary.flagImageUrl
            if (flagUrl != null) {
                b.ivFlag.visibility = View.VISIBLE
                b.tvFlag.visibility = View.GONE
                StationImages.loadFlag(
                    b.ivFlag,
                    flagUrl,
                    R.drawable.bg_flag,
                    onFail = {
                        b.ivFlag.visibility = View.GONE
                        b.tvFlag.visibility = View.VISIBLE
                        b.tvFlag.text = summary.flagEmoji
                    },
                    onReady = {
                        b.tvFlag.visibility = View.GONE
                        b.ivFlag.visibility = View.VISIBLE
                    }
                )
            } else {
                b.ivFlag.setImageDrawable(null)
                b.ivFlag.visibility = View.GONE
                b.tvFlag.visibility = View.VISIBLE
                b.tvFlag.text = summary.flagEmoji
            }

            b.root.setOnClickListener { onCountryClick(summary) }
        }

        fun clear() {
            b.ivFlag.setImageDrawable(null)
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<CountrySummary>() {
            override fun areItemsTheSame(a: CountrySummary, b: CountrySummary) =
                a.name == b.name && a.isoCode == b.isoCode

            override fun areContentsTheSame(a: CountrySummary, b: CountrySummary) =
                a == b
        }
    }
}
