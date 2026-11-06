package com.firstlogistics.flproof.ui.screens.settings

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firstlogistics.flproof.data.model.WatermarkPosition
import com.firstlogistics.flproof.data.model.WatermarkSize
import com.firstlogistics.flproof.data.repository.SettingsRepository
import com.firstlogistics.flproof.export.BackupRestoreManager
import com.firstlogistics.flproof.sharing.ShareHelper
import com.firstlogistics.flproof.utils.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class SettingsUiState(
    val officerName: String = "",
    val officerId: String = "",
    val companyName: String = "",
    val watermarkPosition: WatermarkPosition = WatermarkPosition.BOTTOM_LEFT,
    val watermarkSize: WatermarkSize = WatermarkSize.MEDIUM,
    val isShutterSoundEnabled: Boolean = true,
    val isPinLockEnabled: Boolean = false,
    val appPin: String = "",
    val showCompass: Boolean = true,
    val showAccuracy: Boolean = true,
    val showAddress: Boolean = true,
    val photoQuality: Int = 92,
    val customerWebPortalUrl: String = "",
    val vercelPortalUrl: String = "",
    val supabaseUrl: String = "",
    val supabaseAnonKey: String = "",
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val storagePath: String = ""
)

class SettingsViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val storageManager: StorageManager,
    private val backupRestoreManager: BackupRestoreManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _uiState.value = SettingsUiState(
            officerName = settingsRepository.officerName,
            officerId = settingsRepository.officerId,
            companyName = settingsRepository.companyName,
            watermarkPosition = settingsRepository.watermarkPosition,
            watermarkSize = settingsRepository.watermarkSize,
            isShutterSoundEnabled = settingsRepository.isShutterSoundEnabled,
            isPinLockEnabled = settingsRepository.isPinLockEnabled,
            appPin = settingsRepository.appPin,
            showCompass = settingsRepository.showCompassInWatermark,
            showAccuracy = settingsRepository.showAccuracyInWatermark,
            showAddress = settingsRepository.showAddressInWatermark,
            photoQuality = settingsRepository.photoQuality,
            customerWebPortalUrl = settingsRepository.customerWebPortalUrl,
            vercelPortalUrl = settingsRepository.vercelPortalUrl,
            supabaseUrl = settingsRepository.supabaseUrl,
            supabaseAnonKey = settingsRepository.supabaseAnonKey,
            storagePath = storageManager.rootDir.absolutePath
        )
    }

    fun updateVercelPortalUrl(url: String) {
        settingsRepository.vercelPortalUrl = url
        _uiState.value = _uiState.value.copy(vercelPortalUrl = url)
    }

    fun updateSupabaseUrl(url: String) {
        settingsRepository.supabaseUrl = url
        _uiState.value = _uiState.value.copy(supabaseUrl = url)
    }

    fun updateSupabaseAnonKey(key: String) {
        settingsRepository.supabaseAnonKey = key
        _uiState.value = _uiState.value.copy(supabaseAnonKey = key)
    }

    fun updateCustomerWebPortalUrl(url: String) {
        settingsRepository.customerWebPortalUrl = url
        _uiState.value = _uiState.value.copy(customerWebPortalUrl = url)
    }

    fun updateOfficerName(name: String) {
        settingsRepository.officerName = name
        _uiState.value = _uiState.value.copy(officerName = name)
    }

    fun updateOfficerId(id: String) {
        settingsRepository.officerId = id
        _uiState.value = _uiState.value.copy(officerId = id)
    }

    fun updateCompanyName(name: String) {
        settingsRepository.companyName = name
        _uiState.value = _uiState.value.copy(companyName = name)
    }

    fun updateWatermarkPosition(position: WatermarkPosition) {
        settingsRepository.watermarkPosition = position
        _uiState.value = _uiState.value.copy(watermarkPosition = position)
    }

    fun updateWatermarkSize(size: WatermarkSize) {
        settingsRepository.watermarkSize = size
        _uiState.value = _uiState.value.copy(watermarkSize = size)
    }

    fun toggleShutterSound(enabled: Boolean) {
        settingsRepository.isShutterSoundEnabled = enabled
        _uiState.value = _uiState.value.copy(isShutterSoundEnabled = enabled)
    }

    fun togglePinLock(enabled: Boolean) {
        settingsRepository.isPinLockEnabled = enabled
        _uiState.value = _uiState.value.copy(isPinLockEnabled = enabled)
    }

    fun setAppPin(pin: String) {
        settingsRepository.appPin = pin
        _uiState.value = _uiState.value.copy(appPin = pin)
    }

    fun toggleShowCompass(enabled: Boolean) {
        settingsRepository.showCompassInWatermark = enabled
        _uiState.value = _uiState.value.copy(showCompass = enabled)
    }

    fun toggleShowAccuracy(enabled: Boolean) {
        settingsRepository.showAccuracyInWatermark = enabled
        _uiState.value = _uiState.value.copy(showAccuracy = enabled)
    }

    fun toggleShowAddress(enabled: Boolean) {
        settingsRepository.showAddressInWatermark = enabled
        _uiState.value = _uiState.value.copy(showAddress = enabled)
    }

    fun updatePhotoQuality(quality: Int) {
        settingsRepository.photoQuality = quality
        _uiState.value = _uiState.value.copy(photoQuality = quality)
    }

    fun createLocalBackup(activityContext: Context? = null) {
        val ctx = activityContext ?: context
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBackingUp = true)
            val file = backupRestoreManager.createBackup()
            _uiState.value = _uiState.value.copy(isBackingUp = false)

            if (file != null) {
                Toast.makeText(ctx, "Backup berhasil dibuat: ${file.name}", Toast.LENGTH_LONG).show()
                ShareHelper.shareFile(ctx, file, "application/zip", "Simpan / Bagikan File Backup FL PROOF")
            } else {
                Toast.makeText(ctx, "Gagal membuat backup lokal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun restoreLocalBackup(backupFile: File) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoring = true)
            val success = backupRestoreManager.restoreBackup(backupFile)
            _uiState.value = _uiState.value.copy(isRestoring = false)

            if (success) {
                Toast.makeText(context, "Data berhasil dipulihkan dari backup!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Gagal memulihkan data dari file backup", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

