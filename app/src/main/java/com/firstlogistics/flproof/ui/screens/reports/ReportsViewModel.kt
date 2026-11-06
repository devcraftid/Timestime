package com.firstlogistics.flproof.ui.screens.reports

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.data.repository.EvidenceRepository
import com.firstlogistics.flproof.data.repository.SettingsRepository
import com.firstlogistics.flproof.export.CsvReportExporter
import com.firstlogistics.flproof.export.PdfReportGenerator
import com.firstlogistics.flproof.sharing.ShareHelper
import com.firstlogistics.flproof.utils.StorageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

enum class ReportPeriod(val title: String) {
    DAILY("Harian"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan"),
    ALL_TIME("Semua Periode")
}

data class ReportsUiState(
    val selectedPeriod: ReportPeriod = ReportPeriod.DAILY,
    val selectedCategory: String = "",
    val totalCount: Int = 0,
    val incomingCount: Int = 0,
    val outgoingCount: Int = 0,
    val problematicCount: Int = 0,
    val records: List<EvidenceEntity> = emptyList(),
    val isGeneratingPdf: Boolean = false,
    val isExportingCsv: Boolean = false,
    val generatedFile: File? = null
)

class ReportsViewModel(
    private val context: Context,
    private val evidenceRepository: EvidenceRepository,
    private val settingsRepository: SettingsRepository,
    private val storageManager: StorageManager,
    private val pdfReportGenerator: PdfReportGenerator,
    private val csvReportExporter: CsvReportExporter
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadReportData()
    }

    fun setPeriod(period: ReportPeriod) {
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        loadReportData()
    }

    fun setCategory(category: String) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadReportData()
    }

    fun loadReportData() {
        viewModelScope.launch {
            val (startTime, endTime) = calculateDateRange(_uiState.value.selectedPeriod)
            evidenceRepository.filterEvidence(
                category = _uiState.value.selectedCategory,
                startDate = startTime,
                endDate = endTime
            ).collect { list ->
                val incoming = list.count { it.category == "BARANG MASUK" || it.category == "PENERIMAAN PAKET" }
                val outgoing = list.count { it.category == "BARANG KELUAR" || it.category == "PENGIRIMAN" }
                val problematic = list.count { it.category == "BARANG RUSAK" || it.category == "BARANG HILANG" || it.status == "FLAGGED" }

                _uiState.value = _uiState.value.copy(
                    records = list,
                    totalCount = list.size,
                    incomingCount = incoming,
                    outgoingCount = outgoing,
                    problematicCount = problematic
                )
            }
        }
    }

    private fun calculateDateRange(period: ReportPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val endTime = cal.timeInMillis

        return when (period) {
            ReportPeriod.DAILY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                Pair(cal.timeInMillis, endTime)
            }
            ReportPeriod.WEEKLY -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                Pair(cal.timeInMillis, endTime)
            }
            ReportPeriod.MONTHLY -> {
                cal.add(Calendar.DAY_OF_YEAR, -30)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                Pair(cal.timeInMillis, endTime)
            }
            ReportPeriod.ALL_TIME -> Pair(0L, 0L)
        }
    }

    fun exportPdf(activityContext: Context? = null) {
        val list = _uiState.value.records
        val ctx = activityContext ?: context
        if (list.isEmpty()) {
            Toast.makeText(ctx, "Tidak ada data pada periode ini", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingPdf = true)
            val periodTitle = _uiState.value.selectedPeriod.title.uppercase()
            val file = pdfReportGenerator.generateReport(
                title = "LAPORAN $periodTitle",
                records = list,
                companyName = settingsRepository.companyName,
                officerName = settingsRepository.officerName
            )
            _uiState.value = _uiState.value.copy(isGeneratingPdf = false, generatedFile = file)

            if (file != null) {
                ShareHelper.shareFile(ctx, file, "application/pdf", "Bagikan Laporan PDF ($periodTitle)")
            } else {
                Toast.makeText(ctx, "Gagal membuat laporan PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun exportCsv(activityContext: Context? = null) {
        val list = _uiState.value.records
        val ctx = activityContext ?: context
        if (list.isEmpty()) {
            Toast.makeText(ctx, "Tidak ada data pada periode ini", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExportingCsv = true)
            val file = csvReportExporter.exportToCsv(list)
            _uiState.value = _uiState.value.copy(isExportingCsv = false, generatedFile = file)

            if (file != null) {
                ShareHelper.shareFile(ctx, file, "text/csv", "Bagikan Laporan CSV")
            } else {
                Toast.makeText(ctx, "Gagal membuat file CSV", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

