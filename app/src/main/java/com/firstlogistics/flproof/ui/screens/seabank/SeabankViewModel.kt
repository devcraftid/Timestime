package com.firstlogistics.flproof.ui.screens.seabank

import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firstlogistics.flproof.data.entity.SeabankPackageEntity
import com.firstlogistics.flproof.data.repository.SeabankPackageRepository
import com.firstlogistics.flproof.export.SeabankExportHelper
import com.firstlogistics.flproof.sharing.ShareHelper
import com.firstlogistics.flproof.utils.OfflineTextOcrScanner
import com.firstlogistics.flproof.utils.SeabankLabelParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class SeabankTab { WORK, ARCHIVE }

data class SeabankUiState(
    val activeTab: SeabankTab = SeabankTab.WORK,
    val awbInput: String = "",
    val confirmedAwb: String = "",
    val isAwbConfirmed: Boolean = false,
    val labelPhotoBitmap: Bitmap? = null,
    val recipient: String = "",
    val address: String = "",
    val rawOcrText: String = "",
    val isOcrRunning: Boolean = false,
    val ocrProgress: Float = 0f,
    val ocrStatusText: String = "Pilih atau foto label, lalu jalankan OCR.",
    val editingPackageId: String? = null,
    val packages: List<SeabankPackageEntity> = emptyList(),
    val totalCount: Int = 0,
    val todayCount: Int = 0,
    val searchQuery: String = "",
    val selectedIds: Set<String> = emptySet()
)

class SeabankViewModel(
    private val repository: SeabankPackageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeabankUiState())
    val uiState: StateFlow<SeabankUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allPackages.collect { list ->
                _uiState.value = _uiState.value.copy(
                    packages = list,
                    totalCount = list.size
                )
            }
        }

        viewModelScope.launch {
            repository.getTodayCount().collect { count ->
                _uiState.value = _uiState.value.copy(todayCount = count)
            }
        }
    }

    fun setTab(tab: SeabankTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
    }

    fun setAwbInput(awb: String) {
        _uiState.value = _uiState.value.copy(awbInput = awb)
    }

    fun confirmAwb() {
        val clean = _uiState.value.awbInput.trim()
        if (clean.isBlank()) return
        _uiState.value = _uiState.value.copy(
            confirmedAwb = clean,
            isAwbConfirmed = true
        )
    }

    fun setLabelBitmap(bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(
            labelPhotoBitmap = bitmap,
            ocrStatusText = "Foto label siap. Tekan tombol 'Ubah Foto Menjadi Teks' untuk membaca nama & alamat."
        )
    }

    fun rotateLabelBitmap() {
        val current = _uiState.value.labelPhotoBitmap ?: return
        val rotated = OfflineTextOcrScanner.rotateBitmap(current, 90f)
        _uiState.value = _uiState.value.copy(labelPhotoBitmap = rotated)
    }

    fun setRecipient(text: String) {
        _uiState.value = _uiState.value.copy(recipient = text)
    }

    fun setAddress(text: String) {
        _uiState.value = _uiState.value.copy(address = text)
    }

    fun runOcr() {
        val bitmap = _uiState.value.labelPhotoBitmap ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isOcrRunning = true,
                ocrProgress = 0.2f,
                ocrStatusText = "Membaca teks dari foto label (deteksi orientasi otomatis)..."
            )

            val ocrResult = OfflineTextOcrScanner.recognizeTextAutoOrient(bitmap)
            val rawText = ocrResult.text
            val parsed = SeabankLabelParser.extractFields(rawText)

            val finalAddress = when {
                parsed.address.isNotBlank() -> parsed.address
                parsed.cleanText.isNotBlank() -> parsed.cleanText
                else -> rawText.trim()
            }

            _uiState.value = _uiState.value.copy(
                isOcrRunning = false,
                ocrProgress = 1.0f,
                labelPhotoBitmap = ocrResult.rotatedBitmap,
                recipient = parsed.recipient.ifBlank { _uiState.value.recipient },
                address = finalAddress.ifBlank { _uiState.value.address },
                rawOcrText = parsed.cleanText.ifBlank { rawText },
                ocrStatusText = if (parsed.recipient.isNotBlank() || finalAddress.isNotBlank()) {
                    "OCR berhasil membaca data. Cocokkan dengan foto asli sebelum menyimpan."
                } else {
                    "Teks tidak dikenali dengan sempurna. Silakan ketik nama/alamat secara manual."
                }
            )
        }
    }

    fun savePackage(context: Context) {
        val awb = _uiState.value.confirmedAwb.ifBlank { _uiState.value.awbInput.trim() }
        val recipient = _uiState.value.recipient.trim()
        val address = _uiState.value.address.trim()

        if (awb.isBlank()) {
            Toast.makeText(context, "Nomor AWB belum diisi / dikonfirmasi.", Toast.LENGTH_SHORT).show()
            return
        }
        if (recipient.isBlank() || address.isBlank()) {
            Toast.makeText(context, "Penerima dan alamat wajib diisi.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            val dateFormatted = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date())
            val existingId = _uiState.value.editingPackageId
            val id = existingId ?: UUID.randomUUID().toString()

            val entity = SeabankPackageEntity(
                id = id,
                awb = awb,
                recipient = recipient,
                address = address,
                createdTimestamp = System.currentTimeMillis(),
                createdDateFormatted = dateFormatted
            )

            repository.savePackage(entity)
            Toast.makeText(context, "Paket AWB $awb berhasil disimpan!", Toast.LENGTH_SHORT).show()
            resetPackageForm()
        }
    }

    fun resetPackageForm() {
        _uiState.value = _uiState.value.copy(
            awbInput = "",
            confirmedAwb = "",
            isAwbConfirmed = false,
            labelPhotoBitmap = null,
            recipient = "",
            address = "",
            rawOcrText = "",
            isOcrRunning = false,
            ocrProgress = 0f,
            ocrStatusText = "Pilih atau foto label, lalu jalankan OCR.",
            editingPackageId = null
        )
    }

    fun editPackage(pkg: SeabankPackageEntity) {
        _uiState.value = _uiState.value.copy(
            activeTab = SeabankTab.WORK,
            editingPackageId = pkg.id,
            awbInput = pkg.awb,
            confirmedAwb = pkg.awb,
            isAwbConfirmed = true,
            recipient = pkg.recipient,
            address = pkg.address,
            rawOcrText = "Kepada: ${pkg.recipient}\n\nAlamat:\n${pkg.address}"
        )
    }

    fun deletePackage(pkg: SeabankPackageEntity) {
        viewModelScope.launch {
            repository.deletePackage(pkg)
            _uiState.value = _uiState.value.copy(
                selectedIds = _uiState.value.selectedIds - pkg.id
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun toggleSelect(id: String) {
        val current = _uiState.value.selectedIds
        _uiState.value = _uiState.value.copy(
            selectedIds = if (current.contains(id)) current - id else current + id
        )
    }

    fun selectAll() {
        val allIds = _uiState.value.packages.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(
            selectedIds = if (_uiState.value.selectedIds.size == allIds.size) emptySet() else allIds
        )
    }

    fun deleteSelected(context: Context) {
        val ids = _uiState.value.selectedIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteByIds(ids)
            _uiState.value = _uiState.value.copy(selectedIds = emptySet())
            Toast.makeText(context, "${ids.size} paket berhasil dihapus.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPackagesToExport(): List<SeabankPackageEntity> {
        val selected = _uiState.value.selectedIds
        return if (selected.isNotEmpty()) {
            _uiState.value.packages.filter { selected.contains(it.id) }
        } else {
            _uiState.value.packages
        }
    }

    fun exportExcel(context: Context) {
        val list = getPackagesToExport()
        if (list.isEmpty()) {
            Toast.makeText(context, "Belum ada paket untuk diekspor.", Toast.LENGTH_SHORT).show()
            return
        }
        val file = SeabankExportHelper.exportToCsv(context, list)
        ShareHelper.shareFile(context, file, "text/csv", "Ekspor Excel / CSV SeaBank")
    }

    fun exportDocument(context: Context) {
        val list = getPackagesToExport()
        if (list.isEmpty()) {
            Toast.makeText(context, "Belum ada paket untuk diekspor.", Toast.LENGTH_SHORT).show()
            return
        }
        val file = SeabankExportHelper.exportToDocument(context, list)
        ShareHelper.shareFile(context, file, "application/msword", "Ekspor Dokumen SeaBank")
    }

    fun exportPdf(context: Context) {
        val list = getPackagesToExport()
        if (list.isEmpty()) {
            Toast.makeText(context, "Belum ada paket untuk diekspor.", Toast.LENGTH_SHORT).show()
            return
        }
        val file = SeabankExportHelper.exportToPdf(context, list)
        ShareHelper.shareFile(context, file, "application/pdf", "Ekspor Laporan PDF SeaBank")
    }
}

