package com.firstlogistics.flproof.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GpsData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 0.0f,
    val altitude: Double = 0.0,
    val isAvailable: Boolean = false,
    val statusText: String = "Mencari sinyal GPS..."
)

class LocationTracker(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val _gpsData = MutableStateFlow(GpsData())
    val gpsData: StateFlow<GpsData> = _gpsData.asStateFlow()

    private var isTracking = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            updateLocation(location)
        }

        override fun onLocationAvailability(availability: LocationAvailability) {
            val available = availability.isLocationAvailable
            if (!available && _gpsData.value.latitude == 0.0) {
                _gpsData.value = _gpsData.value.copy(
                    isAvailable = false,
                    statusText = "GPS tidak tersedia"
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking) return
        isTracking = true

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    updateLocation(loc)
                }
            }

            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L)
                .setMinUpdateIntervalMillis(1500L)
                .setMinUpdateDistanceMeters(1.0f)
                .build()

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            _gpsData.value = GpsData(isAvailable = false, statusText = "GPS tidak aktif / izin ditolak")
        }
    }

    fun stopTracking() {
        if (!isTracking) return
        isTracking = false
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateLocation(loc: Location) {
        val accuracyQuality = when {
            loc.accuracy <= 5f -> "Akurasi Sangat Baik (±${loc.accuracy.toInt()}m)"
            loc.accuracy <= 15f -> "Akurasi Baik (±${loc.accuracy.toInt()}m)"
            loc.accuracy <= 30f -> "Akurasi Cukup (±${loc.accuracy.toInt()}m)"
            else -> "Akurasi Rendah (±${loc.accuracy.toInt()}m)"
        }

        _gpsData.value = GpsData(
            latitude = loc.latitude,
            longitude = loc.longitude,
            accuracy = loc.accuracy,
            altitude = loc.altitude,
            isAvailable = true,
            statusText = accuracyQuality
        )
    }
}

