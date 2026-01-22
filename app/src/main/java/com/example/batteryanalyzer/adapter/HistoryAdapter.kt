package com.example.batteryanalyzer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.batteryanalyzer.R
import com.example.batteryanalyzer.databinding.ItemChartBinding
import com.example.batteryanalyzer.databinding.ItemHistoryBinding
import com.example.batteryanalyzer.model.HistoryEntry
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

sealed class HistoryListItem {
    data class ChartItem(val entries: List<HistoryEntry>) : HistoryListItem()
    data class EntryItem(val entry: HistoryEntry) : HistoryListItem()
}

class HistoryAdapter(
    private val onDeleteClick: (HistoryEntry) -> Unit
) : ListAdapter<HistoryListItem, RecyclerView.ViewHolder>(HistoryDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_CHART = 0
        private const val VIEW_TYPE_ENTRY = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HistoryListItem.ChartItem -> VIEW_TYPE_CHART
            is HistoryListItem.EntryItem -> VIEW_TYPE_ENTRY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CHART -> {
                val binding = ItemChartBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                ChartViewHolder(binding)
            }
            VIEW_TYPE_ENTRY -> {
                val binding = ItemHistoryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                HistoryViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HistoryListItem.ChartItem -> (holder as ChartViewHolder).bind(item.entries)
            is HistoryListItem.EntryItem -> (holder as HistoryViewHolder).bind(item.entry)
        }
    }

    class ChartViewHolder(
        private val binding: ItemChartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entries: List<HistoryEntry>) {
            binding.chartView.setData(entries)
        }
    }

    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var isExpanded = false

        fun bind(entry: HistoryEntry) {
            binding.tvDeviceModel.text = entry.deviceModel ?: "Unknown Device"

            // Set health in subtitle
            if (entry.healthPercentage != null) {
                val df = DecimalFormat("#.#")
                binding.tvHealthPercentage.text = "${df.format(entry.healthPercentage)}%"
                binding.tvHealthPercentage.setTextColor(getHealthColor(entry.healthPercentage))
            } else {
                binding.tvHealthPercentage.text = "N/A"
                binding.tvHealthPercentage.setTextColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.darker_gray)
                )
            }

            // Set logfile date in subtitle
            if (entry.logfileTimestamp != null) {
                binding.tvLogfileDate.text = "• ${entry.logfileTimestamp}"
                binding.tvLogfileDate.visibility = View.VISIBLE
            } else {
                binding.tvLogfileDate.visibility = View.GONE
            }

            if (entry.currentCapacityMah != null) {
                binding.tvCurrentCapacity.text = "${entry.currentCapacityMah} mAh"
            } else {
                binding.tvCurrentCapacity.text = "N/A"
            }

            if (entry.designCapacityMah != null) {
                binding.tvDesignCapacity.text = "${entry.designCapacityMah} mAh"
            } else {
                binding.tvDesignCapacity.text = "N/A"
            }

            if (entry.cycleCount != null) {
                binding.tvCycleCount.text = "${entry.cycleCount}"
            } else {
                binding.tvCycleCount.text = "N/A"
            }

            if (entry.stateOfCharge != null) {
                binding.tvStateOfCharge.text = "${entry.stateOfCharge}%"
                binding.socLayout.visibility = View.VISIBLE
            } else {
                binding.socLayout.visibility = View.GONE
            }

            if (entry.firstUseDate != null) {
                binding.tvFirstUseDate.text = entry.firstUseDate
                binding.firstUseLayout.visibility = View.VISIBLE
            } else {
                binding.firstUseLayout.visibility = View.GONE
            }

            // Set up collapse/expand functionality
            updateExpandedState()
            
            binding.root.setOnClickListener {
                isExpanded = !isExpanded
                updateExpandedState()
            }

            binding.btnDelete.setOnClickListener {
                onDeleteClick(entry)
            }
        }

        private fun updateExpandedState() {
            binding.detailsLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.divider.visibility = if (isExpanded) View.VISIBLE else View.GONE
            
            // Rotate arrow with animation
            val rotation = if (isExpanded) 180f else 0f
            binding.expandArrow.animate()
                .rotation(rotation)
                .setDuration(200)
                .start()
        }

        private fun getHealthColor(healthPercentage: Double): Int {
            return when {
                healthPercentage >= 95 -> ContextCompat.getColor(binding.root.context, R.color.health_excellent)
                healthPercentage >= 85 -> ContextCompat.getColor(binding.root.context, R.color.health_good)
                healthPercentage >= 75 -> ContextCompat.getColor(binding.root.context, R.color.health_fair)
                healthPercentage >= 65 -> ContextCompat.getColor(binding.root.context, R.color.health_poor)
                else -> ContextCompat.getColor(binding.root.context, R.color.health_critical)
            }
        }
    }

    private class HistoryDiffCallback : DiffUtil.ItemCallback<HistoryListItem>() {
        override fun areItemsTheSame(oldItem: HistoryListItem, newItem: HistoryListItem): Boolean {
            return when {
                oldItem is HistoryListItem.ChartItem && newItem is HistoryListItem.ChartItem -> true
                oldItem is HistoryListItem.EntryItem && newItem is HistoryListItem.EntryItem -> 
                    oldItem.entry.id == newItem.entry.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: HistoryListItem, newItem: HistoryListItem): Boolean {
            return oldItem == newItem
        }
    }
}
