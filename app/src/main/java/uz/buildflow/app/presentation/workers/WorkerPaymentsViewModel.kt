package uz.buildflow.app.presentation.workers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.PaymentType
import uz.buildflow.app.domain.model.WorkerPayment
import uz.buildflow.app.domain.model.WorkerStats
import uz.buildflow.app.domain.repository.TransactionRepository
import uz.buildflow.app.domain.repository.WorkerRepository
import uz.buildflow.app.domain.usecase.GetWorkerStatsUseCase

data class WorkerPaymentsUiState(
    val payments: List<WorkerPayment> = emptyList(),
    val stats: WorkerStats? = null,
    val selectedPayment: WorkerPayment? = null,
    val isAddSheetOpen: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)

class WorkerPaymentsViewModel(
    private val workerId: String,
    private val initialObjectId: String?,
    private val workerRepository: WorkerRepository,
    private val transactionRepository: TransactionRepository,
    private val getWorkerStatsUseCase: GetWorkerStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerPaymentsUiState())
    val uiState: StateFlow<WorkerPaymentsUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadPayments()
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadPayments()
    }

    private fun loadPayments() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                transactionRepository.getPaymentsByWorker(workerId),
                getWorkerStatsUseCase(workerId)
            ) { paymentsList, stats ->
                WorkerPaymentsUiState(
                    payments = paymentsList,
                    stats = stats,
                    selectedPayment = _uiState.value.selectedPayment,
                    isAddSheetOpen = _uiState.value.isAddSheetOpen,
                    isLoading = false,
                    isRefreshing = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun openAddPayment() {
        _uiState.update { it.copy(selectedPayment = null, isAddSheetOpen = true) }
    }

    fun openEditPayment(payment: WorkerPayment) {
        _uiState.update { it.copy(selectedPayment = payment, isAddSheetOpen = true) }
    }

    fun closeAddPayment() {
        _uiState.update { it.copy(isAddSheetOpen = false, selectedPayment = null) }
    }

    fun savePayment(amount: Double, date: String, type: PaymentType, description: String?, payerObjectId: String? = null, paymentDate: String? = null) {
        viewModelScope.launch {
            val objectId = if (!initialObjectId.isNullOrBlank()) {
                initialObjectId
            } else {
                val worker = workerRepository.getWorkerById(workerId).firstOrNull()
                worker?.objectId ?: ""
            }

            val existing = _uiState.value.selectedPayment
            if (existing != null) {
                val updated = existing.copy(
                    amount = amount,
                    date = date.ifBlank { DateUtil.today() },
                    paymentDate = paymentDate,
                    type = type,
                    payerObjectId = payerObjectId,
                    description = description
                )
                transactionRepository.updateWorkerPayment(updated)
            } else {
                val payment = WorkerPayment(
                    workerId = workerId,
                    objectId = objectId,
                    payerObjectId = payerObjectId,
                    amount = amount,
                    date = date.ifBlank { DateUtil.today() },
                    paymentDate = paymentDate,
                    type = type,
                    description = description
                )
                transactionRepository.insertWorkerPayment(payment)
            }
            closeAddPayment()
        }
    }

    fun deletePayment(payment: WorkerPayment) {
        viewModelScope.launch {
            transactionRepository.deleteWorkerPayment(payment)
            closeAddPayment()
        }
    }

    companion object {
        fun provideFactory(
            workerId: String,
            objectId: String?,
            workerRepository: WorkerRepository,
            transactionRepository: TransactionRepository,
            getWorkerStatsUseCase: GetWorkerStatsUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WorkerPaymentsViewModel(
                    workerId,
                    objectId,
                    workerRepository,
                    transactionRepository,
                    getWorkerStatsUseCase
                ) as T
            }
        }
    }
}
