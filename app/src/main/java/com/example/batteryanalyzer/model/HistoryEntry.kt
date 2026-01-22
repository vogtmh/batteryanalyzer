package com.example.batteryanalyzer.model

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
    val firstUseDate: String?
) : Parcelable {
    
    companion object {
        fun fromBatteryInfo(batteryInfo: BatteryInfo): HistoryEntry {
            val timestamp = System.currentTimeMillis()
            return HistoryEntry(
                id = timestamp.toString(),
                timestamp = timestamp,
                logfileTimestamp = batteryInfo.logfileTimestamp,
                deviceModel = batteryInfo.deviceModel,
                healthPercentage = batteryInfo.healthPercentage,
                currentCapacityMah = batteryInfo.currentCapacityMah,
                designCapacityMah = batteryInfo.designCapacityMah,
                cycleCount = batteryInfo.cycleCount,
                stateOfCharge = batteryInfo.stateOfCharge,
                firstUseDate = batteryInfo.firstUseDate
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
                firstUseDate = json.optString("firstUseDate").takeIf { it.isNotEmpty() }
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
