package com.example.batteryanalyzer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.batteryanalyzer.adapter.FileListAdapter
import com.example.batteryanalyzer.databinding.ActivityMainBinding
import com.example.batteryanalyzer.model.BatteryInfo
import com.example.batteryanalyzer.model.FileInfo
import com.example.batteryanalyzer.parser.DumpstateParser
import com.example.batteryanalyzer.util.HistoryManager
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val parser = DumpstateParser()
    private lateinit var fileAdapter: FileListAdapter
    private lateinit var historyManager: HistoryManager
    
    // State preservation
    private var currentBatteryInfo: BatteryInfo? = null
    private var currentFiles: List<FileInfo>? = null
    private var currentError: String? = null
    private var isParsing: Boolean = false
    private var currentFileUri: String? = null
    private var currentFilePath: String? = null
    private var isFirstLaunch = true
    private var hasRequestedPermission = false
    
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { parseFileFromUri(it) }
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        updateDetectFilesButton(isGranted)
        if (isGranted) {
            scanForFiles()
        } else {
            showPermissionDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
        
        // Keep status bar transparent for edge-to-edge drawer
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        historyManager = HistoryManager(this)
        setupToolbar()
        setupDrawer()
        setupRecyclerView()
        setupClickListeners()
        
        // Restore state after rotation
        savedInstanceState?.let {
            isFirstLaunch = false
            hasRequestedPermission = it.getBoolean("has_requested_permission", false)
            
            currentBatteryInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable("battery_info", BatteryInfo::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable("battery_info")
            }
            
            val fileList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelableArrayList("file_list", FileInfo::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelableArrayList("file_list")
            }
            
            currentError = it.getString("error_message")
            isParsing = it.getBoolean("is_parsing", false)
            currentFileUri = it.getString("file_uri")
            currentFilePath = it.getString("file_path")
            
            // Restore UI state
            currentBatteryInfo?.let { info -> displayResults(info) }
            currentError?.let { error -> showError(error) }
            
            // Only show file list if we don't have results or error displayed
            if (currentBatteryInfo == null && currentError == null) {
                // Restart parsing if we were in the middle of it
                if (isParsing) {
                    currentFileUri?.let { uriString -> 
                        parseFileFromUri(Uri.parse(uriString))
                    } ?: currentFilePath?.let { path ->
                        parseFileFromPath(File(path))
                    }
                } else {
                    fileList?.let { files -> 
                        currentFiles = files
                        showFileList(files)
                    }
                }
            }
            
            // Update button state
            updateDetectFilesButton(hasStoragePermission())
        } ?: run {
            // First time launch - request permission and scan if granted
            if (hasStoragePermission()) {
                scanForFiles()
            } else {
                requestStoragePermission()
                hasRequestedPermission = true
                updateDetectFilesButton(false)
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Check if permission was granted while we were in settings
        if (hasRequestedPermission && hasStoragePermission()) {
            updateDetectFilesButton(true)
            if (isFirstLaunch) {
                scanForFiles()
                isFirstLaunch = false
            }
        } else {
            updateDetectFilesButton(hasStoragePermission())
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("has_requested_permission", hasRequestedPermission)
        currentBatteryInfo?.let {
            outState.putParcelable("battery_info", it)
        }
        currentFiles?.let {
            outState.putParcelableArrayList("file_list", ArrayList(it))
        }
        currentError?.let {
            outState.putString("error_message", it)
        }
        outState.putBoolean("is_parsing", isParsing)
        currentFileUri?.let {
            outState.putString("file_uri", it)
        }
        currentFilePath?.let {
            outState.putString("file_path", it)
        }
    }

    private fun setupRecyclerView() {
        fileAdapter = FileListAdapter { fileInfo ->
            parseFileFromPath(fileInfo.file)
        }
        
        binding.rvFiles.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = fileAdapter
        }
    }
    
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
    }
    
    private fun setupDrawer() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.action_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    true
                }
                R.id.action_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                R.id.action_help -> {
                    startActivity(Intent(this, HelpActivity::class.java))
                    true
                }
                R.id.action_about -> {
                    startActivity(Intent(this, AboutActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                binding.drawerLayout.openDrawer(GravityCompat.START)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private fun setupClickListeners() {
        binding.btnScanFiles.setOnClickListener {
            if (hasStoragePermission()) {
                scanForFiles()
            } else {
                requestStoragePermission()
            }
        }
        
        binding.btnSelectFile.setOnClickListener {
            openFilePicker()
        }
    }
    
    private fun updateDetectFilesButton(enabled: Boolean) {
        binding.btnScanFiles.isEnabled = enabled
        binding.btnScanFiles.alpha = if (enabled) 1.0f else 0.5f
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            showPermissionDialog()
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage("This app needs storage access to scan for dumpstate files. You can grant permission in Settings or use \"Browse Files\" to select files manually.")
            .setPositiveButton(getString(R.string.grant_permission)) { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
            }
            .setNegativeButton("Use Browse") { _, _ ->
                openFilePicker()
            }
            .show()
    }

    private fun scanForFiles() {
        binding.tvStatus.text = getString(R.string.scanning_files)
        binding.tvStatus.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            val files = withContext(Dispatchers.IO) {
                findDumpstateFiles()
            }
            
            binding.tvStatus.visibility = View.GONE
            
            if (files.isNotEmpty()) {
                showFileList(files)
            } else {
                showNoFilesFound()
            }
        }
    }

    private fun findDumpstateFiles(): List<FileInfo> {
        val files = mutableListOf<FileInfo>()
        val seenPaths = mutableSetOf<String>()
        
        // Common locations for dumpstate files (prioritized order)
        val searchPaths = listOf(
            File(Environment.getExternalStorageDirectory(), "log"),
            File(Environment.getExternalStorageDirectory(), "Documents"),
            File(Environment.getExternalStorageDirectory(), "Download")
        )
        
        searchPaths.forEach { dir ->
            if (dir.exists() && dir.isDirectory && dir.canRead()) {
                try {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile && isDumpstateFile(file)) {
                            val canonicalPath = try {
                                file.canonicalPath
                            } catch (e: Exception) {
                                file.absolutePath
                            }
                            if (!seenPaths.contains(canonicalPath)) {
                                seenPaths.add(canonicalPath)
                                files.add(FileInfo(
                                    path = file.absolutePath,
                                    name = file.name,
                                    sizeBytes = file.length(),
                                    lastModified = file.lastModified()
                                ))
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    // Ignore directories we can't access
                }
            }
        }
        
        // Sort by last modified (newest first)
        return files.sortedByDescending { it.lastModified }
    }

    private fun isDumpstateFile(file: File): Boolean {
        val name = file.name.lowercase()
        return ((name.startsWith("dumpstate") || 
                name.startsWith("bugreport") ||
                name.contains("dumpstate")) &&
                (name.endsWith(".txt") || name.endsWith(".log") || name.endsWith(".zip")) &&
                file.length() > 1024 * 1024) // At least 1MB
    }

    private fun showFileList(files: List<FileInfo>) {
        currentFiles = files
        binding.tvFileListTitle.visibility = View.VISIBLE
        binding.rvFiles.visibility = View.VISIBLE
        binding.tvNoFiles.visibility = View.GONE
        fileAdapter.submitList(files)
    }

    private fun showNoFilesFound() {
        binding.tvFileListTitle.visibility = View.VISIBLE
        binding.rvFiles.visibility = View.GONE
        binding.tvNoFiles.visibility = View.VISIBLE
    }

    private fun openFilePicker() {
        filePickerLauncher.launch(arrayOf("text/plain", "application/zip", "*/*"))
    }

    private fun parseFileFromUri(uri: Uri) {
        isParsing = true
        currentFileUri = uri.toString()
        currentFilePath = null
        
        lifecycleScope.launch {
            showLoading(true)
            hideResults()
            hideError()
            hideFileList()

            try {
                val batteryInfo = withContext(Dispatchers.IO) {
                    // Check if it's a ZIP file
                    val fileName = getFileName(uri)
                    if (fileName?.endsWith(".zip", ignoreCase = true) == true) {
                        // Extract and parse the largest txt file from ZIP
                        extractAndParseZip(uri)
                    } else {
                        // Parse regular text file
                        contentResolver.openInputStream(uri)?.use { inputStream ->
                            parser.parse(inputStream) { lineCount ->
                                if (lineCount > 0) {
                                    runOnUiThread {
                                        binding.tvStatus.text = getString(
                                            R.string.parsing_file,
                                            lineCount
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                showLoading(false)

                if (batteryInfo != null && batteryInfo.hasData) {
                    val finalBatteryInfo = applyManualDesignCapacity(batteryInfo)
                    displayResults(finalBatteryInfo)
                } else {
                    showError(getString(R.string.error_no_data))
                }

            } catch (e: Exception) {
                showLoading(false)
                showError(getString(R.string.error_reading_file, e.message ?: "Unknown error"))
            } finally {
                isParsing = false
                currentFileUri = null
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            cursor.getString(nameIndex)
        }
    }

    private fun extractAndParseZip(uri: Uri): BatteryInfo? {
        var extractedFile: File? = null
        try {
            return contentResolver.openInputStream(uri)?.use { inputStream ->
                val zipInputStream = java.util.zip.ZipInputStream(inputStream)
                var largestEntryName: String? = null
                var largestSize = 0L
                
                // First pass: find the largest txt/log file
                var entry = zipInputStream.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && !entry.name.contains("/") && 
                        (entry.name.endsWith(".txt", ignoreCase = true) || 
                         entry.name.endsWith(".log", ignoreCase = true))) {
                        
                        if (entry.size > largestSize) {
                            largestSize = entry.size
                            largestEntryName = entry.name
                        }
                    }
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
                
                largestEntryName ?: return@use null
                
                // Second pass: extract the largest file to cache
                contentResolver.openInputStream(uri)?.use { secondInputStream ->
                    val secondZipStream = java.util.zip.ZipInputStream(secondInputStream)
                    var secondEntry = secondZipStream.nextEntry
                    
                    while (secondEntry != null) {
                        if (secondEntry.name == largestEntryName) {
                            // Extract to cache directory
                            extractedFile = File(cacheDir, "temp_bugreport.txt")
                            extractedFile?.outputStream()?.use { output ->
                                secondZipStream.copyTo(output)
                            }
                            
                            runOnUiThread {
                                binding.tvStatus.text = "Extracted: $largestEntryName"
                            }
                            
                            // Parse the extracted file
                            val result = extractedFile?.inputStream()?.use { stream ->
                                parser.parse(stream) { lineCount ->
                                    if (lineCount > 0) {
                                        runOnUiThread {
                                            binding.tvStatus.text = getString(
                                                R.string.parsing_file,
                                                lineCount
                                            )
                                        }
                                    }
                                }
                            }
                            
                            secondZipStream.closeEntry()
                            return@use result
                        }
                        secondZipStream.closeEntry()
                        secondEntry = secondZipStream.nextEntry
                    }
                    null
                }
            }
        } finally {
            // Clean up extracted file
            extractedFile?.delete()
        }
    }

    private fun parseFileFromPath(file: File) {
        isParsing = true
        currentFilePath = file.absolutePath
        currentFileUri = null
        
        lifecycleScope.launch {
            showLoading(true)
            hideResults()
            hideError()
            hideFileList()

            try {
                val batteryInfo = withContext(Dispatchers.IO) {
                    // Check if it's a ZIP file
                    if (file.name.endsWith(".zip", ignoreCase = true)) {
                        // Extract and parse the largest txt file from ZIP
                        extractAndParseZipFromFile(file)
                    } else {
                        // Parse regular text file
                        file.inputStream().use { inputStream ->
                            parser.parse(inputStream) { lineCount ->
                                if (lineCount > 0) {
                                    runOnUiThread {
                                        binding.tvStatus.text = getString(
                                            R.string.parsing_file,
                                            lineCount
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                showLoading(false)

                if (batteryInfo != null && batteryInfo.hasData) {
                    val finalBatteryInfo = applyManualDesignCapacity(batteryInfo)
                    displayResults(finalBatteryInfo)
                } else {
                    showError(getString(R.string.error_no_data))
                }

            } catch (e: Exception) {
                showLoading(false)
                showError(getString(R.string.error_reading_file, e.message ?: "Unknown error"))
            } finally {
                isParsing = false
                currentFilePath = null
            }
        }
    }

    private fun extractAndParseZipFromFile(file: File): BatteryInfo? {
        var extractedFile: File? = null
        try {
            return file.inputStream().use { inputStream ->
                val zipInputStream = java.util.zip.ZipInputStream(inputStream)
                var largestEntryName: String? = null
                var largestSize = 0L
                
                // First pass: find the largest txt/log file
                var entry = zipInputStream.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory && !entry.name.contains("/") && 
                        (entry.name.endsWith(".txt", ignoreCase = true) || 
                         entry.name.endsWith(".log", ignoreCase = true))) {
                        
                        if (entry.size > largestSize) {
                            largestSize = entry.size
                            largestEntryName = entry.name
                        }
                    }
                    zipInputStream.closeEntry()
                    entry = zipInputStream.nextEntry
                }
                
                largestEntryName ?: return@use null
                
                // Second pass: extract the largest file to cache
                file.inputStream().use { secondInputStream ->
                    val secondZipStream = java.util.zip.ZipInputStream(secondInputStream)
                    var secondEntry = secondZipStream.nextEntry
                    
                    while (secondEntry != null) {
                        if (secondEntry.name == largestEntryName) {
                            // Extract to cache directory
                            extractedFile = File(cacheDir, "temp_bugreport.txt")
                            extractedFile?.outputStream()?.use { output ->
                                secondZipStream.copyTo(output)
                            }
                            
                            runOnUiThread {
                                binding.tvStatus.text = "Extracted: $largestEntryName"
                            }
                            
                            // Parse the extracted file
                            val result = extractedFile?.inputStream()?.use { stream ->
                                parser.parse(stream) { lineCount ->
                                    if (lineCount > 0) {
                                        runOnUiThread {
                                            binding.tvStatus.text = getString(
                                                R.string.parsing_file,
                                                lineCount
                                            )
                                        }
                                    }
                                }
                            }
                            
                            secondZipStream.closeEntry()
                            return@use result
                        }
                        secondZipStream.closeEntry()
                        secondEntry = secondZipStream.nextEntry
                    }
                    null
                }
            }
        } finally {
            // Clean up extracted file
            extractedFile?.delete()
        }
    }

    private fun displayResults(batteryInfo: BatteryInfo) {
        currentBatteryInfo = batteryInfo
        currentError = null
        binding.cardResults.visibility = View.VISIBLE
        
        // Save to history
        val saved = historyManager.saveResult(batteryInfo)
        if (saved) {
            android.widget.Toast.makeText(
                this,
                getString(R.string.history_saved),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        // Battery Health Percentage
        val healthPercentage = batteryInfo.healthPercentage
        if (healthPercentage != null) {
            val df = DecimalFormat("#.#")
            binding.tvHealthPercentage.text = "${df.format(healthPercentage)}%"
            binding.tvHealthPercentage.setTextColor(getHealthColor(healthPercentage))
        } else {
            binding.tvHealthPercentage.text = "N/A"
            binding.tvHealthPercentage.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }

        // Current Capacity
        if (batteryInfo.currentCapacityMah != null) {
            binding.tvCurrentCapacity.text = getString(
                R.string.format_mah,
                batteryInfo.currentCapacityMah
            )
        } else {
            binding.tvCurrentCapacity.text = getString(R.string.not_available)
        }

        // Design Capacity
        if (batteryInfo.designCapacityMah != null) {
            val hasReliableDesign = batteryInfo.healthPercentage != null
            if (hasReliableDesign) {
                binding.tvDesignCapacity.text = getString(
                    R.string.format_mah,
                    batteryInfo.designCapacityMah
                )
            } else {
                // Show estimated value with note
                binding.tvDesignCapacity.text = getString(
                    R.string.format_mah,
                    batteryInfo.designCapacityMah
                ) + " (approx)"
            }
        } else {
            binding.tvDesignCapacity.text = getString(R.string.not_available)
        }

        // Capacity Loss
        if (batteryInfo.capacityLossMah != null) {
            binding.tvCapacityLoss.text = getString(
                R.string.format_mah,
                batteryInfo.capacityLossMah
            )
        } else {
            binding.tvCapacityLoss.text = getString(R.string.not_available)
        }

        // Cycle Count
        if (batteryInfo.cycleCount != null) {
            binding.tvCycleCount.text = getString(
                R.string.format_cycles,
                batteryInfo.cycleCount
            )
        } else {
            binding.tvCycleCount.text = getString(R.string.not_available)
        }

        // First Use Date
        if (batteryInfo.firstUseDate != null) {
            binding.tvFirstUseDate.text = batteryInfo.firstUseDate
        } else {
            binding.tvFirstUseDate.text = getString(R.string.not_available)
        }

        // Logfile Timestamp
        if (batteryInfo.logfileTimestamp != null) {
            binding.tvLogfileTimestamp.text = batteryInfo.logfileTimestamp
        } else {
            binding.tvLogfileTimestamp.text = getString(R.string.not_available)
        }

        // Health Status
        val statusText = getHealthStatusText(healthPercentage)
        binding.tvHealthStatus.text = statusText
        if (healthPercentage != null) {
            binding.tvHealthStatus.setBackgroundColor(
                getHealthBackgroundColor(healthPercentage)
            )
        } else {
            // Show gray background for unreliable health
            binding.tvHealthStatus.setBackgroundColor(
                ContextCompat.getColor(this, android.R.color.darker_gray)
            )
        }
        
        // Device Model
        if (batteryInfo.deviceModel != null) {
            binding.deviceModelLabel.visibility = View.VISIBLE
            binding.deviceModelText.visibility = View.VISIBLE
            binding.deviceModelText.text = batteryInfo.deviceModel
        } else {
            binding.deviceModelLabel.visibility = View.GONE
            binding.deviceModelText.visibility = View.GONE
        }
        
        // State of Charge (SoC)
        if (batteryInfo.stateOfCharge != null) {
            binding.stateOfChargeLabel.visibility = View.VISIBLE
            binding.stateOfChargeText.visibility = View.VISIBLE
            binding.stateOfChargeText.text = "${batteryInfo.stateOfCharge}%"
        } else {
            binding.stateOfChargeLabel.visibility = View.GONE
            binding.stateOfChargeText.visibility = View.GONE
        }
    }

    private fun getHealthColor(healthPercentage: Double): Int {
        return when {
            healthPercentage >= 95 -> getColor(R.color.health_excellent)
            healthPercentage >= 85 -> getColor(R.color.health_good)
            healthPercentage >= 75 -> getColor(R.color.health_fair)
            healthPercentage >= 65 -> getColor(R.color.health_poor)
            else -> getColor(R.color.health_critical)
        }
    }

    private fun getHealthBackgroundColor(healthPercentage: Double?): Int {
        val isDarkMode = (resources.configuration.uiMode and 
                         android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                         android.content.res.Configuration.UI_MODE_NIGHT_YES
        
        return when {
            healthPercentage == null -> if (isDarkMode) 0xFF6D6D00.toInt() else 0xFFFFF176.toInt()
            healthPercentage >= 95 -> if (isDarkMode) 0xFF1B5E20.toInt() else 0xFF81C784.toInt() // Darker green for dark mode, vibrant for light
            healthPercentage >= 85 -> if (isDarkMode) 0xFF558B2F.toInt() else 0xFFAED581.toInt() // Dark lime / light lime
            healthPercentage >= 75 -> if (isDarkMode) 0xFF9E6F00.toInt() else 0xFFFFD54F.toInt() // Dark amber / amber
            healthPercentage >= 65 -> if (isDarkMode) 0xFFE65100.toInt() else 0xFFFFB74D.toInt() // Dark orange / orange
            else -> if (isDarkMode) 0xFFB71C1C.toInt() else 0xFFEF5350.toInt() // Dark red / red
        }
    }

    private fun getHealthStatusText(healthPercentage: Double?): String {
        return when {
            healthPercentage == null -> "Battery Health: Unknown"
            healthPercentage >= 95 -> getString(R.string.health_excellent)
            healthPercentage >= 85 -> getString(R.string.health_good)
            healthPercentage >= 75 -> getString(R.string.health_fair)
            healthPercentage >= 65 -> getString(R.string.health_poor)
            else -> getString(R.string.health_replace)
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.progressBar.isIndeterminate = true
        binding.tvStatus.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnScanFiles.isEnabled = !show
        binding.btnSelectFile.isEnabled = !show
        
        if (show) {
            binding.tvStatus.text = getString(R.string.parsing_file, 0)
        }
    }

    private fun hideResults() {
        binding.cardResults.visibility = View.GONE
    }

    private fun hideFileList() {
        binding.tvFileListTitle.visibility = View.GONE
        binding.rvFiles.visibility = View.GONE
        binding.tvNoFiles.visibility = View.GONE
    }

    private fun showError(message: String) {
        currentError = message
        currentBatteryInfo = null
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.tvError.visibility = View.GONE
    }
    
    private fun showHelpDialog() {
        val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
        
        val message = buildString {
            append(getString(R.string.help_samsung_title))
            append("\n\n")
            append(getString(R.string.help_samsung_instructions))
            append("\n\n\n")
            append(getString(R.string.help_sony_title))
            append("\n\n")
            append(getString(R.string.help_sony_instructions))
        }
        
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.help_title))
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
    
    private fun showDesignCapacityDialog(batteryInfo: BatteryInfo) {
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        
        // Try to get suggested capacity from PowerProfile
        val suggestedCapacity = getDesignCapacityFromPowerProfile()
        if (suggestedCapacity > 0) {
            input.hint = "Suggested: $suggestedCapacity"
            input.setText(suggestedCapacity.toString())
        } else {
            input.hint = "e.g., 5000"
        }
        
        val message = buildString {
            append("Battery health cannot be calculated because design capacity is not available in the log.\n\n")
            if (batteryInfo.deviceModel != null) {
                append("Device: ${batteryInfo.deviceModel}\n")
            }
            append("Current capacity: ${batteryInfo.currentCapacityMah} mAh\n")
            if (suggestedCapacity > 0) {
                append("Suggested design capacity: $suggestedCapacity mAh (from system)\n")
            }
            append("\nPlease enter the design capacity in mAh:")
        }
        
        AlertDialog.Builder(this)
            .setTitle("Design Capacity Required")
            .setMessage(message)
            .setView(input)
            .setPositiveButton("Calculate") { _, _ ->
                val designCapacity = input.text.toString().toIntOrNull()
                if (designCapacity != null && designCapacity in 2000..15000) {
                    // Save to SharedPreferences so it appears in settings
                    getSharedPreferences("BatteryAnalyzer", MODE_PRIVATE)
                        .edit()
                        .putInt("design_capacity", designCapacity)
                        .apply()
                    recalculateHealthWithDesignCapacity(batteryInfo, designCapacity)
                } else {
                    android.widget.Toast.makeText(
                        this,
                        "Invalid capacity. Please enter a value between 2000-15000 mAh",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Look Up Online") { _, _ ->
                // Open browser to search for device battery specs
                val searchQuery = if (batteryInfo.deviceModel != null) {
                    "${batteryInfo.deviceModel} battery capacity mAh"
                } else {
                    "battery capacity mAh"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(searchQuery)}"))
                try {
                    startActivity(intent)
                    // Show dialog again after they return
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        showDesignCapacityDialog(batteryInfo)
                    }, 1000)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(this, "Cannot open browser", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }
    
    private fun showSettingsDialog() {
        val prefs = getSharedPreferences("BatteryAnalyzer", MODE_PRIVATE)
        val savedCapacity = prefs.getInt("design_capacity", -1)
        
        val input = android.widget.EditText(this)
        input.inputType = android.text.InputType.TYPE_CLASS_NUMBER
        input.hint = "e.g., 5000"
        if (savedCapacity > 0) {
            input.setText(savedCapacity.toString())
        }
        
        val linearLayout = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 20, 60, 20)
        }
        
        val description = android.widget.TextView(this).apply {
            text = getString(R.string.settings_design_capacity_description)
            textSize = 14f
            setPadding(0, 0, 0, 30)
        }
        
        linearLayout.addView(description)
        linearLayout.addView(input)
        
        val dialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.settings_title))
            .setView(linearLayout)
            .setPositiveButton(getString(R.string.settings_save)) { _, _ ->
                val capacity = input.text.toString().toIntOrNull()
                if (capacity != null && capacity in 2000..15000) {
                    prefs.edit().putInt("design_capacity", capacity).apply()
                    android.widget.Toast.makeText(this, getString(R.string.settings_saved), android.widget.Toast.LENGTH_SHORT).show()
                } else if (input.text.toString().isEmpty()) {
                    prefs.edit().remove("design_capacity").apply()
                    android.widget.Toast.makeText(this, getString(R.string.settings_cleared), android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(
                        this,
                        "Invalid capacity. Please enter a value between 2000-15000 mAh",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton(getString(R.string.settings_cancel), null)
            .setNeutralButton(getString(R.string.settings_clear)) { _, _ ->
                prefs.edit().remove("design_capacity").apply()
                android.widget.Toast.makeText(this, getString(R.string.settings_cleared), android.widget.Toast.LENGTH_SHORT).show()
            }
            .create()
        
        dialog.show()
        dialog.window?.setLayout(
            android.view.WindowManager.LayoutParams.MATCH_PARENT,
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
    }
    
    private fun recalculateHealthWithDesignCapacity(batteryInfo: BatteryInfo, designCapacity: Int) {
        val currentCapacity = batteryInfo.currentCapacityMah ?: return
        val healthPercentage = (currentCapacity.toDouble() / designCapacity.toDouble()) * 100.0
        
        // Create updated BatteryInfo with new values
        val updatedInfo = batteryInfo.copy(
            designCapacityMah = designCapacity,
            healthPercentage = healthPercentage
        )
        
        // Redisplay with updated info
        displayResults(updatedInfo)
    }
    
    private fun applyManualDesignCapacity(batteryInfo: BatteryInfo): BatteryInfo {
        // If design capacity is already in the log, use it
        if (batteryInfo.designCapacityMah != null) {
            return batteryInfo
        }
        
        // Check if user has saved a design capacity in settings
        val savedCapacity = getSharedPreferences("BatteryAnalyzer", MODE_PRIVATE)
            .getInt("design_capacity", -1)
        
        if (batteryInfo.currentCapacityMah != null) {
            if (savedCapacity > 0) {
                // Use saved capacity automatically
                val currentCapacity = batteryInfo.currentCapacityMah
                val healthPercentage = (currentCapacity.toDouble() / savedCapacity.toDouble()) * 100.0
                return batteryInfo.copy(
                    designCapacityMah = savedCapacity,
                    healthPercentage = healthPercentage
                )
            } else {
                // Prompt user to provide design capacity
                showDesignCapacityDialog(batteryInfo)
            }
        }
        
        return batteryInfo
    }
    
    private fun getDesignCapacityFromPowerProfile(): Int {
        try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val constructor = powerProfileClass.getConstructor(android.content.Context::class.java)
            val powerProfile = constructor.newInstance(applicationContext)
            
            // Try getBatteryCapacity() method
            val method = powerProfileClass.getMethod("getBatteryCapacity")
            val capacity = method.invoke(powerProfile) as? Double
            
            return capacity?.toInt() ?: 0
        } catch (e: Exception) {
            android.util.Log.e("BatteryAnalyzer", "Failed to get capacity from PowerProfile: ${e.message}")
            return 0
        }
    }
}
