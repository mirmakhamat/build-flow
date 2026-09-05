package uz.buildflow.app.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import uz.buildflow.app.domain.model.*
import uz.buildflow.app.domain.usecase.GetGlobalWorkersReportUseCase

data class GlobalWorkersReportUiState(
    val isLoading: Boolean = true,
    val summary: GlobalWorkersReportSummary? = null,
    val filteredWorkers: List<WorkerGlobalReportItem> = emptyList(),
    val searchQuery: String = "",
    val activeFilter: WorkerReportFilter = WorkerReportFilter.ALL,
    val activeSort: WorkerReportSort = WorkerReportSort.DEBT_DESC,
    val selectedWorkerForDetails: WorkerGlobalReportItem? = null
)

class GlobalWorkersReportViewModel(
    private val getGlobalWorkersReportUseCase: GetGlobalWorkersReportUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(WorkerReportFilter.ALL)
    private val _activeSort = MutableStateFlow(WorkerReportSort.DEBT_DESC)
    private val _selectedWorker = MutableStateFlow<WorkerGlobalReportItem?>(null)

    val uiState: StateFlow<GlobalWorkersReportUiState> = combine(
        getGlobalWorkersReportUseCase(),
        _searchQuery,
        _activeFilter,
        _activeSort,
        _selectedWorker
    ) { summary, query, filter, sort, selectedWorker ->
        val filtered = summary.workers.filter { item ->
            // 1. Qidiruv filtri (Ism, kasb, telefon)
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                val q = query.trim().lowercase()
                item.workerName.lowercase().contains(q) ||
                        item.position.lowercase().contains(q) ||
                        (item.phone?.contains(q) == true) ||
                        item.workedObjects.any { it.lowercase().contains(q) }
            }

            // 2. Status filtri
            val matchesFilter = when (filter) {
                WorkerReportFilter.ALL -> true
                WorkerReportFilter.HAS_DEBT -> item.balance > 0
                WorkerReportFilter.FULLY_PAID -> item.balance == 0.0 && item.totalEarned > 0
                WorkerReportFilter.HAS_ADVANCE -> item.balance < 0
            }

            matchesQuery && matchesFilter
        }.let { list ->
            // 3. Saralash (Sort)
            when (sort) {
                WorkerReportSort.DEBT_DESC -> list.sortedByDescending { it.balance }
                WorkerReportSort.EARNED_DESC -> list.sortedByDescending { it.totalEarned }
                WorkerReportSort.DAYS_DESC -> list.sortedByDescending { it.workedDaysCount }
                WorkerReportSort.NAME_ASC -> list.sortedBy { it.workerName.lowercase() }
            }
        }

        GlobalWorkersReportUiState(
            isLoading = false,
            summary = summary,
            filteredWorkers = filtered,
            searchQuery = query,
            activeFilter = filter,
            activeSort = sort,
            selectedWorkerForDetails = selectedWorker
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GlobalWorkersReportUiState(isLoading = true)
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChanged(filter: WorkerReportFilter) {
        _activeFilter.value = filter
    }

    fun onSortChanged(sort: WorkerReportSort) {
        _activeSort.value = sort
    }

    fun onSelectWorker(workerItem: WorkerGlobalReportItem?) {
        _selectedWorker.value = workerItem
    }

    companion object {
        fun provideFactory(
            getGlobalWorkersReportUseCase: GetGlobalWorkersReportUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GlobalWorkersReportViewModel(getGlobalWorkersReportUseCase) as T
            }
        }
    }
}
