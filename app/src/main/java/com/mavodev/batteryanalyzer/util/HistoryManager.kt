package com.mavodev.batteryanalyzer.util

import android.content.Context
import android.content.SharedPreferences
import com.mavodev.batteryanalyzer.model.BatteryInfo
import com.mavodev.batteryanalyzer.model.HistoryEntry
import org.json.JSONArray
import org.json.JSONObject

class HistoryManager(context: Context) {
    
    private val prefs: SharedPreferences = 
        context.getSharedPreferences("BatteryAnalyzerHistory", Context.MODE_PRIVATE)
    
    companion object {
        private const val KEY_HISTORY = "history_entries"
        private const val MAX_ENTRIES = 100 // Keep last 100 entries
    }
    
    /**
     * Save a new battery analysis result to history
     * @param batteryInfo The battery info to save
     * @return true if saved, false if it's a duplicate
     */
    fun saveResult(batteryInfo: BatteryInfo): Boolean {
        val newEntry = HistoryEntry.fromBatteryInfo(batteryInfo)
        val currentEntries = getAllEntries().toMutableList()
        
        // Check for duplicates
        val isDuplicate = currentEntries.any { it.isDuplicateOf(newEntry) }
        if (isDuplicate) {
            return false // Don't save duplicates
        }
        
        // Add new entry at the beginning
        currentEntries.add(0, newEntry)
        
        // Keep only the most recent MAX_ENTRIES
        val entriesToSave = currentEntries.take(MAX_ENTRIES)
        
        // Save to SharedPreferences
        saveEntries(entriesToSave)
        
        return true
    }
    
    /**
     * Get all history entries, sorted by timestamp (newest first)
     */
    fun getAllEntries(): List<HistoryEntry> {
        val jsonString = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        
        return try {
            val jsonArray = JSONArray(jsonString)
            val entries = mutableListOf<HistoryEntry>()
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                entries.add(HistoryEntry.fromJson(jsonObject))
            }
            
            entries.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Delete a specific history entry
     */
    fun deleteEntry(entryId: String) {
        val currentEntries = getAllEntries().toMutableList()
        currentEntries.removeAll { it.id == entryId }
        saveEntries(currentEntries)
    }
    
    /**
     * Clear all history entries
     */
    fun clearAllHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }
    
    /**
     * Get the count of history entries
     */
    fun getEntryCount(): Int {
        return getAllEntries().size
    }
    
    private fun saveEntries(entries: List<HistoryEntry>) {
        val jsonArray = JSONArray()
        entries.forEach { entry ->
            jsonArray.put(entry.toJson())
        }
        
        prefs.edit()
            .putString(KEY_HISTORY, jsonArray.toString())
            .apply()
    }
}
