package com.fuseforge.cardash.services.obd

import android.util.Log
import com.fuseforge.cardash.data.db.TripDataPoint
import com.fuseforge.cardash.data.db.OBDDataType
import com.fuseforge.cardash.data.db.OBDLogDao
import com.fuseforge.cardash.data.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Date

private const val TAG = "PollingEngine"

/**
 * High-performance sequential polling engine for OBD-II data collection.
 * Optimized for ELM327 hardware to prevent command collisions and adapter lockups.
 */
class PollingEngine(
    private val obdService: OBDService,
    private val obdServiceWithDiagnostics: OBDServiceWithDiagnostics,
    private val sensorCollector: com.fuseforge.cardash.services.sensors.SensorCollector,
    private val preferences: PreferencesManager,
    private val obdLogDao: OBDLogDao,
    private val externalScope: CoroutineScope
) {
    private var pollingJob: Job? = null
    
    private val _dataFlow = MutableSharedFlow<TripDataPoint>(replay = 1)
    val dataFlow: SharedFlow<TripDataPoint> = _dataFlow.asSharedFlow()
    
    private val _isPolling = MutableStateFlow(false)
    val isPolling: StateFlow<Boolean> = _isPolling.asStateFlow()

    // Current metrics state (held locally)
    private var currentRpm = 0
    private var currentSpeed = 0
    private var currentEngineLoad = 0
    private var currentCoolantTemp = 0
    private var currentFuelLevel = 0
    private var currentIntakeAirTemp = 0
    private var currentThrottlePosition = 0
    private var currentFuelPressure = 0
    private var currentBaroPressure = 0
    private var currentBatteryVoltage = 0.0f
    private var isEngineRunning = false
    private var cycleCount = 0
    private var lastStorageTime = 0L

    fun start() {
        if (_isPolling.value) return
        
        _isPolling.value = true
        sensorCollector.start()
        pollingJob = externalScope.launch {
            Log.d(TAG, "Polling engine started with sensors")
            while (isActive) {
                if (obdService.connectionStatus.value != ConnectionStatus.CONNECTED) {
                    delay(1000)
                    continue
                }

                try {
                    val startTime = System.currentTimeMillis()
                    
                    // 1. Poll ALWAYS (Tier 1) - High frequency
                    pollRPM()
                    pollEngineLoad()
                    pollThrottlePosition()
                    
                    // Simplified Engine Running Logic: Only use Engine Load
                    isEngineRunning = currentEngineLoad > 0

                    if (isEngineRunning || cycleCount % 5 == 0) {
                        pollSpeed()
                    }

                    // 2. Poll Tier 2 (Every 5 cycles)
                    if (cycleCount % 5 == 0) {
                        pollCoolantTemp()
                        pollFuelPressure()
                        pollBatteryVoltage()
                    }

                    // 3. Poll Tier 3 (Every 10 cycles)
                    if (cycleCount % 10 == 0) {
                        pollIntakeAirTemp()
                    }

                    // 4. Poll Tier 4 (Every 20 cycles)
                    if (cycleCount % 20 == 0) {
                        pollBaroPressure()
                        pollFuelLevel()
                    }

                    // 5. Gather Sensor Data
                    val location = sensorCollector.lastLocation.value
                    val acceleration = sensorCollector.linearAcceleration.value

                    // Emit latest combined data
                    val dataPoint = TripDataPoint(
                        timestamp = Date(),
                        tripId = obdServiceWithDiagnostics.getSessionId(),
                        
                        // GPS
                        latitude = location?.latitude,
                        longitude = location?.longitude,
                        speedGps = location?.speed?.let { (it * 3.6).toInt() }, // Convert m/s to km/h
                        
                        // OBD
                        rpm = currentRpm,
                        speedObd = currentSpeed,
                        engineLoad = currentEngineLoad,
                        coolantTemp = currentCoolantTemp,
                        fuelLevel = currentFuelLevel,
                        intakeAirTemp = currentIntakeAirTemp,
                        throttlePosition = currentThrottlePosition,
                        fuelPressure = currentFuelPressure,
                        baroPressure = currentBaroPressure,
                        batteryVoltage = currentBatteryVoltage,

                        // IMU
                        gForceX = acceleration.getOrNull(0),
                        gForceY = acceleration.getOrNull(1),
                        gForceZ = acceleration.getOrNull(2)
                    )
                    _dataFlow.emit(dataPoint)

                    // Database Storage Logic
                    val timeSinceLastStorage = System.currentTimeMillis() - lastStorageTime
                    if (shouldWriteToDatabase(dataPoint, timeSinceLastStorage)) {
                        obdLogDao.insertTripDataPoint(dataPoint)
                        lastStorageTime = System.currentTimeMillis()
                    }

                    cycleCount++
                    
                    // Maintain stable cycle time
                    val elapsedTime = System.currentTimeMillis() - startTime
                    val delayTime = (preferences.getDataCollectionFrequency() - elapsedTime).coerceAtLeast(50L)
                    delay(delayTime)

                } catch (e: Exception) {
                    Log.e(TAG, "Error in polling cycle: ${e.message}")
                    delay(1000)
                }
            }
        }
    }

    fun stop() {
        _isPolling.value = false
        sensorCollector.stop()
        pollingJob?.cancel()
        pollingJob = null
        Log.d(TAG, "Polling engine stopped")
    }

    private fun shouldWriteToDatabase(point: TripDataPoint, elapsed: Long): Boolean {
        // Force write every X seconds (from preferences)
        if (elapsed >= preferences.getStorageFrequency()) return true
        
        // Dynamic write on significant change (only if engine running)
        if (isEngineRunning) {
            // Simplified threshold checks
            if (point.rpm != null && Math.abs(point.rpm!! - currentRpm) > 200) return true
            if (point.speedObd != null && Math.abs(point.speedObd!! - currentSpeed) > 5) return true
        }
        
        return false
    }

    private suspend fun pollRPM() {
        try {
            val response = obdService.sendCommand("01 0C")
            currentRpm = obdService.parseRPMResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to poll RPM: ${e.message}")
            currentRpm = 0 // Reset on failure (likely engine off/no data)
        }
    }

    private suspend fun pollSpeed() {
        try {
            val response = obdService.sendCommand(OBDService.SPEED_COMMAND)
            currentSpeed = obdService.parseSpeedResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to poll Speed: ${e.message}")
        }
    }

    private suspend fun pollEngineLoad() {
        try {
            val response = obdService.sendCommand(OBDService.ENGINE_LOAD_COMMAND)
            currentEngineLoad = obdService.parseEngineLoadResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to poll Engine Load: ${e.message}")
            currentEngineLoad = 0 // Reset on failure (likely engine off/no data)
        }
    }

    private suspend fun pollCoolantTemp() {
        try {
            val response = obdService.sendCommand(OBDService.COOLANT_TEMP_COMMAND)
            currentCoolantTemp = obdService.parseCoolantTempResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to poll Coolant Temp: ${e.message}")
        }
    }

    private suspend fun pollFuelLevel() {
        try {
            val response = obdService.sendCommand(OBDService.FUEL_LEVEL_COMMAND)
            currentFuelLevel = obdService.parseFuelLevelResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to poll Fuel Level: ${e.message}")
        }
    }

    private suspend fun pollIntakeAirTemp() {
        try {
            val response = obdService.sendCommand(OBDService.INTAKE_AIR_TEMP_COMMAND)
            currentIntakeAirTemp = obdService.parseIntakeAirTempResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to poll Intake Air Temp: ${e.message}")
        }
    }

    private suspend fun pollThrottlePosition() {
        try {
            val response = obdService.sendCommand(OBDService.THROTTLE_POSITION_COMMAND)
            currentThrottlePosition = obdService.parseThrottlePositionResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to poll Throttle Position: ${e.message}")
        }
    }

    private suspend fun pollFuelPressure() {
        try {
            val response = obdService.sendCommand(OBDService.FUEL_PRESSURE_COMMAND)
            currentFuelPressure = obdService.parseFuelPressureResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to poll Fuel Pressure: ${e.message}")
        }
    }

    private suspend fun pollBaroPressure() {
        try {
            val response = obdService.sendCommand(OBDService.BARO_PRESSURE_COMMAND)
            currentBaroPressure = obdService.parseBaroPressureResponse(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to poll Baro Pressure: ${e.message}")
        }
    }

    private suspend fun pollBatteryVoltage() {
        try {
            currentBatteryVoltage = obdService.getBatteryVoltage()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to poll Battery Voltage: ${e.message}")
        }
    }
}
