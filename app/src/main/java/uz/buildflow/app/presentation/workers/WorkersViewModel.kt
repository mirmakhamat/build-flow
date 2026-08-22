package uz.buildflow.app.presentation.workers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.buildflow.app.domain.model.BuildObject
import uz.buildflow.app.domain.model.Worker
import uz.buildflow.app.domain.model.WorkerStats
import uz.buildflow.app.domain.repository.ObjectRepository
import uz.buildflow.app.domain.repository.WorkerRepository
import uz.buildflow.app.domain.usecase.GetWorkerStatsUseCase

data class WorkerWithStats(
    val worker: Worker,
    val stats: WorkerStats?
)

data class WorkersUiState(
    val workers: List<WorkerWithStats> = emptyList(),
    val availableObjects: List<BuildObject> = emptyList(),
    val selectedObjectId: String? = null,
    val isAddEditSheetOpen: Boolean = false,
    val selectedWorker: Worker? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

class WorkersViewModel(
    private val workerRepository: WorkerRepository,
    private val objectRepository: ObjectRepository,
    private val getWorkerStatsUseCase: GetWorkerStatsUseCase,
    initialObjectId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkersUiState(selectedObjectId = initialObjectId))
    val uiState: StateFlow<WorkersUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadObjectsAndWorkers(initialObjectId)
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadObjectsAndWorkers(_uiState.value.selectedObjectId)
    }

    fun selectObject(objectId: String) {
        _uiState.update { it.copy(selectedObjectId = objectId) }
        loadWorkersForObject(objectId)
    }

    private fun loadObjectsAndWorkers(initialObjectId: String?) {
        viewModelScope.launch {
            objectRepository.getAllObjects().collect { objects ->
                val activeObjects = objects.filter { it.status == uz.buildflow.app.domain.model.ObjectStatus.ACTIVE }
                val targetObjectId = initialObjectId 
                    ?: activeObjects.firstOrNull()?.id 
                    ?: objects.firstOrNull()?.id

                _uiState.update {
                    it.copy(
                        availableObjects = objects,
                        selectedObjectId = targetObjectId
                    )
                }

                if (targetObjectId != null) {
                    loadWorkersForObject(targetObjectId)
                } else {
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
                }
            }
        }
    }

    private fun loadWorkersForObject(objectId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            workerRepository.getWorkersByObject(objectId).flatMapLatest { workerList ->
                if (workerList.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val statsFlows = workerList.map { w ->
                        getWorkerStatsUseCase(w.id).map { stats ->
                            WorkerWithStats(w, stats)
                        }
                    }
                    combine(statsFlows) { it.toList() }
                }
            }.collect { list ->
                _uiState.update {
                    it.copy(
                        workers = list,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }
        }
    }

    fun openAddWorker() {
        if (_uiState.value.availableObjects.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Ishchi qo'shish uchun avval Obyektlar bo'limida obyekt yarating!") }
            return
        }
        _uiState.update { it.copy(selectedWorker = null, isAddEditSheetOpen = true) }
    }

    fun openEditWorker(worker: Worker) {
        _uiState.update { it.copy(selectedWorker = worker, isAddEditSheetOpen = true) }
    }

    fun closeAddEditSheet() {
        _uiState.update { it.copy(isAddEditSheetOpen = false, selectedWorker = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun saveWorker(objectId: String, name: String, phone: String?, position: String?, defaultRate: Double, startDate: String) {
        viewModelScope.launch {
            val selected = _uiState.value.selectedWorker
            if (selected != null) {
                val updated = selected.copy(
                    objectId = objectId,
                    name = name,
                    phone = phone,
                    position = position,
                    defaultRate = defaultRate,
                    startDate = startDate,
                    updatedAt = System.currentTimeMillis()
                )
                workerRepository.updateWorker(updated)
            } else {
                val newWorker = Worker(
                    objectId = objectId,
                    name = name,
                    phone = phone,
                    position = position,
                    defaultRate = defaultRate,
                    startDate = startDate
                )
                workerRepository.insertWorker(newWorker)
            }
            closeAddEditSheet()
        }
    }

    companion object {
        fun provideFactory(
            workerRepository: WorkerRepository,
            objectRepository: ObjectRepository,
            getWorkerStatsUseCase: GetWorkerStatsUseCase,
            initialObjectId: String? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WorkersViewModel(workerRepository, objectRepository, getWorkerStatsUseCase, initialObjectId) as T
            }
        }
    }
}
