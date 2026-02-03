package com.mavodev.batteryanalyzer

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import android.graphics.Color
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.mavodev.batteryanalyzer.databinding.ActivitySettingsBinding
import com.mavodev.batteryanalyzer.model.HistoryEntry
import com.mavodev.batteryanalyzer.util.HistoryManager
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var historyManager: HistoryManager
    
    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { exportToFile(it) }
    }
    
    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importFromFile(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop + systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        historyManager = HistoryManager(this)
        loadSettings()
        setupListeners()
    }
    
    private fun loadSettings() {
        val prefs = getSharedPreferences("BatteryAnalyzer", MODE_PRIVATE)
        val capacity = prefs.getInt("design_capacity", -1)
        if (capacity > 0) {
            binding.etDesignCapacity.setText(capacity.toString())
        }
    }
    
    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
        
        binding.btnClear.setOnClickListener {
            clearSettings()
        }
        
        binding.btnExport.setOnClickListener {
            exportData()
        }
        
        binding.btnImport.setOnClickListener {
            importData()
        }
    }
    
    private fun saveSettings() {
        val capacityText = binding.etDesignCapacity.text.toString()
        val capacity = capacityText.toIntOrNull()
        
        val prefs = getSharedPreferences("BatteryAnalyzer", MODE_PRIVATE)
        
        if (capacity != null && capacity in 2000..15000) {
            prefs.edit().putInt("design_capacity", capacity).apply()
            Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
            finish()
        } else if (capacityText.isEmpty()) {
            prefs.edit().remove("design_capacity").apply()
            Toast.makeText(this, getString(R.string.settings_cleared), Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Invalid capacity. Please enter a value between 2000-15000 mAh", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun clearSettings() {
        val prefs = getSharedPreferences("BatteryAnalyzer", MODE_PRIVATE)
        prefs.edit().remove("design_capacity").apply()
        binding.etDesignCapacity.setText("")
        Toast.makeText(this, getString(R.string.settings_cleared), Toast.LENGTH_SHORT).show()
    }
    
    private fun exportData() {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val filename = "battery_analyzer_backup_${dateFormat.format(Date())}.json"
        exportLauncher.launch(filename)
    }
    
    private fun exportToFile(uri: android.net.Uri) {
        try {
            val prefs = getSharedPreferences("BatteryAnalyzer", MODE_PRIVATE)
            val designCapacity = prefs.getInt("design_capacity", -1)
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            
            val exportData = JSONObject().apply {
                put("version", versionName)
                put("exportDate", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date()))
                
                // Export settings
                val settings = JSONObject()
                if (designCapacity > 0) {
                    settings.put("design_capacity", designCapacity)
                }
                put("settings", settings)
                
                // Export history
                val historyJson = historyManager.exportAllEntries()
                put("history", JSONArray(historyJson))
            }
            
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(exportData.toString(2).toByteArray())
            }
            
            Toast.makeText(this, getString(R.string.export_success), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.import_error, e.message), Toast.LENGTH_LONG).show()
        }
    }
    
    private fun importData() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.import_confirm_title))
            .setMessage(getString(R.string.import_confirm_message))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                importLauncher.launch(arrayOf("application/json"))
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
    
    private fun importFromFile(uri: android.net.Uri) {
        try {
            val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: throw Exception("Could not read file")
            
            val importData = JSONObject(jsonString)
            
            // Import settings
            val settings = importData.optJSONObject("settings")
            val prefs = getSharedPreferences("BatteryAnalyzer", MODE_PRIVATE)
            val editor = prefs.edit()
            
            if (settings != null && settings.has("design_capacity")) {
                editor.putInt("design_capacity", settings.getInt("design_capacity"))
            } else {
                editor.remove("design_capacity")
            }
            editor.apply()
            
            // Import history
            val historyArray = importData.optJSONArray("history")
            if (historyArray != null) {
                val entries = mutableListOf<HistoryEntry>()
                for (i in 0 until historyArray.length()) {
                    val entryJson = historyArray.getJSONObject(i)
                    entries.add(HistoryEntry.fromJson(entryJson))
                }
                historyManager.importEntries(entries)
            }
            
            Toast.makeText(this, getString(R.string.import_success), Toast.LENGTH_SHORT).show()
            loadSettings() // Reload UI
        } catch (e: Exception) {
            Toast.makeText(this, getString(R.string.import_error, e.message), Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}