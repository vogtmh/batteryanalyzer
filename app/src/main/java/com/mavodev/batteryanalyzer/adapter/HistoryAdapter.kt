package com.mavodev.batteryanalyzer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mavodev.batteryanalyzer.R
import com.mavodev.batteryanalyzer.databinding.ItemChartBinding
import com.mavodev.batteryanalyzer.databinding.ItemHistoryBinding
import com.mavodev.batteryanalyzer.model.HistoryEntry
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

sealed class HistoryListItem {
    data class ChartItem(val entries: List<HistoryEntry>) : HistoryListItem()
    data class EntryItem(val entry: HistoryEntry) : HistoryListItem()
    data class ReplacementHeader(val newBatteryDate: String) : HistoryListItem()
}

class HistoryAdapter(
    private val onDeleteClick: (HistoryEntry) -> Unit
) : ListAdapter<HistoryListItem, RecyclerView.ViewHolder>(HistoryDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_CHART = 0
        private const val VIEW_TYPE_ENTRY = 1
        private const val VIEW_TYPE_REPLACEMENT = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HistoryListItem.ChartItem -> VIEW_TYPE_CHART
            is HistoryListItem.EntryItem -> VIEW_TYPE_ENTRY
            is HistoryListItem.ReplacementHeader -> VIEW_TYPE_REPLACEMENT
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
            VIEW_TYPE_REPLACEMENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_battery_replacement, parent, false)
                ReplacementViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HistoryListItem.ChartItem -> (holder as ChartViewHolder).bind(item.entries)
            is HistoryListItem.EntryItem -> (holder as HistoryViewHolder).bind(item.entry)
            is HistoryListItem.ReplacementHeader -> (holder as ReplacementViewHolder).bind(item.newBatteryDate)
        }
    }
    
    fun submitHistoryList(entries: List<HistoryEntry>) {
        if (entries.isEmpty()) {
            submitList(emptyList())
            return
        }
        
        val items = mutableListOf<HistoryListItem>()
        
        // Add chart
        items.add(HistoryListItem.ChartItem(entries))
        
        // Process entries and detect replacements
        // Entries are sorted by timestamp descending (newest first)
        
        // Add newest entry
        items.add(HistoryListItem.EntryItem(entries[0]))
        
        for (i in 1 until entries.size) {
            val currentEntry = entries[i]
            val previousEntry = entries[i - 1] // The newer entry we just processed
            
            // Check if first use date changed
            // If previous (newer) has date A and current (older) has date B
            // Then a replacement happened between them (at date A)
            val currentDate = currentEntry.firstUseDate
            val previousDate = previousEntry.firstUseDate
            
            if (currentDate != null && previousDate != null && currentDate != previousDate) {
                items.add(HistoryListItem.ReplacementHeader(previousDate))
            }
            
            items.add(HistoryListItem.EntryItem(currentEntry))
        }
        
        submitList(items)
    }

    class ChartViewHolder(
        private val binding: ItemChartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entries: List<HistoryEntry>) {
            binding.chartView.setData(entries)
        }
    }
    class ReplacementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvBatteryReplaced: android.widget.TextView = itemView.findViewById(R.id.tvBatteryReplaced)
        
        fun bind(date: String) {
            // date is YYYY-MM-DD
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
                val parsedDate = inputFormat.parse(date)
                if (parsedDate != null) {
                    val formattedDate = outputFormat.format(parsedDate)
                    tvBatteryReplaced.text = "Battery Replaced on $formattedDate"
                } else {
                     tvBatteryReplaced.text = "Battery Replaced on $date"
                }
            } catch (e: Exception) {
                tvBatteryReplaced.text = "Battery Replaced on $date"
            }
        }
    }

    inner class HistoryViewHolder(
        private val binding: ItemHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var isExpanded = false

        fun bind(entry: HistoryEntry) {
            // Set health as title
            if (entry.healthPercentage != null) {
                val df = DecimalFormat("#.#")
                val text = "${df.format(entry.healthPercentage)}%"
                // Add device model as well? User said "replace it", so just health.
                // But maybe "Health: 98%"? User said "98.5%".
                binding.tvHealthTitle.text = text
                binding.tvHealthTitle.setTextColor(getHealthColor(entry.healthPercentage))
            } else {
                binding.tvHealthTitle.text = "N/A"
                binding.tvHealthTitle.setTextColor(
                    ContextCompat.getColor(binding.root.context, android.R.color.darker_gray)
                )
            }

            // Set logfile date in subtitle
            if (entry.logfileTimestamp != null) {
                binding.tvLogfileDate.text = entry.logfileTimestamp
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
            
            if (entry.calculatedHealthPercentage != null && 
                entry.calculatedHealthPercentage != entry.healthPercentage) {
                val df = DecimalFormat("#.#")
                binding.tvCalculatedHealth.text = "${df.format(entry.calculatedHealthPercentage)}%"
                binding.calculatedHealthLayout.visibility = View.VISIBLE
            } else {
                binding.calculatedHealthLayout.visibility = View.GONE
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
                oldItem is HistoryListItem.ReplacementHeader && newItem is HistoryListItem.ReplacementHeader ->
                    oldItem.newBatteryDate == newItem.newBatteryDate
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: HistoryListItem, newItem: HistoryListItem): Boolean {
            return oldItem == newItem
        }
    }
}
