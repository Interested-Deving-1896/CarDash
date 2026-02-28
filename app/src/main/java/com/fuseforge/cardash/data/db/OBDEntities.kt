package com.fuseforge.cardash.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date

@Entity(tableName = "obd_logs")
@TypeConverters(DateConverter::class)
data class OBDLogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Date = Date(),
    val command: String,
    val rawResponse: String,
    val parsedValue: String,
    val dataType: OBDDataType,
    val sessionId: String,
    val isError: Boolean = false,
    val errorMessage: String? = null
)

// Projection class for fuel level readings (for Room query)
data class FuelLevelReading(
    val fuelLevel: Int,
    val timestamp: Date
)

enum class OBDDataType {
    RPM,
    SPEED,
    ENGINE_LOAD,
    COOLANT_TEMP,
    FUEL_LEVEL,
    INTAKE_AIR_TEMP,
    THROTTLE_POSITION,
    FUEL_PRESSURE,
    BARO_PRESSURE,
    BATTERY_VOLTAGE,
    MAF,
    AMBIENT_AIR_TEMP,
    CONNECTION,
    INITIALIZATION,
    UNKNOWN
}

@Entity(tableName = "trips")
@TypeConverters(DateConverter::class)
data class Trip(
    @PrimaryKey
    val tripId: String,
    val vehicleId: String? = null,
    val deviceAddress: String,
    val deviceName: String?,
    val startTime: Date = Date(),
    val endTime: Date? = null,
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val maxSpeed: Int? = null,
    val maxRpm: Int? = null,
    val maxTemp: Int? = null,
    val vehicleInfo: String? = null
)

/**
 * This entity represents a single "row" of OBD data paired with location.
 */
@Entity(tableName = "trip_data_points")
@TypeConverters(DateConverter::class)
data class TripDataPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Date = Date(),
    val tripId: String,
    
    // GPS Context
    val latitude: Double? = null,
    val longitude: Double? = null,

    // Vehicle parameters
    val speedObd: Int? = null,
    val speedGps: Int? = null,
    val rpm: Int? = null,
    val engineLoad: Int? = null,
    val coolantTemp: Int? = null,
    val fuelLevel: Int? = null,
    val intakeAirTemp: Int? = null,
    val throttlePosition: Int? = null,
    val fuelPressure: Int? = null,
    val baroPressure: Int? = null,
    val batteryVoltage: Float? = null,
    val maf: Float? = null,
    val ambientAirTemp: Int? = null,
    
    // IMU Context
    val gForceX: Float? = null,
    val gForceY: Float? = null,
    val gForceZ: Float? = null
)

/**
 * Entity for Diagnostic Trouble Code (DTC) translations.
 * Allows offline fault description lookups.
 */
@Entity(tableName = "diagnostic_codes")
data class DiagnosticCode(
    @PrimaryKey
    val code: String, // e.g. "P0300"
    val description: String, // e.g. "Random or Multiple Cylinder Misfire Detected"
    val possibleCauses: String? = null,
    val severity: Int = 1 // 1: Info, 2: Warning, 3: Critical
)