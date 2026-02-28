package com.fuseforge.cardash.services.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "SensorCollector"

/**
 * Collects GPS and IMU data from Android sensors.
 * Optimized for low latency and high accuracy during driving.
 */
class SensorCollector(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val _lastLocation = MutableStateFlow<Location?>(null)
    val lastLocation: StateFlow<Location?> = _lastLocation.asStateFlow()

    private val _linearAcceleration = MutableStateFlow(FloatArray(3))
    val linearAcceleration: StateFlow<FloatArray> = _linearAcceleration.asStateFlow()

    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
        .setMinUpdateIntervalMillis(500)
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            _lastLocation.value = locationResult.lastLocation
        }
    }

    fun start() {
        Log.d(TAG, "Starting sensor collection")
        startLocationUpdates()
        startSensorUpdates()
    }

    fun stop() {
        Log.d(TAG, "Stopping sensor collection")
        stopLocationUpdates()
        stopSensorUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error starting location updates: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun startSensorUpdates() {
        // Use LINEAR_ACCELERATION to ignore gravity and get actual vehicle movement
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        if (accel != null) {
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_GAME)
        } else {
            Log.w(TAG, "Linear Acceleration sensor not available, falling back to Accelerometer")
            val fallback = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            sensorManager.registerListener(this, fallback, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    private fun stopSensorUpdates() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LINEAR_ACCELERATION, Sensor.TYPE_ACCELEROMETER -> {
                _linearAcceleration.value = event.values.copyOf()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
