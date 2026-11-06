package com.firstlogistics.flproof.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.firstlogistics.flproof.data.model.WatermarkConfig
import com.firstlogistics.flproof.data.model.WatermarkPosition
import com.firstlogistics.flproof.data.model.WatermarkSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("fl_proof_settings", Context.MODE_PRIVATE)

    private val _watermarkConfig = MutableStateFlow(loadWatermarkConfig())
    val watermarkConfig: StateFlow<WatermarkConfig> = _watermarkConfig.asStateFlow()

    var officerName: String
        get() = prefs.getString("officer_name", "Petugas Lapangan") ?: "Petugas Lapangan"
        set(value) {
            prefs.edit().putString("officer_name", value).apply()
            updateWatermarkConfig()
        }

    var officerId: String
        get() = prefs.getString("officer_id", "FL-OPR-01") ?: "FL-OPR-01"
        set(value) = prefs.edit().putString("officer_id", value).apply()

    var companyName: String
        get() = prefs.getString("company_name", "FIRST LOGISTICS") ?: "FIRST LOGISTICS"
        set(value) {
            prefs.edit().putString("company_name", value).apply()
            updateWatermarkConfig()
        }

    var watermarkPosition: WatermarkPosition
        get() = try {
            WatermarkPosition.valueOf(prefs.getString("wm_pos", WatermarkPosition.BOTTOM_LEFT.name)!!)
        } catch (e: Exception) {
            WatermarkPosition.BOTTOM_LEFT
        }
        set(value) {
            prefs.edit().putString("wm_pos", value.name).apply()
            updateWatermarkConfig()
        }

    var watermarkSize: WatermarkSize
        get() = try {
            WatermarkSize.valueOf(prefs.getString("wm_size", WatermarkSize.MEDIUM.name)!!)
        } catch (e: Exception) {
            WatermarkSize.MEDIUM
        }
        set(value) {
            prefs.edit().putString("wm_size", value.name).apply()
            updateWatermarkConfig()
        }

    var isShutterSoundEnabled: Boolean
        get() = prefs.getBoolean("shutter_sound", true)
        set(value) = prefs.edit().putBoolean("shutter_sound", value).apply()

    var isPinLockEnabled: Boolean
        get() = prefs.getBoolean("pin_lock_enabled", false)
        set(value) = prefs.edit().putBoolean("pin_lock_enabled", value).apply()

    var appPin: String
        get() = prefs.getString("app_pin", "") ?: ""
        set(value) = prefs.edit().putString("app_pin", value).apply()

    var photoQuality: Int
        get() = prefs.getInt("photo_quality", 92)
        set(value) = prefs.edit().putInt("photo_quality", value).apply()

    var showCompassInWatermark: Boolean
        get() = prefs.getBoolean("wm_show_compass", true)
        set(value) {
            prefs.edit().putBoolean("wm_show_compass", value).apply()
            updateWatermarkConfig()
        }

    var showAccuracyInWatermark: Boolean
        get() = prefs.getBoolean("wm_show_accuracy", true)
        set(value) {
            prefs.edit().putBoolean("wm_show_accuracy", value).apply()
            updateWatermarkConfig()
        }

    var showAddressInWatermark: Boolean
        get() = prefs.getBoolean("wm_show_address", true)
        set(value) {
            prefs.edit().putBoolean("wm_show_address", value).apply()
            updateWatermarkConfig()
        }

    var showQrInWatermark: Boolean
        get() = prefs.getBoolean("wm_show_qr", true)
        set(value) {
            prefs.edit().putBoolean("wm_show_qr", value).apply()
            updateWatermarkConfig()
        }

    companion object {
        const val DEFAULT_SUPABASE_URL = "https://huokirjroudttbmwjlma.supabase.co"
        const val DEFAULT_SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Imh1b2tpcmpyb3VkdHRibXdqbG1hIiwicm9sZSI6ImFub24iLCJpYXQiOjE3OTM5MzU1MjcsImV4cCI6MjEwOTUxMTUyN30.ij2UmmzBDB-oUos1nTkB6xu_VnJaaDL6JNyMRKRvO5c"
    }

    var customerWebPortalUrl: String
        get() = prefs.getString("customer_web_url", "") ?: ""
        set(value) = prefs.edit().putString("customer_web_url", value).apply()

    var vercelPortalUrl: String
        get() = prefs.getString("vercel_portal_url", "") ?: ""
        set(value) = prefs.edit().putString("vercel_portal_url", value).apply()

    var supabaseUrl: String
        get() = prefs.getString("supabase_url", DEFAULT_SUPABASE_URL)?.ifBlank { DEFAULT_SUPABASE_URL } ?: DEFAULT_SUPABASE_URL
        set(value) = prefs.edit().putString("supabase_url", value).apply()

    var supabaseAnonKey: String
        get() = prefs.getString("supabase_anon_key", DEFAULT_SUPABASE_ANON_KEY)?.ifBlank { DEFAULT_SUPABASE_ANON_KEY } ?: DEFAULT_SUPABASE_ANON_KEY
        set(value) = prefs.edit().putString("supabase_anon_key", value).apply()

    private fun loadWatermarkConfig(): WatermarkConfig {
        return WatermarkConfig(
            position = watermarkPosition,
            size = watermarkSize,
            showLogo = true,
            showCoordinates = true,
            showAddress = showAddressInWatermark,
            showAccuracy = showAccuracyInWatermark,
            showCompass = showCompassInWatermark,
            showResolution = true,
            companyName = companyName,
            officerName = officerName
        )
    }

    private fun updateWatermarkConfig() {
        _watermarkConfig.value = loadWatermarkConfig()
    }
}

