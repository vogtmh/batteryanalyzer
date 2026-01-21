package com.example.batteryanalyzer.parser

import com.example.batteryanalyzer.model.BatteryInfo
import com.example.batteryanalyzer.model.BatteryLevelChange
import java.io.BufferedReader
import java.io.InputStream
import java.util.regex.Pattern

class DumpstateParser {
    
    companion object {
        // Samsung patterns
        private val CYCLE_COUNT_PATTERN = Pattern.compile("Cycle\\((\\d+),\\s*(\\d+)\\)")
        private val CAP_NOM_PATTERN = Pattern.compile("CAP_NOM\\s+(\\d+)mAh")
        private val FULL_CHARGE_PATTERN = Pattern.compile("batteryFullChargeUah:\\s+(\\d+)")
        private val DESIGN_CAP_UAH_PATTERN = Pattern.compile("batteryFullChargeDesignCapacityUah:\\s+(\\d+)")
        private val FIRST_USE_DATE_PATTERN = Pattern.compile("FirstUseDate.*?\\[(\\d{8})\\]")
        private val SOC_PATTERN = Pattern.compile("SoC:(\\d+)\\(%\\)")
        
        // Sony/Generic patterns
        private val SONY_CYCLE_COUNT_PATTERN = Pattern.compile("android\\.os\\.extra\\.CYCLE_COUNT=(\\d+)")
        private val SONY_CHARGE_COUNTER_PATTERN = Pattern.compile("charge_counter=(\\d+)")
        private val HEALTHD_BATTERY_PATTERN = Pattern.compile("healthd: battery l=(\\d+).*?fc=(\\d+).*?cc=(\\d+)")
        private val CHARGE_COUNTER_SERVICE_PATTERN = Pattern.compile("Charge counter:\\s+(\\d+)")
        private val BATTERY_LEVEL_PATTERN = Pattern.compile("^\\s+level:\\s+(\\d+)")
        private val ESTIMATED_CAPACITY_PATTERN = Pattern.compile("Estimated battery capacity:\\s+(\\d+)\\s*mAh")
        private val LEARNED_CAPACITY_PATTERN = Pattern.compile("Last learned battery capacity:\\s+(\\d+)\\s*mAh")
        private val DUMPSTATE_TIMESTAMP_PATTERN = Pattern.compile("== dumpstate: (\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2})")
        private val DEVICE_BRAND_PATTERN = Pattern.compile("\\[ro\\.product\\.brand\\]:\\s*\\[(.+?)\\]")
        private val DEVICE_MODEL_PATTERN = Pattern.compile("\\[ro\\.product\\.model\\]:\\s*\\[(.+?)\\]")
        
        private val BATTERY_CHANGED_PATTERN = Pattern.compile(
            "ACTION_BATTERY_CHANGED.*?level:(\\d+)"
        )
        private val TIMESTAMP_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})")
    }
    
    fun parse(inputStream: InputStream, progressCallback: ((Int) -> Unit)? = null): BatteryInfo {
        var cycleCount: Int? = null
        var currentCapacity: Int? = null
        var fullChargeCapacity: Int? = null
        var designCapacity: Int? = null
        var firstUseDate: String? = null
        var stateOfCharge: Int? = null
        var logfileTimestamp: String? = null
        var deviceBrand: String? = null
        var deviceModel: String? = null
        var hasReliableCapacity = false // Track if we have Samsung CAP_NOM
        val batteryChanges = mutableListOf<BatteryLevelChange>()
        val errors = mutableListOf<String>()
        
        try {
            val reader = BufferedReader(inputStream.reader())
            var lineCount = 0
            var lastTimestamp = ""
            
            reader.useLines { lines ->
                lines.forEach { line ->
                    lineCount++
                    
                    // Report progress every 10000 lines
                    if (lineCount % 10000 == 0) {
                        progressCallback?.invoke(lineCount)
                    }
                    
                    // Extract timestamp if present
                    val timestampMatcher = TIMESTAMP_PATTERN.matcher(line)
                    if (timestampMatcher.find()) {
                        lastTimestamp = timestampMatcher.group(1) ?: ""
                    }
                    
                    // Extract logfile timestamp from dumpstate header (only first occurrence)
                    if (logfileTimestamp == null) {
                        val dumpstateMatcher = DUMPSTATE_TIMESTAMP_PATTERN.matcher(line)
                        if (dumpstateMatcher.find()) {
                            logfileTimestamp = formatLogfileTimestamp(dumpstateMatcher.group(1))
                        }
                    }
                    
                    // Parse cycle count (Samsung)
                    if (cycleCount == null) {
                        val cycleMatcher = CYCLE_COUNT_PATTERN.matcher(line)
                        if (cycleMatcher.find()) {
                            cycleCount = cycleMatcher.group(1)?.toIntOrNull()
                        }
                    }
                    
                    // Parse cycle count (Sony/Generic)
                    if (cycleCount == null) {
                        val sonyCycleMatcher = SONY_CYCLE_COUNT_PATTERN.matcher(line)
                        if (sonyCycleMatcher.find()) {
                            cycleCount = sonyCycleMatcher.group(1)?.toIntOrNull()
                        }
                    }
                    
                    // Parse CAP_NOM (current capacity - Samsung)
                    if (currentCapacity == null) {
                        val capNomMatcher = CAP_NOM_PATTERN.matcher(line)
                        if (capNomMatcher.find()) {
                            currentCapacity = capNomMatcher.group(1)?.toIntOrNull()
                            hasReliableCapacity = true // Mark as reliable
                        }
                    }
                    
                    // Parse charge_counter (Sony/Generic)
                    if (currentCapacity == null) {
                        val sonyChargeMatcher = SONY_CHARGE_COUNTER_PATTERN.matcher(line)
                        if (sonyChargeMatcher.find()) {
                            val chargeCounterUah = sonyChargeMatcher.group(1)?.toLongOrNull()
                            if (chargeCounterUah != null) {
                                currentCapacity = (chargeCounterUah / 1000).toInt()
                            }
                        }
                    }
                    
                    // Parse Charge counter from DUMP OF SERVICE battery
                    if (currentCapacity == null) {
                        val serviceChargeMatcher = CHARGE_COUNTER_SERVICE_PATTERN.matcher(line)
                        if (serviceChargeMatcher.find()) {
                            val chargeCounterUah = serviceChargeMatcher.group(1)?.toLongOrNull()
                            if (chargeCounterUah != null) {
                                currentCapacity = (chargeCounterUah / 1000).toInt()
                            }
                        }
                    }
                    
                    // Parse healthd battery line (has both fc and cc)
                    if (currentCapacity == null || cycleCount == null) {
                        val healthdMatcher = HEALTHD_BATTERY_PATTERN.matcher(line)
                        if (healthdMatcher.find()) {
                            if (currentCapacity == null) {
                                val fcUah = healthdMatcher.group(2)?.toLongOrNull()
                                if (fcUah != null) {
                                    currentCapacity = (fcUah / 1000).toInt()
                                }
                            }
                            if (cycleCount == null) {
                                cycleCount = healthdMatcher.group(3)?.toIntOrNull()
                            }
                        }
                    }
                    
                    // Parse full charge capacity
                    if (fullChargeCapacity == null) {
                        val fullChargeMatcher = FULL_CHARGE_PATTERN.matcher(line)
                        if (fullChargeMatcher.find()) {
                            val uAh = fullChargeMatcher.group(1)?.toLongOrNull()
                            fullChargeCapacity = uAh?.let { (it / 1000).toInt() }
                        }
                    }
                    
                    // Parse design capacity from batteryFullChargeDesignCapacityUah
                    if (designCapacity == null) {
                        val designUahMatcher = DESIGN_CAP_UAH_PATTERN.matcher(line)
                        if (designUahMatcher.find()) {
                            val uAh = designUahMatcher.group(1)?.toLongOrNull()
                            designCapacity = uAh?.let { (it / 1000).toInt() }
                        }
                    }
                    
                    // Parse estimated battery capacity from battery stats
                    if (designCapacity == null) {
                        val estimatedMatcher = ESTIMATED_CAPACITY_PATTERN.matcher(line)
                        if (estimatedMatcher.find()) {
                            designCapacity = estimatedMatcher.group(1)?.toIntOrNull()
                        }
                    }
                    
                    // Parse learned battery capacity from battery stats
                    // This represents the full charge capacity (what battery can hold when fully charged)
                    // Only use if we don't have a reliable Samsung CAP_NOM value
                    if (!hasReliableCapacity) {
                        val learnedMatcher = LEARNED_CAPACITY_PATTERN.matcher(line)
                        if (learnedMatcher.find()) {
                            val learned = learnedMatcher.group(1)?.toIntOrNull()
                            if (learned != null) {
                                fullChargeCapacity = learned // This is the full charge capacity
                                currentCapacity = learned // Also use as current capacity for health calc
                            }
                        }
                    }
                    
                    // Parse first use date
                    if (firstUseDate == null) {
                        val firstUseMatcher = FIRST_USE_DATE_PATTERN.matcher(line)
                        if (firstUseMatcher.find()) {
                            val dateStr = firstUseMatcher.group(1)
                            firstUseDate = formatFirstUseDate(dateStr)
                        }
                    }
                    
                    // Parse State of Charge (SoC) - Samsung
                    if (stateOfCharge == null) {
                        val socMatcher = SOC_PATTERN.matcher(line)
                        if (socMatcher.find()) {
                            stateOfCharge = socMatcher.group(1)?.toIntOrNull()
                        }
                    }
                    
                    // Parse battery level (Sony/Generic) from "  level: 99"
                    if (stateOfCharge == null) {
                        val levelMatcher = BATTERY_LEVEL_PATTERN.matcher(line)
                        if (levelMatcher.find()) {
                            stateOfCharge = levelMatcher.group(1)?.toIntOrNull()
                        }
                    }
                    
                    // Parse device brand
                    if (deviceBrand == null) {
                        val brandMatcher = DEVICE_BRAND_PATTERN.matcher(line)
                        if (brandMatcher.find()) {
                            deviceBrand = brandMatcher.group(1)?.replaceFirstChar { it.uppercase() }
                        }
                    }
                    
                    // Parse device model
                    if (deviceModel == null) {
                        val modelMatcher = DEVICE_MODEL_PATTERN.matcher(line)
                        if (modelMatcher.find()) {
                            deviceModel = modelMatcher.group(1)
                        }
                    }
                    
                    // Parse battery level changes
                    if (batteryChanges.size < 50) { // Limit to most recent 50 changes
                        val batteryMatcher = BATTERY_CHANGED_PATTERN.matcher(line)
                        if (batteryMatcher.find()) {
                            val level = batteryMatcher.group(1)?.toIntOrNull()
                            if (level != null && lastTimestamp.isNotEmpty()) {
                                batteryChanges.add(
                                    BatteryLevelChange(
                                        timestamp = lastTimestamp,
                                        level = level,
                                        action = "BATTERY_CHANGED"
                                    )
                                )
                            }
                        }
                    }
                }
            }
            
            progressCallback?.invoke(-1) // Signal completion
            
        } catch (e: Exception) {
            errors.add("Error parsing file: ${e.message}")
        }
        
        // Calculate health percentage
        val designCapacityData = getReasonableDesignCapacity(designCapacity, currentCapacity, fullChargeCapacity)
        val actualDesignCapacity = designCapacityData.first
        val isDesignCapacityReliable = designCapacityData.second
        val healthPercentage = if (isDesignCapacityReliable) {
            calculateHealthPercentage(
                currentCapacity, 
                fullChargeCapacity, 
                actualDesignCapacity
            )
        } else {
            null // Don't show health if design capacity is estimated
        }
        
        // Combine brand and model
        val deviceName = when {
            deviceBrand != null && deviceModel != null -> "$deviceBrand $deviceModel"
            deviceModel != null -> deviceModel
            else -> null
        }
        
        return BatteryInfo(
            cycleCount = cycleCount,
            currentCapacityMah = currentCapacity ?: fullChargeCapacity,
            designCapacityMah = actualDesignCapacity,
            fullChargeCapacityMah = fullChargeCapacity,
            healthPercentage = healthPercentage,
            firstUseDate = firstUseDate,
            stateOfCharge = stateOfCharge,
            logfileTimestamp = logfileTimestamp,
            deviceModel = deviceName,
            batteryLevelChanges = batteryChanges.takeLast(20), // Most recent 20
            parseErrors = errors
        )
    }
    
    private fun formatFirstUseDate(dateStr: String?): String? {
        if (dateStr == null || dateStr.length != 8) return null
        try {
            val year = dateStr.substring(0, 4)
            val month = dateStr.substring(4, 6).toInt()
            val day = dateStr.substring(6, 8).toInt()
            
            val monthName = when (month) {
                1 -> "January"
                2 -> "February"
                3 -> "March"
                4 -> "April"
                5 -> "May"
                6 -> "June"
                7 -> "July"
                8 -> "August"
                9 -> "September"
                10 -> "October"
                11 -> "November"
                12 -> "December"
                else -> return null
            }
            
            val daySuffix = when {
                day in 11..13 -> "th"
                day % 10 == 1 -> "st"
                day % 10 == 2 -> "nd"
                day % 10 == 3 -> "rd"
                else -> "th"
            }
            
            return "$monthName $day$daySuffix, $year"
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun formatLogfileTimestamp(timestamp: String?): String? {
        if (timestamp == null) return null
        try {
            // Input: "2026-01-21 16:06:52"
            // Output: "January 21st, 2026 at 4:06 PM"
            val parts = timestamp.split(" ")
            if (parts.size != 2) return timestamp
            
            val dateParts = parts[0].split("-")
            val timeParts = parts[1].split(":")
            
            if (dateParts.size != 3 || timeParts.size != 3) return timestamp
            
            val year = dateParts[0]
            val month = dateParts[1].toInt()
            val day = dateParts[2].toInt()
            val hour = timeParts[0].toInt()
            val minute = timeParts[1]
            
            val monthName = when (month) {
                1 -> "January"
                2 -> "February"
                3 -> "March"
                4 -> "April"
                5 -> "May"
                6 -> "June"
                7 -> "July"
                8 -> "August"
                9 -> "September"
                10 -> "October"
                11 -> "November"
                12 -> "December"
                else -> return timestamp
            }
            
            val daySuffix = when {
                day in 11..13 -> "th"
                day % 10 == 1 -> "st"
                day % 10 == 2 -> "nd"
                day % 10 == 3 -> "rd"
                else -> "th"
            }
            
            val amPm = if (hour >= 12) "PM" else "AM"
            val hour12 = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            
            return "$monthName $day$daySuffix, $year at $hour12:$minute $amPm"
        } catch (e: Exception) {
            return timestamp
        }
    }
    
    private fun calculateHealthPercentage(
        currentCapacity: Int?,
        fullChargeCapacity: Int?,
        designCapacity: Int?
    ): Double? {
        val actualCapacity = currentCapacity ?: fullChargeCapacity ?: return null
        val design = designCapacity ?: return null
        return (actualCapacity.toDouble() / design.toDouble()) * 100.0
    }
    
    private fun getReasonableDesignCapacity(
        parsedDesignCapacity: Int?,
        currentCapacity: Int?,
        fullChargeCapacity: Int?
    ): Pair<Int?, Boolean> {
        // If we have a parsed value and it's reasonable (2000-15000 mAh for phones/tablets), use it
        parsedDesignCapacity?.let {
            if (it in 2000..15000) {
                return Pair(it, true) // Reliable value from device
            }
        }
        
        // If design capacity is invalid but we have fullChargeCapacity from healthd, use that
        // (it's the best available estimate for current battery capacity)
        fullChargeCapacity?.let {
            if (it in 2000..15000) {
                return Pair(it, false) // Use full charge capacity as approximation
            }
        }
        
        // Last resort: use current capacity
        val actualCapacity = currentCapacity
        actualCapacity?.let {
            if (it in 2000..15000) {
                return Pair(it, false) // Use current capacity as approximation
            }
        }
        
        return Pair(null, false)
    }
}
