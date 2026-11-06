package com.firstlogistics.flproof.ui.screens.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.data.repository.EvidenceRepository
import com.firstlogistics.flproof.location.CompassSensorManager
import com.firstlogistics.flproof.location.LocationTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LocationsUiState(
    val locationRecords: List<EvidenceEntity> = emptyList(),
    val filteredRecords: List<EvidenceEntity> = emptyList(),
    val searchQuery: String = ""
)

class LocationViewModel(
    private val evidenceRepository: EvidenceRepository,
    val locationTracker: LocationTracker,
    val compassSensorManager: CompassSensorManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationsUiState())
    val uiState: StateFlow<LocationsUiState> = _uiState.asStateFlow()

    init {
        locationTracker.startTracking()
        compassSensorManager.startListening()

        viewModelScope.launch {
            evidenceRepository.getLocationsWithCoordinates().collect { list ->
                _uiState.value = _uiState.value.copy(
                    locationRecords = list,
                    filteredRecords = filterList(list, _uiState.value.searchQuery)
                )
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredRecords = filterList(_uiState.value.locationRecords, query)
        )
    }

    private fun filterList(list: List<EvidenceEntity>, query: String): List<EvidenceEntity> {
        if (query.isBlank()) return list
        return list.filter {
            it.address.contains(query, ignoreCase = true) ||
            it.evidenceId.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true)
        }
    }
}

