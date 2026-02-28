package com.fuseforge.cardash.ui.history

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fuseforge.cardash.CarDashApp
import com.fuseforge.cardash.data.db.AppDatabase
import com.fuseforge.cardash.data.db.TripDataPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val context: Context) : ViewModel() {
    private val dao = AppDatabase.getDatabase(context).obdLogDao()
    
    // Stream of the last 25 readings
    val lastReadings: StateFlow<List<TripDataPoint>> = dao.getLastTripDataPoints(25)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // List of parameters to display
    val parameters: List<ParameterInfo> = listOf(
        ParameterInfo("RPM", "rpm", "rpm") { it.rpm?.toString() ?: "-" },
        ParameterInfo("Speed", "speed", "km/h") { it.speedObd?.toString() ?: "-" },
        ParameterInfo("Load", "engineLoad", "%") { it.engineLoad?.toString() ?: "-" },
        ParameterInfo("Coolant", "coolantTemp", "°C") { it.coolantTemp?.toString() ?: "-" },
        ParameterInfo("Fuel", "fuelLevel", "%") { it.fuelLevel?.toString() ?: "-" },
        ParameterInfo("IAT", "intakeAirTemp", "°C") { it.intakeAirTemp?.toString() ?: "-" },
        ParameterInfo("Throttle", "throttlePosition", "%") { it.throttlePosition?.toString() ?: "-" },
        ParameterInfo("F.Press", "fuelPressure", "kPa") { it.fuelPressure?.toString() ?: "-" },
        ParameterInfo("B.Press", "baroPressure", "kPa") { it.baroPressure?.toString() ?: "-" },
        ParameterInfo("Battery", "batteryVoltage", "V") { 
            it.batteryVoltage?.let { voltage -> String.format("%.1f", voltage) } ?: "-" 
        }
    )
}

/**
 * Class representing a parameter to display in the history screen
 */
data class ParameterInfo(
    val displayName: String,
    val id: String,
    val unit: String,
    val valueFormatter: (TripDataPoint) -> String
)

/**
 * Factory for creating HistoryViewModel with application context
 */
class HistoryViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistoryViewModel(context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}