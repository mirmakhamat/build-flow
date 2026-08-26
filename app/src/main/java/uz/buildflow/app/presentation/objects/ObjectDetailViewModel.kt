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

data class ObjectDetailUiState(
    val obj: BuildObject? = null,
    val summary: ObjectFinancialSummary? = null,
    val isEditSheetOpen: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

class ObjectDetailViewModel(
    private val objectId: String,
    private val objectRepository: ObjectRepository,
    private val getObjectFinancialSummaryUseCase: GetObjectFinancialSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ObjectDetailUiState())
    val uiState: StateFlow<ObjectDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadData()
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadData()
    }

    private fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                objectRepository.getObjectById(objectId),
                getObjectFinancialSummaryUseCase(objectId)
            ) { obj, summary ->
                ObjectDetailUiState(
                    obj = obj,
                    summary = summary,
                    isEditSheetOpen = _uiState.value.isEditSheetOpen,
                    isLoading = false,
                    isRefreshing = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun openEditSheet() {
        _uiState.update { it.copy(isEditSheetOpen = true) }
    }

    fun closeEditSheet() {
        _uiState.update { it.copy(isEditSheetOpen = false) }
    }

    fun updateObject(
        name: String,
        description: String?,
        totalPrice: Double,
        startDate: String,
        status: ObjectStatus
    ) {
        viewModelScope.launch {
            val current = _uiState.value.obj ?: return@launch
            val updated = current.copy(
                name = name,
                description = description,
                totalPrice = totalPrice,
                startDate = startDate,
                status = status,
                updatedAt = System.currentTimeMillis()
            )
            objectRepository.updateObject(updated)
            closeEditSheet()
        }
    }

    companion object {
        fun provideFactory(
            objectId: String,
            objectRepository: ObjectRepository,
            getObjectFinancialSummaryUseCase: GetObjectFinancialSummaryUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ObjectDetailViewModel(objectId, objectRepository, getObjectFinancialSummaryUseCase) as T
            }
        }
    }
}
