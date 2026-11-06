package com.firstlogistics.flproof.ui.screens.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.data.repository.EvidenceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class GalleryUiState(
    val records: List<EvidenceEntity> = emptyList(),
    val trashedRecords: List<EvidenceEntity> = emptyList(),
    val isGridView: Boolean = true,
    val searchQuery: String = "",
    val selectedCategory: String = "",
    val selectedStatus: String = "",
    val showTrashTab: Boolean = false,
    val selectedItemsForBatch: Set<Long> = emptySet(),
    val isBatchMode: Boolean = false
)

class GalleryViewModel(
    private val evidenceRepository: EvidenceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GalleryUiState())
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private val _activeRecords = MutableStateFlow<List<EvidenceEntity>>(emptyList())
    val activeRecords: StateFlow<List<EvidenceEntity>> = _activeRecords.asStateFlow()

    private val _trashedRecords = MutableStateFlow<List<EvidenceEntity>>(emptyList())
    val trashedRecords: StateFlow<List<EvidenceEntity>> = _trashedRecords.asStateFlow()

    init {
        observeRecords()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeRecords() {
        viewModelScope.launch {
            _uiState.map { Triple(it.searchQuery, it.selectedCategory, it.selectedStatus) }
                .distinctUntilChanged()
                .flatMapLatest { (query, cat, status) ->
                    evidenceRepository.filterEvidence(query = query, category = cat, status = status)
                }.collect { filtered ->
                    _activeRecords.value = filtered
                }
        }

        viewModelScope.launch {
            evidenceRepository.getTrashedEvidence().collect {
                _trashedRecords.value = it
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setSelectedCategory(cat: String) {
        val next = if (_uiState.value.selectedCategory == cat) "" else cat
        _uiState.value = _uiState.value.copy(selectedCategory = next)
    }

    fun setSelectedStatus(status: String) {
        val next = if (_uiState.value.selectedStatus == status) "" else status
        _uiState.value = _uiState.value.copy(selectedStatus = next)
    }

    fun toggleViewMode() {
        _uiState.value = _uiState.value.copy(isGridView = !_uiState.value.isGridView)
    }

    fun toggleTrashTab() {
        _uiState.value = _uiState.value.copy(showTrashTab = !_uiState.value.showTrashTab)
    }

    fun toggleBatchSelection(id: Long) {
        val current = _uiState.value.selectedItemsForBatch.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _uiState.value = _uiState.value.copy(
            selectedItemsForBatch = current,
            isBatchMode = current.isNotEmpty()
        )
    }

    fun clearBatchSelection() {
        _uiState.value = _uiState.value.copy(
            selectedItemsForBatch = emptySet(),
            isBatchMode = false
        )
    }

    fun softDelete(id: Long) {
        viewModelScope.launch {
            evidenceRepository.softDelete(id)
        }
    }

    fun restore(id: Long) {
        viewModelScope.launch {
            evidenceRepository.restore(id)
        }
    }

    fun hardDelete(id: Long) {
        viewModelScope.launch {
            evidenceRepository.hardDelete(id)
        }
    }
}
