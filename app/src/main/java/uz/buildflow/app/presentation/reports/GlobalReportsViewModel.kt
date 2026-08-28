package uz.buildflow.app.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.buildflow.app.domain.model.BuildObject
import uz.buildflow.app.domain.model.GlobalFinancialSummary
import uz.buildflow.app.domain.model.WorkerStats
import uz.buildflow.app.domain.repository.ObjectRepository
import uz.buildflow.app.domain.repository.WorkerRepository
import uz.buildflow.app.domain.usecase.GetGlobalFinancialSummaryUseCase
import uz.buildflow.app.domain.usecase.GetWorkerStatsUseCase

enum class GlobalReportTab(val title: String) {
    OVERVIEW("Umumiy"),
    OBJECTS_RANKING("Obyektlar"),
    EXPENSES_CATEGORIES("Xarajatlar"),
    INTER_OBJECT_BALANCES("Kassalararo")
}

enum class GlobalDrillDownType {
    GLOBAL_INCOMES,
    GLOBAL_EXPENSES,
    GLOBAL_WORKER_DEBTS,
    GLOBAL_CASH_BALANCES,
    GLOBAL_OWN_POCKET,
    INTER_OBJECT_DEBTS
}

data class GlobalReportsUiState(
    val isLoading: Boolean = true,
    val summary: GlobalFinancialSummary? = null,
    val availableObjects: List<BuildObject> = emptyList(),
    val selectedTab: GlobalReportTab = GlobalReportTab.OVERVIEW,
    val selectedDrillDownType: GlobalDrillDownType? = null,
    val allWorkersStats: List<WorkerStats> = emptyList()
)

class GlobalReportsViewModel(
    private val getGlobalFinancialSummaryUseCase: GetGlobalFinancialSummaryUseCase,
    private val objectRepository: ObjectRepository,
    private val workerRepository: WorkerRepository,
    private val getWorkerStatsUseCase: GetWorkerStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(GlobalReportsUiState())
    val uiState: StateFlow<GlobalReportsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                getGlobalFinancialSummaryUseCase(),
                objectRepository.getAllObjects(),
                workerRepository.getAllWorkers()
            ) { summary, objects, workers ->
                Triple(summary, objects, workers)
            }.flatMapLatest { (summary, objects, workers) ->
                if (workers.isEmpty()) {
                    flowOf(Triple(summary, objects, emptyList<WorkerStats>()))
                } else {
                    val statsFlows = workers.map { getWorkerStatsUseCase(it.id) }
                    combine(statsFlows) { statsArray ->
                        Triple(summary, objects, statsArray.filterNotNull())
                    }
                }
            }.collect { (summary, objects, workersStats) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        summary = summary,
                        availableObjects = objects,
                        allWorkersStats = workersStats
                    )
                }
            }
        }
    }

    fun selectTab(tab: GlobalReportTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun openDrillDown(type: GlobalDrillDownType) {
        _uiState.update { it.copy(selectedDrillDownType = type) }
    }

    fun closeDrillDown() {
        _uiState.update { it.copy(selectedDrillDownType = null) }
    }

    companion object {
        fun provideFactory(
            getGlobalFinancialSummaryUseCase: GetGlobalFinancialSummaryUseCase,
            objectRepository: ObjectRepository,
            workerRepository: WorkerRepository,
            getWorkerStatsUseCase: GetWorkerStatsUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GlobalReportsViewModel(
                    getGlobalFinancialSummaryUseCase,
                    objectRepository,
                    workerRepository,
                    getWorkerStatsUseCase
                ) as T
            }
        }
    }
}
