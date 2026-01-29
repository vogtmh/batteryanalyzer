package com.mavodev.batteryanalyzer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mavodev.batteryanalyzer.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = getColor(R.color.blue_700)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        displayBuildInfo()
    }
    
    private fun displayBuildInfo() {
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val versionName = packageInfo.versionName
            
            binding.tvVersion.text = "Version $versionName"
            binding.tvBuildDate.text = "Build Date: January 22, 2026"
        } catch (e: Exception) {
            binding.tvVersion.text = "Version information unavailable"
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
