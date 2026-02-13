package com.mavodev.batteryanalyzer.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class HistoryEntry(
    val id: String, // Unique identifier based on timestamp
    val timestamp: Long, // When this result was analyzed
    val logfileTimestamp: String?, // When the logfile was created
    val deviceModel: String?,
    val healthPercentage: Double?,
    val currentCapacityMah: Int?,
    val designCapacityMah: Int?,
    val cycleCount: Int?,
    val stateOfCharge: Int?,
    val firstUseDate: String?,
    val logfileTimestampLong: Long? = null,
    val ratedCapacityMah: Int? = null
) : Parcelable {
    
    val eventTimestamp: Long
        get() = timestamp
    
    val calculatedHealthPercentage: Double?
        get() {
            val baseCapacity = ratedCapacityMah ?: designCapacityMah
            return if (currentCapacityMah != null && baseCapacity != null && baseCapacity > 0) {
                (currentCapacityMah.toDouble() / baseCapacity.toDouble()) * 100.0
            } else null
        }
    
    companion object {
        fun fromBatteryInfo(batteryInfo: BatteryInfo): HistoryEntry {
            // Priority: Log date (if found in file), otherwise analysis time
            val ts = batteryInfo.logfileTimestampLong ?: System.currentTimeMillis()
            return HistoryEntry(
                id = ts.toString(),
                timestamp = ts,
                logfileTimestamp = batteryInfo.logfileTimestamp,
                deviceModel = batteryInfo.deviceModel,
                healthPercentage = batteryInfo.healthPercentage,
                currentCapacityMah = batteryInfo.currentCapacityMah,
                designCapacityMah = batteryInfo.designCapacityMah,
                cycleCount = batteryInfo.cycleCount,
                stateOfCharge = batteryInfo.stateOfCharge,
                firstUseDate = batteryInfo.firstUseDate,
                logfileTimestampLong = batteryInfo.logfileTimestampLong,
                ratedCapacityMah = batteryInfo.ratedCapacityMah
            )
        }
        
        fun fromJson(json: JSONObject): HistoryEntry {
            return HistoryEntry(
                id = json.getString("id"),
                timestamp = json.getLong("timestamp"),
                logfileTimestamp = json.optString("logfileTimestamp").takeIf { it.isNotEmpty() },
                deviceModel = json.optString("deviceModel").takeIf { it.isNotEmpty() },
                healthPercentage = if (json.has("healthPercentage") && !json.isNull("healthPercentage")) 
                    json.getDouble("healthPercentage") else null,
                currentCapacityMah = if (json.has("currentCapacityMah") && !json.isNull("currentCapacityMah")) 
                    json.getInt("currentCapacityMah") else null,
                designCapacityMah = if (json.has("designCapacityMah") && !json.isNull("designCapacityMah")) 
                    json.getInt("designCapacityMah") else null,
                cycleCount = if (json.has("cycleCount") && !json.isNull("cycleCount")) 
                    json.getInt("cycleCount") else null,
                stateOfCharge = if (json.has("stateOfCharge") && !json.isNull("stateOfCharge")) 
                    json.getInt("stateOfCharge") else null,
                firstUseDate = json.optString("firstUseDate").takeIf { it.isNotEmpty() },
                logfileTimestampLong = if (json.has("logfileTimestampLong") && !json.isNull("logfileTimestampLong"))
                    json.getLong("logfileTimestampLong") else null,
                ratedCapacityMah = if (json.has("ratedCapacityMah") && !json.isNull("ratedCapacityMah"))
                    json.getInt("ratedCapacityMah") else null
            )
        }
    }
    
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("timestamp", timestamp)
            put("logfileTimestamp", logfileTimestamp ?: JSONObject.NULL)
            put("deviceModel", deviceModel ?: JSONObject.NULL)
            put("healthPercentage", healthPercentage ?: JSONObject.NULL)
            put("currentCapacityMah", currentCapacityMah ?: JSONObject.NULL)
            put("designCapacityMah", designCapacityMah ?: JSONObject.NULL)
            put("cycleCount", cycleCount ?: JSONObject.NULL)
            put("stateOfCharge", stateOfCharge ?: JSONObject.NULL)
            put("firstUseDate", firstUseDate ?: JSONObject.NULL)
            put("logfileTimestampLong", logfileTimestampLong ?: JSONObject.NULL)
            put("ratedCapacityMah", ratedCapacityMah ?: JSONObject.NULL)
        }
    }
    
    // Check if two entries are duplicates (same device and similar data from same logfile)
    fun isDuplicateOf(other: HistoryEntry): Boolean {
        return logfileTimestamp == other.logfileTimestamp &&
               deviceModel == other.deviceModel &&
               currentCapacityMah == other.currentCapacityMah &&
               cycleCount == other.cycleCount
    }
}
