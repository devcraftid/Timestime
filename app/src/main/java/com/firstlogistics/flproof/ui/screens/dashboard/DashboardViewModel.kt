package com.firstlogistics.flproof.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.data.repository.EvidenceRepository
import com.firstlogistics.flproof.data.repository.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val totalCount: Int = 0,
    val totalIncoming: Int = 0,
    val totalOutgoing: Int = 0,
    val totalToday: Int = 0,
    val totalProblematic: Int = 0,
    val recentRecords: List<EvidenceEntity> = emptyList(),
    val currentTimeString: String = "",
    val currentDateString: String = "",
    val officerName: String = "",
    val companyName: String = "FIRST LOGISTICS"
)

class DashboardViewModel(
    private val evidenceRepository: EvidenceRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadMetrics()
        startClockTicker()
    }

    private fun loadMetrics() {
        viewModelScope.launch {
            val statsFlow = combine(
                evidenceRepository.getTotalCount(),
                evidenceRepository.getTotalIncoming(),
                evidenceRepository.getTotalOutgoing(),
                evidenceRepository.getTotalToday(),
                evidenceRepository.getTotalProblematic()
            ) { count, inc, out, today, prob ->
                arrayOf(count, inc, out, today, prob)
            }

            combine(
                statsFlow,
                evidenceRepository.getRecentEvidence(6)
            ) { stats, recent ->
                _uiState.value = _uiState.value.copy(
                    totalCount = stats[0] as Int,
                    totalIncoming = stats[1] as Int,
                    totalOutgoing = stats[2] as Int,
                    totalToday = stats[3] as Int,
                    totalProblematic = stats[4] as Int,
                    recentRecords = recent,
                    officerName = settingsRepository.officerName,
                    companyName = settingsRepository.companyName
                )
            }.collect()
        }
    }

    private fun startClockTicker() {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                _uiState.value = _uiState.value.copy(
                    currentTimeString = com.firstlogistics.flproof.utils.DateFormatter.getCurrentTimeFormatted(now),
                    currentDateString = com.firstlogistics.flproof.utils.DateFormatter.getCurrentDateFormatted(now)
                )
                delay(1000L)
            }
        }
    }
}

