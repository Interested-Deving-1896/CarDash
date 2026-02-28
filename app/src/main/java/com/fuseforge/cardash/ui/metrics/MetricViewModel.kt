package com.fuseforge.cardash.ui.metrics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.bluetooth.BluetoothDevice
import android.content.Context
import com.fuseforge.cardash.data.preferences.AppPreferences
import com.fuseforge.cardash.services.obd.OBDService
import com.fuseforge.cardash.services.obd.OBDServiceWithDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date
import com.fuseforge.cardash.data.db.AppDatabase

import com.fuseforge.cardash.services.obd.PollingEngine

class MetricViewModel(
    private val obdService: OBDService,
    private val obdServiceWithDiagnostics: OBDServiceWithDiagnostics,
    private val pollingEngine: PollingEngine
) : ViewModel() {

    // Get application context from the OBD service
    private val context = obdServiceWithDiagnostics.getContext()
    
    // Initialize preferences
    private val preferences = (context.applicationContext as com.fuseforge.cardash.CarDashApp).preferencesManager

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState = _connectionState.asStateFlow()

    // Engine state tracking
    private val _engineRunning = MutableStateFlow(false)
    val engineRunning = _engineRunning.asStateFlow()

    private val _rpm = MutableStateFlow(0)
    val rpm = _rpm.asStateFlow()
    
    private val _engineLoad = MutableStateFlow(0)
    val engineLoad = _engineLoad.asStateFlow()

    private val _speed = MutableStateFlow(0)
    val speed = _speed.asStateFlow()

    private val _coolantTemp = MutableStateFlow(0)
    val coolantTemp = _coolantTemp.asStateFlow()

    private val _fuelLevel = MutableStateFlow(0)
    val fuelLevel = _fuelLevel.asStateFlow()

    private val _intakeAirTemp = MutableStateFlow(0)
    val intakeAirTemp = _intakeAirTemp.asStateFlow()

    private val _throttlePosition = MutableStateFlow(0)
    val throttlePosition = _throttlePosition.asStateFlow()

    private val _fuelPressure = MutableStateFlow(0)
    val fuelPressure = _fuelPressure.asStateFlow()

    private val _baroPressure = MutableStateFlow(0)
    val baroPressure = _baroPressure.asStateFlow()

    private val _batteryVoltage = MutableStateFlow(0f)
    val batteryVoltage = _batteryVoltage.asStateFlow()
    
    private val _speedGps = MutableStateFlow(0)
    val speedGps = _speedGps.asStateFlow()

    private val _latitude = MutableStateFlow<Double?>(null)
    val latitude = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow<Double?>(null)
    val longitude = _longitude.asStateFlow()

    private val _gForceX = MutableStateFlow(0f)
    val gForceX = _gForceX.asStateFlow()

    private val _gForceY = MutableStateFlow(0f)
    val gForceY = _gForceY.asStateFlow()

    private val _gForceZ = MutableStateFlow(0f)
    val gForceZ = _gForceZ.asStateFlow()
    
    private val _averageSpeed = MutableStateFlow(0)
    val averageSpeed = _averageSpeed.asStateFlow()
    
    private val _fuelLevelHistory = MutableStateFlow<List<Int>>(emptyList())
    val fuelLevelHistory = _fuelLevelHistory.asStateFlow()
    
    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage = _errorMessage.asSharedFlow()
    
    // Logging preference flow for UI
    private val _verboseLoggingEnabled = MutableStateFlow(preferences.isVerboseLoggingEnabled())
    val verboseLoggingEnabled = _verboseLoggingEnabled.asStateFlow()

    init {
        // Observe connection status from OBDService
        obdService.connectionStatus
            .onEach { status ->
                _connectionState.value = when(status) {
                    com.fuseforge.cardash.services.obd.ConnectionStatus.CONNECTED -> ConnectionState.Connected
                    com.fuseforge.cardash.services.obd.ConnectionStatus.CONNECTING -> ConnectionState.Connecting
                    com.fuseforge.cardash.services.obd.ConnectionStatus.RECONNECTING -> ConnectionState.Connecting
                    com.fuseforge.cardash.services.obd.ConnectionStatus.ERROR -> ConnectionState.Failed("Connection failed")
                    com.fuseforge.cardash.services.obd.ConnectionStatus.DISCONNECTED -> ConnectionState.Disconnected
                }
            }
            .launchIn(viewModelScope)

        // Observe data from PollingEngine
        pollingEngine.dataFlow
            .onEach { point ->
                _rpm.value = point.rpm ?: 0
                _speed.value = point.speedObd ?: 0
                _engineLoad.value = point.engineLoad ?: 0
                _coolantTemp.value = point.coolantTemp ?: 0
                _fuelLevel.value = point.fuelLevel ?: 0
                _intakeAirTemp.value = point.intakeAirTemp ?: 0
                _throttlePosition.value = point.throttlePosition ?: 0
                _fuelPressure.value = point.fuelPressure ?: 0
                _baroPressure.value = point.baroPressure ?: 0
                _batteryVoltage.value = point.batteryVoltage ?: 0f
                _speedGps.value = point.speedGps ?: 0
                _latitude.value = point.latitude
                _longitude.value = point.longitude
                _gForceX.value = point.gForceX ?: 0f
                _gForceY.value = point.gForceY ?: 0f
                _gForceZ.value = point.gForceZ ?: 0f
                _engineRunning.value = (point.engineLoad ?: 0) > 0
            }
            .launchIn(viewModelScope)
    }

    /**
     * Connect to OBD device and start data collection
     */
    fun connectToDevice(deviceAddress: String) {
        viewModelScope.launch {
            _connectionState.value = ConnectionState.Connecting
            _errorMessage.emit("Connecting to $deviceAddress...")
            
            // Connection is handled by CaseDashDataCollectorService on real device, 
            // but for UI flow we still call obdService.connect or trigger the service.
            // Here we keep it simple by calling obdService.connect which PollingEngine observes.
            val result = obdService.connect(deviceAddress)
            if (result is OBDService.ConnectionResult.Success) {
                // Polling engine is started by the Service, but we can also start it here if needed
                pollingEngine.start()
            } else if (result is OBDService.ConnectionResult.Error) {
                _errorMessage.emit("Connection error: ${result.message}")
            }
        }
    }

    /**
     * Disconnect from OBD device and stop data collection
     */
    fun disconnect() {
        viewModelScope.launch {
            pollingEngine.stop()
            obdService.disconnect()
            
            // Reset all state values
            _engineRunning.value = false
            _rpm.value = 0
            _engineLoad.value = 0
            _speed.value = 0
            _coolantTemp.value = 0
            _fuelLevel.value = 0
            _intakeAirTemp.value = 0
            _throttlePosition.value = 0
            _fuelPressure.value = 0
            _baroPressure.value = 0
            _batteryVoltage.value = 0f
            _averageSpeed.value = 0
        }
    }

    /**
     * Get paired Bluetooth devices
     */
    suspend fun getPairedDevices(): Set<BluetoothDevice> {
        return withContext(Dispatchers.IO) {
            obdService.bluetoothManager.getPairedDevices()
        }
    }

    /**
     * Check permissions (not implemented in OBDService)
     */
    fun checkPermissions(): String? {
        return null // OBDService doesn't have checkPermissions()
    }

    /**
     * Toggle verbose logging setting
     */
    fun toggleVerboseLogging(enabled: Boolean) {
        preferences.setVerboseLoggingEnabled(enabled)
        _verboseLoggingEnabled.value = enabled
    }
    
    /**
     * Set data collection frequency
     */
    fun setDataCollectionFrequency(frequencyMs: Int) {
        if (frequencyMs in 100..10000) { // Reasonable range check
            preferences.updateDataCollectionFrequency(frequencyMs)
        }
    }
    
    /**
     * Set storage frequency
     */
    fun setStorageFrequency(frequencyMs: Int) {
        if (frequencyMs in 1000..60000) { // Reasonable range check
            preferences.updateStorageFrequency(frequencyMs)
        }
    }

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        class Failed(val message: String) : ConnectionState()
    }
}