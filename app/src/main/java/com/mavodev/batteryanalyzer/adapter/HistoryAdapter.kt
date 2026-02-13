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
            // Reported Health Title
            val reportedHealth = entry.healthPercentage
            if (reportedHealth != null) {
                val df = DecimalFormat("#.#")
                binding.tvReportedHealthTitle.text = "${df.format(reportedHealth)}%"
                binding.tvReportedHealthTitle.setTextColor(getHealthColor(reportedHealth))
                
                val healthColor = getHealthColor(reportedHealth)
                binding.ivReportedHealth.imageTintList = 
                    android.content.res.ColorStateList.valueOf(healthColor)
                binding.layoutReportedHealth.visibility = View.VISIBLE
            } else {
                binding.layoutReportedHealth.visibility = View.GONE
            }

            // Calculated Health Title
            val calculatedHealth = entry.calculatedHealthPercentage
            if (calculatedHealth != null) {
                val df = DecimalFormat("#.#")
                binding.tvCalculatedHealthTitle.text = "${df.format(calculatedHealth)}%"
                binding.tvCalculatedHealthTitle.setTextColor(getHealthColor(calculatedHealth))
                
                val healthColor = getHealthColor(calculatedHealth)
                binding.ivCalculatedHealth.imageTintList = 
                    android.content.res.ColorStateList.valueOf(healthColor)
                binding.layoutCalculatedHealth.visibility = View.VISIBLE
            } else {
                binding.layoutCalculatedHealth.visibility = View.GONE
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

            if (entry.ratedCapacityMah != null && entry.ratedCapacityMah > 0) {
                binding.tvRatedCapacity.text = "${entry.ratedCapacityMah} mAh"
                binding.ratedCapacityLayout.visibility = View.VISIBLE
                // Hide typical capacity in history if we have rated capacity
                binding.typicalCapacityLayout.visibility = View.GONE
            } else {
                binding.ratedCapacityLayout.visibility = View.GONE
                if (entry.designCapacityMah != null) {
                    binding.tvTypicalCapacity.text = "${entry.designCapacityMah} mAh"
                    binding.typicalCapacityLayout.visibility = View.VISIBLE
                } else {
                    binding.typicalCapacityLayout.visibility = View.GONE
                }
            }
            
            // Details Section - Reported Health
            if (reportedHealth != null) {
                val df = DecimalFormat("#.#")
                binding.tvReportedHealth.text = "${df.format(reportedHealth)}%"
                binding.reportedHealthLayout.visibility = View.VISIBLE
            } else {
                binding.reportedHealthLayout.visibility = View.GONE
            }

            // Details Section - Calculated Health
            if (calculatedHealth != null) {
                val df = DecimalFormat("#.#")
                binding.tvCalculatedHealth.text = "${df.format(calculatedHealth)}%"
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
