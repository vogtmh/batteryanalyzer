package com.mavodev.batteryanalyzer

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.mavodev.batteryanalyzer.adapter.HistoryAdapter
import com.mavodev.batteryanalyzer.adapter.HistoryListItem
import com.mavodev.batteryanalyzer.databinding.ActivityHistoryBinding
import com.mavodev.batteryanalyzer.util.HistoryManager

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var historyManager: HistoryManager
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window.statusBarColor = getColor(R.color.blue_700)
        
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        historyManager = HistoryManager(this)

        setupRecyclerView()
        loadHistory()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter { entry ->
            showDeleteConfirmationDialog(entry.id)
        }

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = historyAdapter
        }
    }

    private fun loadHistory() {
        val entries = historyManager.getAllEntries()
        
        if (entries.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.rvHistory.visibility = View.VISIBLE
            
            // Build list with chart as first item if we have health data
            val listItems = mutableListOf<HistoryListItem>()
            
            val entriesWithHealth = entries.filter { it.healthPercentage != null }
            if (entriesWithHealth.size >= 2) {
                listItems.add(HistoryListItem.ChartItem(entries))
            }
            
            // Add all history entries
            entries.forEach { entry ->
                listItems.add(HistoryListItem.EntryItem(entry))
            }
            
            historyAdapter.submitList(listItems)
        }
    }

    private fun showDeleteConfirmationDialog(entryId: String) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.history_delete_title))
            .setMessage(getString(R.string.history_delete_message))
            .setPositiveButton(getString(R.string.history_delete_confirm)) { _, _ ->
                historyManager.deleteEntry(entryId)
                loadHistory()
            }
            .setNegativeButton(getString(R.string.settings_cancel), null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
