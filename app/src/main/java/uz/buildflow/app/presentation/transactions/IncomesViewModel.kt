package uz.buildflow.app.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.BuildObject
import uz.buildflow.app.domain.model.Expense
import uz.buildflow.app.domain.model.MoneyTransaction
import uz.buildflow.app.domain.model.TransactionType
import uz.buildflow.app.domain.repository.ExpenseRepository
import uz.buildflow.app.domain.repository.ObjectRepository
import uz.buildflow.app.domain.repository.TransactionRepository

data class IncomesUiState(
    val transactions: List<MoneyTransaction> = emptyList(),
    val currentObject: BuildObject? = null,
    val availableObjects: List<BuildObject> = emptyList(),
    val totalIncome: Double = 0.0,
    val selectedObjectId: String? = null,
    val selectedTransaction: MoneyTransaction? = null,
    val isAddSheetOpen: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
) {
    val totalPrice: Double
        get() = currentObject?.totalPrice ?: 0.0

    val remainingReceivable: Double
        get() = (totalPrice - totalIncome).coerceAtLeast(0.0)
}

class IncomesViewModel(
    private val transactionRepository: TransactionRepository,
    private val expenseRepository: ExpenseRepository,
    private val objectRepository: ObjectRepository,
    initialObjectId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(IncomesUiState(selectedObjectId = initialObjectId))
    val uiState: StateFlow<IncomesUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadIncomes(initialObjectId)
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadIncomes(_uiState.value.selectedObjectId)
    }

    fun setObjectId(objectId: String?) {
        _uiState.update { it.copy(selectedObjectId = objectId) }
        loadIncomes(objectId)
    }

    private fun loadIncomes(objectId: String?) {
        if (objectId == null) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                transactionRepository.getTransactionsByObject(objectId),
                transactionRepository.getTotalIncomeByObject(objectId),
                objectRepository.getObjectById(objectId),
                objectRepository.getAllObjects()
            ) { list, total, obj, allObjs ->
                IncomesUiState(
                    transactions = list,
                    totalIncome = total,
                    currentObject = obj,
                    availableObjects = allObjs,
                    selectedObjectId = objectId,
                    selectedTransaction = _uiState.value.selectedTransaction,
                    isAddSheetOpen = _uiState.value.isAddSheetOpen,
                    isLoading = false,
                    isRefreshing = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun openAddIncome() {
        _uiState.update { it.copy(selectedTransaction = null, isAddSheetOpen = true) }
    }

    fun openEditIncome(tx: MoneyTransaction) {
        _uiState.update { it.copy(selectedTransaction = tx, isAddSheetOpen = true) }
    }

    fun closeAddIncome() {
        _uiState.update { it.copy(isAddSheetOpen = false, selectedTransaction = null) }
    }

    fun saveIncome(amount: Double, date: String, description: String?, sourceObjectId: String? = null) {
        viewModelScope.launch {
            val objectId = _uiState.value.selectedObjectId ?: return@launch
            val existing = _uiState.value.selectedTransaction
            val allObjs = _uiState.value.availableObjects
            val currentObj = allObjs.find { it.id == objectId }

            if (existing != null) {
                val updated = existing.copy(
                    amount = amount,
                    date = date.ifBlank { DateUtil.today() },
                    description = description
                )
                transactionRepository.updateTransaction(updated)

                // Agar bu o'tkazma bo'lsa, bog'langan xarajatni ham yangilaymiz
                val linkedExpenseId = "exp_tr_${existing.id}"
                if (sourceObjectId != null) {
                    val sourceObj = allObjs.find { it.id == sourceObjectId }
                    val linkedExp = Expense(
                        id = linkedExpenseId,
                        objectId = sourceObjectId,
                        payerObjectId = sourceObjectId,
                        category = "Kassalararo o'tkazma",
                        amount = amount,
                        date = date.ifBlank { DateUtil.today() },
                        description = "[${currentObj?.name ?: "Boshqa obyekt"} kassasiga o'tkazma] ${description ?: ""}".trim()
                    )
                    expenseRepository.insertExpense(linkedExp)
                }
            } else {
                val finalDesc = if (sourceObjectId != null) {
                    val sourceObj = allObjs.find { it.id == sourceObjectId }
                    "[${sourceObj?.name ?: "Boshqa obyekt"} kassasidan o'tkazma] ${description ?: ""}".trim()
                } else {
                    description
                }

                val tx = MoneyTransaction(
                    objectId = objectId,
                    type = TransactionType.INCOME,
                    amount = amount,
                    date = date.ifBlank { DateUtil.today() },
                    description = finalDesc
                )
                transactionRepository.insertTransaction(tx)

                // Chiqim qiluvchi obyektda xarajat yaratamiz
                if (sourceObjectId != null) {
                    val linkedExp = Expense(
                        id = "exp_tr_${tx.id}",
                        objectId = sourceObjectId,
                        payerObjectId = sourceObjectId,
                        category = "Kassalararo o'tkazma",
                        amount = amount,
                        date = date.ifBlank { DateUtil.today() },
                        description = "[${currentObj?.name ?: "Boshqa obyekt"} kassasiga o'tkazma] ${description ?: ""}".trim()
                    )
                    expenseRepository.insertExpense(linkedExp)
                }
            }
            closeAddIncome()
        }
    }

    fun deleteTransaction(tx: MoneyTransaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(tx)
            // Bog'langan xarajatni ham o'chiramiz
            expenseRepository.deleteExpenseById("exp_tr_${tx.id}")
            closeAddIncome()
        }
    }

    companion object {
        fun provideFactory(
            transactionRepository: TransactionRepository,
            expenseRepository: ExpenseRepository,
            objectRepository: ObjectRepository,
            initialObjectId: String? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return IncomesViewModel(transactionRepository, expenseRepository, objectRepository, initialObjectId) as T
            }
        }
    }
}
