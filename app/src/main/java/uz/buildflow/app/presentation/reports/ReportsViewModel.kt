package uz.buildflow.app.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.buildflow.app.domain.model.*
import uz.buildflow.app.domain.repository.ExpenseRepository
import uz.buildflow.app.domain.repository.ObjectRepository
import uz.buildflow.app.domain.repository.TransactionRepository
import uz.buildflow.app.domain.repository.WorkerRepository
import uz.buildflow.app.domain.usecase.GetObjectFinancialSummaryUseCase
import uz.buildflow.app.domain.usecase.GetWorkerStatsUseCase

enum class DrillDownType {
    INCOMES,
    WORKER_PAYMENTS,
    EXTERNAL_WORKERS_PAID,
    EXTERNAL_PAID_FOR_THIS_WORKERS,
    WORKER_DEBTS,
    ALL_EXPENSES,
    CATEGORY_EXPENSES,
    CASH_OUTFLOW,
    EXTERNAL_EXPENSES_PAID,
    EXTERNAL_EXPENSES_PAID_BY_OTHERS
}

data class ReportsUiState(
    val summary: ObjectFinancialSummary? = null,
    val incomes: List<MoneyTransaction> = emptyList(),
    val expenses: List<Expense> = emptyList(),
    val workerPayments: List<WorkerPayment> = emptyList(),
    val externalWorkerPayments: List<WorkerPayment> = emptyList(),
    val externalPaidForThisWorkers: List<WorkerPayment> = emptyList(),
    val externalExpensesPaid: List<Expense> = emptyList(),
    val workerStatsList: List<WorkerStats> = emptyList(),
    val availableObjects: List<BuildObject> = emptyList(),
    val allWorkers: List<Worker> = emptyList(),
    val selectedDrillDownType: DrillDownType? = null,
    val selectedCategoryName: String? = null,
    val isLoading: Boolean = true
)

class ReportsViewModel(
    private val objectId: String,
    private val getObjectFinancialSummaryUseCase: GetObjectFinancialSummaryUseCase,
    private val transactionRepository: TransactionRepository,
    private val expenseRepository: ExpenseRepository,
    private val workerRepository: WorkerRepository,
    private val getWorkerStatsUseCase: GetWorkerStatsUseCase,
    private val objectRepository: ObjectRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadReportData()
    }

    private fun loadReportData() {
        viewModelScope.launch {
            // 1. Summary
            launch {
                getObjectFinancialSummaryUseCase(objectId).collect { sum ->
                    _uiState.update { it.copy(summary = sum, isLoading = false) }
                }
            }

            // 2. Incomes
            launch {
                transactionRepository.getTransactionsByObject(objectId).collect { list ->
                    _uiState.update { it.copy(incomes = list) }
                }
            }

            // 3. Expenses
            launch {
                expenseRepository.getExpensesByObject(objectId).collect { list ->
                    _uiState.update { it.copy(expenses = list) }
                }
            }

            // 4. Worker Payments
            launch {
                transactionRepository.getPaymentsByObject(objectId).collect { list ->
                    _uiState.update { it.copy(workerPayments = list) }
                }
            }

            // 5. External Worker Payments (bu kassa to'lagan boshqa ishchilarga)
            launch {
                transactionRepository.getPaymentsPaidForOtherObjectsWorkers(objectId).collect { list ->
                    _uiState.update { it.copy(externalWorkerPayments = list) }
                }
            }

            // 6. External Paid By Others For This Workers (boshqa kassa to'lagan bu ishchilarga)
            launch {
                transactionRepository.getPaymentsPaidByOtherObjectsForThisWorkers(objectId).collect { list ->
                    _uiState.update { it.copy(externalPaidForThisWorkers = list) }
                }
            }

            // 7. Objects & All Workers
            launch {
                objectRepository.getAllObjects().collect { list ->
                    _uiState.update { it.copy(availableObjects = list) }
                }
            }
            launch {
                workerRepository.getAllWorkers().collect { list ->
                    _uiState.update { it.copy(allWorkers = list) }
                }
            }

            // 8. Workers & Debts
            launch {
                workerRepository.getWorkersByObject(objectId).flatMapLatest { workerList ->
                    if (workerList.isEmpty()) {
                        flowOf(emptyList())
                    } else {
                        val statsFlows = workerList.map { w -> getWorkerStatsUseCase(w.id, objectId) }
                        combine(statsFlows) { array -> array.filterNotNull() }
                    }
                }.collect { stats ->
                    _uiState.update { it.copy(workerStatsList = stats) }
                }
            }
        }
    }

    fun openDrillDown(type: DrillDownType, categoryName: String? = null) {
        _uiState.update {
            it.copy(
                selectedDrillDownType = type,
                selectedCategoryName = categoryName
            )
        }
    }

    fun closeDrillDown() {
        _uiState.update {
            it.copy(
                selectedDrillDownType = null,
                selectedCategoryName = null
            )
        }
    }

    companion object {
        fun provideFactory(
            objectId: String,
            getObjectFinancialSummaryUseCase: GetObjectFinancialSummaryUseCase,
            transactionRepository: TransactionRepository,
            expenseRepository: ExpenseRepository,
            workerRepository: WorkerRepository,
            getWorkerStatsUseCase: GetWorkerStatsUseCase,
            objectRepository: ObjectRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ReportsViewModel(
                    objectId,
                    getObjectFinancialSummaryUseCase,
                    transactionRepository,
                    expenseRepository,
                    workerRepository,
                    getWorkerStatsUseCase,
                    objectRepository
                ) as T
            }
        }
    }
}
