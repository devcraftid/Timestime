package com.firstlogistics.flproof.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

class AddressResolver(private val context: Context) {

    private val cache = ConcurrentHashMap<String, String>()

    suspend fun resolveAddress(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        if (latitude == 0.0 && longitude == 0.0) {
            return@withContext "Lokasi belum diperoleh"
        }

        val cacheKey = String.format(Locale.US, "%.4f,%.4f", latitude, longitude)
        cache[cacheKey]?.let { return@withContext it }

        if (!Geocoder.isPresent()) {
            val fallback = formatFallbackCoordinate(latitude, longitude)
            cache[cacheKey] = fallback
            return@withContext fallback
        }

        try {
            val geocoder = Geocoder(context, Locale("id", "ID"))
            val addressString = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine<String> { continuation ->
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val addr = addresses.firstOrNull()
                            if (addr != null) {
                                continuation.resume(formatAddress(addr))
                            } else {
                                continuation.resume(formatFallbackCoordinate(latitude, longitude))
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            continuation.resume(formatFallbackCoordinate(latitude, longitude))
                        }
                    })
                }
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val addr = addresses?.firstOrNull()
                if (addr != null) {
                    formatAddress(addr)
                } else {
                    formatFallbackCoordinate(latitude, longitude)
                }
            }

            cache[cacheKey] = addressString
            addressString
        } catch (e: Exception) {
            val fallback = formatFallbackCoordinate(latitude, longitude)
            cache[cacheKey] = fallback
            fallback
        }
    }

    private fun formatAddress(addr: Address): String {
        val sb = StringBuilder()
        val thoroughfare = addr.thoroughfare ?: addr.subThoroughfare ?: addr.featureName
        val subLocality = addr.subLocality
        val locality = addr.locality
        val subAdmin = addr.subAdminArea
        val admin = addr.adminArea

        if (!thoroughfare.isNullOrBlank()) sb.append(thoroughfare).append(", ")
        if (!subLocality.isNullOrBlank()) sb.append(subLocality).append(", ")
        if (!locality.isNullOrBlank()) sb.append(locality).append(", ")
        if (!subAdmin.isNullOrBlank() && subAdmin != locality) sb.append(subAdmin).append(", ")
        if (!admin.isNullOrBlank()) sb.append(admin)

        val result = sb.toString().trimEnd(',', ' ')
        return if (result.isNotBlank()) result else addr.getAddressLine(0) ?: "Lokasi Terdeteksi"
    }

    private fun formatFallbackCoordinate(lat: Double, lon: Double): String {
        return String.format(Locale.US, "Lat: %.6f, Lon: %.6f", lat, lon)
    }
}

