package uz.buildflow.app.presentation.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.BuildObject
import uz.buildflow.app.domain.model.Expense
import uz.buildflow.app.domain.model.ExpenseCategoryItem
import uz.buildflow.app.domain.repository.ExpenseCategoryRepository
import uz.buildflow.app.domain.repository.ExpenseRepository
import uz.buildflow.app.domain.repository.ObjectRepository
import uz.buildflow.app.domain.repository.TransactionRepository

data class ExpensesUiState(
    val expenses: List<Expense> = emptyList(),
    val categories: List<ExpenseCategoryItem> = emptyList(),
    val availableObjects: List<BuildObject> = emptyList(),
    val totalExpense: Double = 0.0,
    val totalBuildingExpense: Double = 0.0,
    val totalTransfersOut: Double = 0.0,
    val selectedObjectId: String? = null,
    val selectedExpense: Expense? = null,
    val isAddSheetOpen: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)

class ExpensesViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: ExpenseCategoryRepository,
    private val objectRepository: ObjectRepository,
    private val transactionRepository: TransactionRepository,
    initialObjectId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpensesUiState(selectedObjectId = initialObjectId))
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadData(initialObjectId)
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadData(_uiState.value.selectedObjectId)
    }

    fun setObjectId(objectId: String?) {
        _uiState.update { it.copy(selectedObjectId = objectId) }
        loadData(objectId)
    }

    private fun loadData(objectId: String?) {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { catList ->
                _uiState.update { it.copy(categories = catList) }
            }
        }

        viewModelScope.launch {
            objectRepository.getAllObjects().collect { objList ->
                _uiState.update { it.copy(availableObjects = objList) }
            }
        }

        if (objectId != null) {
            loadJob?.cancel()
            loadJob = viewModelScope.launch {
                combine(
                    expenseRepository.getExpensesByObject(objectId),
                    expenseRepository.getTotalExpenseByObject(objectId),
                    expenseRepository.getTotalBuildingExpenseByObject(objectId),
                    expenseRepository.getTotalTransfersOutByObject(objectId)
                ) { list, total, buildingExp, transfersOut ->
                    _uiState.value.copy(
                        expenses = list,
                        totalExpense = total,
                        totalBuildingExpense = buildingExp,
                        totalTransfersOut = transfersOut,
                        selectedObjectId = objectId,
                        isLoading = false,
                        isRefreshing = false
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            }
        }
    }

    fun openAddExpense() {
        _uiState.update { it.copy(selectedExpense = null, isAddSheetOpen = true) }
    }

    fun openEditExpense(expense: Expense) {
        _uiState.update { it.copy(selectedExpense = expense, isAddSheetOpen = true) }
    }

    fun closeAddExpense() {
        _uiState.update { it.copy(isAddSheetOpen = false, selectedExpense = null) }
    }

    fun addNewCategory(name: String) {
        viewModelScope.launch {
            categoryRepository.insertCategory(name)
        }
    }

    fun saveExpense(
        category: String,
        amount: Double,
        date: String,
        description: String?,
        workerId: String?,
        payerObjectId: String? = null
    ) {
        viewModelScope.launch {
            val objectId = _uiState.value.selectedObjectId ?: return@launch
            val existing = _uiState.value.selectedExpense

            if (existing != null) {
                val updated = existing.copy(
                    category = category,
                    amount = amount,
                    date = date.ifBlank { DateUtil.today() },
                    description = description,
                    workerId = workerId,
                    payerObjectId = payerObjectId,
                    updatedAt = System.currentTimeMillis()
                )
                expenseRepository.updateExpense(updated)
            } else {
                val expense = Expense(
                    objectId = objectId,
                    payerObjectId = payerObjectId,
                    workerId = workerId,
                    category = category,
                    amount = amount,
                    date = date.ifBlank { DateUtil.today() },
                    description = description
                )
                expenseRepository.insertExpense(expense)
            }
            closeAddExpense()
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
            // Bu "Kassalararo o'tkazma" xarajati bo'lsa, unga bog'langan (qabul qiluvchi
            // obyektdagi) kirim yozuvini ham o'chiramiz - aks holda u yerda "hech qayerdan
            // kelmagan" kirim osilib qolaveradi.
            if (expense.category == "Kassalararo o'tkazma" && expense.id.startsWith("exp_tr_")) {
                val linkedTransactionId = expense.id.removePrefix("exp_tr_")
                transactionRepository.deleteTransactionById(linkedTransactionId)
            }
            closeAddExpense()
        }
    }

    companion object {
        fun provideFactory(
            expenseRepository: ExpenseRepository,
            categoryRepository: ExpenseCategoryRepository,
            objectRepository: ObjectRepository,
            transactionRepository: TransactionRepository,
            initialObjectId: String? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ExpensesViewModel(expenseRepository, categoryRepository, objectRepository, transactionRepository, initialObjectId) as T
            }
        }
    }
}
