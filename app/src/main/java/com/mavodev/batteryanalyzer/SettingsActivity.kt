package com.mavodev.batteryanalyzer

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mavodev.batteryanalyzer.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = getColor(R.color.blue_700)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
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
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}