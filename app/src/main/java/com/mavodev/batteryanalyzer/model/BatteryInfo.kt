package com.mavodev.batteryanalyzer.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class BatteryInfo(
    val cycleCount: Int? = null,
    val currentCapacityMah: Int? = null,
    val designCapacityMah: Int? = null,
    val fullChargeCapacityMah: Int? = null,
    val healthPercentage: Double? = null,
    val calculatedHealthPercentage: Double? = null,
    val firstUseDate: String? = null,
    val stateOfCharge: Int? = null,
    val logfileTimestamp: String? = null,
    val deviceModel: String? = null,
    val batteryLevelChanges: List<BatteryLevelChange> = emptyList(),
    val parseErrors: List<String> = emptyList()
) : Parcelable {
    val isDegraded: Boolean
        get() = healthPercentage?.let { it < 80.0 } ?: false
    
    val degradationPercentage: Double?
        get() = healthPercentage?.let { 100.0 - it }
    
    val capacityLossMah: Int?
        get() {
            return if (designCapacityMah != null && currentCapacityMah != null) {
                designCapacityMah - currentCapacityMah
            } else null
        }
    
    val hasData: Boolean
        get() = cycleCount != null || currentCapacityMah != null || 
                designCapacityMah != null || fullChargeCapacityMah != null
}

@Parcelize
data class BatteryLevelChange(
    val timestamp: String,
    val level: Int,
    val action: String
) : Parcelable
