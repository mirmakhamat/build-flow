package uz.buildflow.app.presentation.objects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.buildflow.app.domain.model.BuildObject
import uz.buildflow.app.domain.model.ObjectFinancialSummary
import uz.buildflow.app.domain.model.ObjectStatus
import uz.buildflow.app.domain.repository.ObjectRepository
import uz.buildflow.app.domain.usecase.GetObjectFinancialSummaryUseCase

data class ObjectWithSummary(
    val obj: BuildObject,
    val summary: ObjectFinancialSummary?
)

data class ObjectsUiState(
    val objects: List<ObjectWithSummary> = emptyList(),
    val isAddEditSheetOpen: Boolean = false,
    val selectedObject: BuildObject? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

class ObjectsViewModel(
    private val objectRepository: ObjectRepository,
    private val getObjectFinancialSummaryUseCase: GetObjectFinancialSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObjectsUiState())
    val uiState: StateFlow<ObjectsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadObjects()
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadObjects()
    }

    private fun loadObjects() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            objectRepository.getAllObjects().flatMapLatest { objList ->
                if (objList.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    val summaryFlows = objList.map { obj ->
                        getObjectFinancialSummaryUseCase(obj.id).map { summary ->
                            ObjectWithSummary(obj, summary)
                        }
                    }
                    combine(summaryFlows) { it.toList() }
                }
            }.collect { list ->
                _uiState.value = _uiState.value.copy(
                    objects = list,
                    isLoading = false,
                    isRefreshing = false
                )
            }
        }
    }

    fun openAddObject() {
        _uiState.update { it.copy(selectedObject = null, isAddEditSheetOpen = true) }
    }

    fun openEditObject(obj: BuildObject) {
        _uiState.update { it.copy(selectedObject = obj, isAddEditSheetOpen = true) }
    }

    fun closeAddEditSheet() {
        _uiState.update { it.copy(isAddEditSheetOpen = false, selectedObject = null) }
    }

    fun saveObject(name: String, description: String?, totalPrice: Double, startDate: String, status: ObjectStatus) {
        viewModelScope.launch {
            val selected = _uiState.value.selectedObject
            if (selected != null) {
                val updated = selected.copy(
                    name = name,
                    description = description,
                    totalPrice = totalPrice,
                    startDate = startDate,
                    status = status,
                    updatedAt = System.currentTimeMillis()
                )
                objectRepository.updateObject(updated)
            } else {
                val newObj = BuildObject(
                    name = name,
                    description = description,
                    totalPrice = totalPrice,
                    startDate = startDate,
                    status = status
                )
                objectRepository.insertObject(newObj)
            }
            closeAddEditSheet()
        }
    }

    companion object {
        fun provideFactory(
            objectRepository: ObjectRepository,
            getObjectFinancialSummaryUseCase: GetObjectFinancialSummaryUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ObjectsViewModel(objectRepository, getObjectFinancialSummaryUseCase) as T
            }
        }
    }
}
