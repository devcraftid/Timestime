package com.firstlogistics.flproof.ui.screens.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firstlogistics.flproof.FLProofApplication
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.data.repository.EvidenceRepository
import com.firstlogistics.flproof.data.repository.SettingsRepository
import com.firstlogistics.flproof.export.PdfReportGenerator
import com.firstlogistics.flproof.sharing.ShareHelper
import com.firstlogistics.flproof.utils.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class EvidenceDetailUiState(
    val evidence: EvidenceEntity? = null,
    val selectedMedia: EvidenceEntity? = null,
    val sessionMediaList: List<EvidenceEntity> = emptyList(),
    val showOriginalPhoto: Boolean = false,
    val isExportingPdf: Boolean = false,
    val isUploadingCloud: Boolean = false,
    val cloudStatusMessage: String? = null,
    val message: String? = null
)

class EvidenceDetailViewModel(
    private val context: Context,
    private val evidenceRepository: EvidenceRepository,
    private val settingsRepository: SettingsRepository,
    private val storageManager: StorageManager,
    private val pdfReportGenerator: PdfReportGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(EvidenceDetailUiState())
    val uiState: StateFlow<EvidenceDetailUiState> = _uiState.asStateFlow()

    fun loadEvidence(evidenceId: String) {
        viewModelScope.launch {
            val entity = evidenceRepository.getByEvidenceId(evidenceId) ?: evidenceRepository.findEvidenceByCode(evidenceId)
            if (entity != null) {
                _uiState.value = _uiState.value.copy(
                    evidence = entity,
                    selectedMedia = entity
                )

                // Load all session media for multi-media gallery
                val sessionId = entity.sessionId ?: entity.evidenceId
                evidenceRepository.getSessionPhotos(sessionId).collect { mediaList ->
                    _uiState.value = _uiState.value.copy(
                        sessionMediaList = mediaList,
                        selectedMedia = _uiState.value.selectedMedia ?: mediaList.firstOrNull() ?: entity
                    )
                }
            }
        }
    }

    fun selectMedia(item: EvidenceEntity) {
        _uiState.value = _uiState.value.copy(selectedMedia = item)
    }

    fun togglePhotoView() {
        _uiState.value = _uiState.value.copy(showOriginalPhoto = !_uiState.value.showOriginalPhoto)
    }

    fun playVideo(activityContext: Context? = null, videoPath: String?) {
        val path = videoPath ?: _uiState.value.selectedMedia?.videoPath ?: return
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(context, "File video tidak ditemukan di perangkat.", Toast.LENGTH_SHORT).show()
            return
        }

        val ctx = activityContext ?: context
        try {
            val uri = FileProvider.getUriForFile(
                ctx,
                "${ctx.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "video/mp4")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Gagal memutar video: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun syncSessionToSupabase() {
        val ev = _uiState.value.evidence ?: return
        val sessionId = ev.sessionId ?: ev.evidenceId

        val app = context.applicationContext as? FLProofApplication
        val uploader = app?.supabaseUploader
        if (uploader == null) {
            Toast.makeText(context, "Layanan Supabase tidak tersedia.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUploadingCloud = true, cloudStatusMessage = "Mengunggah media ke Supabase...")
            val res = uploader.syncSessionToCloud(sessionId)
            _uiState.value = _uiState.value.copy(isUploadingCloud = false)

            if (res.isSuccess) {
                val count = res.getOrNull() ?: 0
                _uiState.value = _uiState.value.copy(cloudStatusMessage = "Berhasil sinkronisasi $count media ke Cloud!")
                Toast.makeText(context, "Berhasil upload $count media ke Supabase & Vercel!", Toast.LENGTH_LONG).show()
                // Reload session media list
                loadEvidence(ev.evidenceId)
            } else {
                val err = res.exceptionOrNull()?.message ?: "Gagal upload"
                _uiState.value = _uiState.value.copy(cloudStatusMessage = "Upload gagal: $err")
                Toast.makeText(context, "Gagal upload: $err", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun getVercelPortalUrl(): String {
        val ev = _uiState.value.evidence ?: return ""
        val sessionId = ev.sessionId ?: ev.evidenceId
        val portalBase = settingsRepository.vercelPortalUrl.trim().ifBlank {
            settingsRepository.customerWebPortalUrl.trim()
        }
        return if (portalBase.isNotBlank()) {
            if (portalBase.contains("?")) "$portalBase&id=$sessionId" else "$portalBase?id=$sessionId"
        } else {
            val app = context.applicationContext as? FLProofApplication
            app?.evidenceHttpServer?.getSessionUrl(sessionId) ?: "http://192.168.43.1:8080/session?id=$sessionId"
        }
    }

    fun openVercelPortal(activityContext: Context? = null) {
        val url = getVercelPortalUrl()
        val ctx = activityContext ?: context
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(ctx, "Gagal membuka browser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyVercelUrl() {
        val url = getVercelPortalUrl()
        copyToClipboard("Link Web Bukti Pelanggan", url)
        Toast.makeText(context, "Link portal disalin: $url", Toast.LENGTH_SHORT).show()
    }

    fun copyCoordinates() {
        val ev = _uiState.value.selectedMedia ?: _uiState.value.evidence ?: return
        val text = "${ev.latitude}, ${ev.longitude}"
        copyToClipboard("Koordinat GPS", text)
        Toast.makeText(context, "Koordinat disalin: $text", Toast.LENGTH_SHORT).show()
    }

    fun copyAddress() {
        val ev = _uiState.value.selectedMedia ?: _uiState.value.evidence ?: return
        copyToClipboard("Alamat Dokumentasi", ev.address)
        Toast.makeText(context, "Alamat disalin", Toast.LENGTH_SHORT).show()
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
    }

    fun saveToGallery() {
        val ev = _uiState.value.selectedMedia ?: _uiState.value.evidence ?: return
        if (ev.mediaType == "video") {
            Toast.makeText(context, "File video tersimpan di folder Videos/FL_PROOF", Toast.LENGTH_LONG).show()
            return
        }
        val targetPath = if (_uiState.value.showOriginalPhoto) ev.originalPhotoPath else ev.watermarkedPhotoPath
        val file = File(targetPath)
        val success = storageManager.copyToGallery(file, ev.evidenceId)
        if (success) {
            Toast.makeText(context, "Foto berhasil disimpan ke Galeri perangkat (FL_PROOF)", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Gagal mengekspor foto ke Galeri", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareEvidence(activityContext: Context? = null) {
        val ev = _uiState.value.selectedMedia ?: _uiState.value.evidence ?: return
        val ctx = activityContext ?: context
        ShareHelper.shareEvidence(ctx, ev, useWatermarked = !_uiState.value.showOriginalPhoto)
    }

    fun exportPdfReport(activityContext: Context? = null) {
        val ev = _uiState.value.evidence ?: return
        val ctx = activityContext ?: context
        val list = _uiState.value.sessionMediaList.ifEmpty { listOf(ev) }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExportingPdf = true)
            val file = pdfReportGenerator.generateReport(
                title = "LAPORAN SESI BUKTI #${ev.sessionId ?: ev.evidenceId}",
                records = list
            )
            _uiState.value = _uiState.value.copy(isExportingPdf = false)
            if (file != null) {
                ShareHelper.shareFile(ctx, file, "application/pdf", "Bagikan Laporan Bukti PDF")
            } else {
                Toast.makeText(ctx, "Gagal membuat PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun openCustomerWebViewer(activityContext: Context? = null) {
        val ev = _uiState.value.evidence ?: return
        val ctx = activityContext ?: context
        viewModelScope.launch {
            try {
                val sessionPhotos = evidenceRepository.getSessionPhotosList(ev.sessionId ?: ev.evidenceId)
                val htmlFile = storageManager.generateHtmlWebViewer(ev, sessionPhotos)
                val uri = FileProvider.getUriForFile(
                    ctx,
                    "${ctx.packageName}.fileprovider",
                    htmlFile
                )
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "text/html")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(intent)
            } catch (e: Exception) {
                openVercelPortal(ctx)
            }
        }
    }
}
