package uz.buildflow.app.presentation.workers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.buildflow.app.domain.model.*
import uz.buildflow.app.domain.repository.TransactionRepository
import uz.buildflow.app.domain.repository.WorkerDayRepository
import uz.buildflow.app.domain.repository.WorkerRepository
import uz.buildflow.app.domain.usecase.GetWorkerStatsUseCase

data class WorkerDetailUiState(
    val worker: Worker? = null,
    val stats: WorkerStats? = null,
    val days: List<WorkerDay> = emptyList(),
    val payments: List<WorkerPayment> = emptyList(),
    val generalBonuses: List<GeneralBonus> = emptyList(),
    val selectedDate: String? = null,
    val selectedDayRecord: WorkerDay? = null,
    val selectedBonus: GeneralBonus? = null,
    val selectedPayment: WorkerPayment? = null,
    val isDayEditSheetOpen: Boolean = false,
    val isBonusSheetOpen: Boolean = false,
    val isPaymentSheetOpen: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
) {
    val selectedDatePayments: List<WorkerPayment>
        get() = if (selectedDate == null) emptyList() else payments.filter { it.date == selectedDate }

    val selectedDateBonuses: List<GeneralBonus>
        get() = if (selectedDate == null) emptyList() else generalBonuses.filter { it.date == selectedDate }
}

class WorkerDetailViewModel(
    private val workerId: String,
    private val workerRepository: WorkerRepository,
    private val workerDayRepository: WorkerDayRepository,
    private val transactionRepository: TransactionRepository,
    private val getWorkerStatsUseCase: GetWorkerStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerDetailUiState())
    val uiState: StateFlow<WorkerDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadWorkerData()
    }

    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadWorkerData()
    }

    private fun loadWorkerData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                workerRepository.getWorkerById(workerId),
                getWorkerStatsUseCase(workerId),
                workerDayRepository.getDaysByWorker(workerId),
                transactionRepository.getPaymentsByWorker(workerId),
                workerDayRepository.getGeneralBonusesByWorker(workerId)
            ) { worker, stats, days, paymentsList, generalBonuses ->
                val currentSelectedDate = _uiState.value.selectedDate
                val currentDayRec = if (currentSelectedDate != null) days.find { it.date == currentSelectedDate } else _uiState.value.selectedDayRecord

                WorkerDetailUiState(
                    worker = worker,
                    stats = stats,
                    days = days,
                    payments = paymentsList,
                    generalBonuses = generalBonuses,
                    selectedBonus = _uiState.value.selectedBonus,
                    selectedPayment = _uiState.value.selectedPayment,
                    selectedDayRecord = currentDayRec,
                    selectedDate = currentSelectedDate,
                    isDayEditSheetOpen = _uiState.value.isDayEditSheetOpen,
                    isBonusSheetOpen = _uiState.value.isBonusSheetOpen,
                    isPaymentSheetOpen = _uiState.value.isPaymentSheetOpen,
                    isLoading = false,
                    isRefreshing = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun selectDateForEdit(date: String) {
        viewModelScope.launch {
            val record = workerDayRepository.getDayByWorkerAndDate(workerId, date)
            _uiState.update {
                it.copy(
                    selectedDate = date,
                    selectedDayRecord = record,
                    isDayEditSheetOpen = true
                )
            }
        }
    }

    fun closeDayEditSheet() {
        _uiState.update { it.copy(isDayEditSheetOpen = false, selectedDate = null, selectedDayRecord = null) }
    }

    fun saveDayRecord(status: AttendanceStatus, paymentAmount: Double, isPaid: Boolean, note: String?) {
        viewModelScope.launch {
            val date = _uiState.value.selectedDate ?: return@launch
            val worker = _uiState.value.worker ?: return@launch
            val existing = _uiState.value.selectedDayRecord

            val paymentStatus = if (isPaid) PaymentStatus.PAID else PaymentStatus.UNPAID

            if (existing != null) {
                val updated = existing.copy(
                    status = status,
                    paymentAmount = paymentAmount,
                    paymentStatus = paymentStatus,
                    note = note,
                    updatedAt = System.currentTimeMillis()
                )
                workerDayRepository.saveWorkerDay(updated)
            } else {
                val newRecord = WorkerDay(
                    workerId = workerId,
                    date = date,
                    status = status,
                    paymentAmount = paymentAmount,
                    paymentStatus = paymentStatus,
                    note = note
                )
                workerDayRepository.saveWorkerDay(newRecord)
            }

            val existingPayments = transactionRepository.getPaymentsByWorker(workerId).firstOrNull() ?: emptyList()
            val existingDaySalaryPayment = existingPayments.find { it.date == date && it.type == PaymentType.SALARY }

            if (isPaid && paymentAmount > 0) {
                if (existingDaySalaryPayment != null) {
                    transactionRepository.updateWorkerPayment(existingDaySalaryPayment.copy(amount = paymentAmount))
                } else {
                    val payment = WorkerPayment(
                        workerId = workerId,
                        objectId = worker.objectId,
                        amount = paymentAmount,
                        date = date,
                        type = PaymentType.SALARY,
                        description = "${date} kunlik ish haqi to'landi"
                    )
                    transactionRepository.insertWorkerPayment(payment)
                }
            } else if (!isPaid && existingDaySalaryPayment != null) {
                transactionRepository.deleteWorkerPayment(existingDaySalaryPayment)
            }

            closeDayEditSheet()
        }
    }

    fun payForDaySalary(record: WorkerDay) {
        viewModelScope.launch {
            val worker = _uiState.value.worker ?: return@launch
            val updated = record.copy(paymentStatus = PaymentStatus.PAID, updatedAt = System.currentTimeMillis())
            workerDayRepository.saveWorkerDay(updated)

            val existingPayments = transactionRepository.getPaymentsByWorker(workerId).firstOrNull() ?: emptyList()
            val existingDaySalaryPayment = existingPayments.find { it.date == record.date && it.type == PaymentType.SALARY }

            if (existingDaySalaryPayment != null) {
                transactionRepository.updateWorkerPayment(existingDaySalaryPayment.copy(amount = record.paymentAmount))
            } else {
                val payment = WorkerPayment(
                    workerId = workerId,
                    objectId = worker.objectId,
                    amount = record.paymentAmount,
                    date = record.date,
                    type = PaymentType.SALARY,
                    description = "${record.date} kunlik ish haqi to'landi"
                )
                transactionRepository.insertWorkerPayment(payment)
            }
        }
    }

    fun markDayAsUnpaid(record: WorkerDay) {
        viewModelScope.launch {
            val updated = record.copy(paymentStatus = PaymentStatus.UNPAID, updatedAt = System.currentTimeMillis())
            workerDayRepository.saveWorkerDay(updated)

            val existingPayments = transactionRepository.getPaymentsByWorker(workerId).firstOrNull() ?: emptyList()
            val existingDaySalaryPayment = existingPayments.find { it.date == record.date && it.type == PaymentType.SALARY }
            if (existingDaySalaryPayment != null) {
                transactionRepository.deleteWorkerPayment(existingDaySalaryPayment)
            }
        }
    }

    fun deleteDayRecord(record: WorkerDay) {
        viewModelScope.launch {
            workerDayRepository.deleteWorkerDay(record)
            val existingPayments = transactionRepository.getPaymentsByWorker(workerId).firstOrNull() ?: emptyList()
            val paymentToDelete = existingPayments.find { it.date == record.date && it.type == PaymentType.SALARY }
            if (paymentToDelete != null) {
                transactionRepository.deleteWorkerPayment(paymentToDelete)
            }
            closeDayEditSheet()
        }
    }

    // BONUSLAR
    fun openAddBonus(date: String? = null) {
        val targetDate = date ?: _uiState.value.selectedDate ?: uz.buildflow.app.core.util.DateUtil.today()
        val templateBonus = GeneralBonus(workerId = workerId, objectId = _uiState.value.worker?.objectId ?: "", amount = 0.0, date = targetDate)
        _uiState.update { it.copy(selectedBonus = templateBonus, isBonusSheetOpen = true) }
    }

    fun openEditBonus(bonus: GeneralBonus) {
        _uiState.update { it.copy(selectedBonus = bonus, isBonusSheetOpen = true) }
    }

    fun closeBonusSheet() {
        _uiState.update { it.copy(isBonusSheetOpen = false, selectedBonus = null) }
    }

    fun saveGeneralBonus(amount: Double, date: String, reason: String?, isPaid: Boolean) {
        viewModelScope.launch {
            val worker = _uiState.value.worker ?: return@launch
            val existing = _uiState.value.selectedBonus

            if (existing != null && existing.amount > 0) {
                val updated = existing.copy(
                    amount = amount,
                    date = date,
                    reason = reason
                )
                workerDayRepository.updateGeneralBonus(updated)
            } else {
                val bonus = GeneralBonus(
                    workerId = workerId,
                    objectId = worker.objectId,
                    amount = amount,
                    date = date,
                    reason = reason
                )
                workerDayRepository.insertGeneralBonus(bonus)
            }

            val existingPayments = transactionRepository.getPaymentsByWorker(workerId).firstOrNull() ?: emptyList()
            val existingBonusPayment = existingPayments.find { it.date == date && it.type == PaymentType.BONUS_PAYOUT }

            if (isPaid && amount > 0) {
                if (existingBonusPayment != null) {
                    transactionRepository.updateWorkerPayment(existingBonusPayment.copy(amount = amount, description = reason ?: "Bonus to'landi"))
                } else {
                    val payment = WorkerPayment(
                        workerId = workerId,
                        objectId = worker.objectId,
                        amount = amount,
                        date = date,
                        type = PaymentType.BONUS_PAYOUT,
                        description = "${date} ${reason ?: "Bonus"} to'landi"
                    )
                    transactionRepository.insertWorkerPayment(payment)
                }
            } else if (!isPaid && existingBonusPayment != null) {
                transactionRepository.deleteWorkerPayment(existingBonusPayment)
            }

            closeBonusSheet()
        }
    }

    fun payForBonus(bonus: GeneralBonus) {
        viewModelScope.launch {
            val worker = _uiState.value.worker ?: return@launch
            val existingPayments = transactionRepository.getPaymentsByWorker(workerId).firstOrNull() ?: emptyList()
            val existingBonusPayment = existingPayments.find { it.date == bonus.date && it.type == PaymentType.BONUS_PAYOUT }

            if (existingBonusPayment != null) {
                transactionRepository.updateWorkerPayment(existingBonusPayment.copy(amount = bonus.amount))
            } else {
                val payment = WorkerPayment(
                    workerId = workerId,
                    objectId = worker.objectId,
                    amount = bonus.amount,
                    date = bonus.date,
                    type = PaymentType.BONUS_PAYOUT,
                    description = "${bonus.date} ${bonus.reason ?: "Bonus"} to'landi"
                )
                transactionRepository.insertWorkerPayment(payment)
            }
        }
    }

    fun markBonusAsUnpaid(bonus: GeneralBonus) {
        viewModelScope.launch {
            val existingPayments = transactionRepository.getPaymentsByWorker(workerId).firstOrNull() ?: emptyList()
            val existingBonusPayment = existingPayments.find { it.date == bonus.date && it.type == PaymentType.BONUS_PAYOUT }
            if (existingBonusPayment != null) {
                transactionRepository.deleteWorkerPayment(existingBonusPayment)
            }
        }
    }

    fun deleteGeneralBonus(bonus: GeneralBonus) {
        viewModelScope.launch {
            workerDayRepository.deleteGeneralBonus(bonus)
            val existingPayments = transactionRepository.getPaymentsByWorker(workerId).firstOrNull() ?: emptyList()
            val paymentToDelete = existingPayments.find { it.date == bonus.date && it.type == PaymentType.BONUS_PAYOUT }
            if (paymentToDelete != null) {
                transactionRepository.deleteWorkerPayment(paymentToDelete)
            }
            closeBonusSheet()
        }
    }

    // TO'LOVLAR & AVANSLAR
    fun openAddPayment(date: String? = null) {
        val targetDate = date ?: _uiState.value.selectedDate ?: uz.buildflow.app.core.util.DateUtil.today()
        val templatePayment = WorkerPayment(
            workerId = workerId,
            objectId = _uiState.value.worker?.objectId ?: "",
            amount = 0.0,
            date = targetDate,
            type = PaymentType.ADVANCE
        )
        _uiState.update { it.copy(selectedPayment = templatePayment, isPaymentSheetOpen = true) }
    }

    fun openEditPayment(payment: WorkerPayment) {
        _uiState.update { it.copy(selectedPayment = payment, isPaymentSheetOpen = true) }
    }

    fun closePaymentSheet() {
        _uiState.update { it.copy(isPaymentSheetOpen = false, selectedPayment = null) }
    }

    fun savePayment(amount: Double, date: String, type: PaymentType, description: String?, isPaid: Boolean) {
        viewModelScope.launch {
            val worker = _uiState.value.worker ?: return@launch
            val existing = _uiState.value.selectedPayment

            if (isPaid && amount > 0) {
                if (existing != null && existing.amount > 0) {
                    val updated = existing.copy(
                        amount = amount,
                        date = date,
                        type = type,
                        description = description
                    )
                    transactionRepository.updateWorkerPayment(updated)
                } else {
                    val payment = WorkerPayment(
                        workerId = workerId,
                        objectId = worker.objectId,
                        amount = amount,
                        date = date,
                        type = type,
                        description = description
                    )
                    transactionRepository.insertWorkerPayment(payment)
                }
            } else if (!isPaid && existing != null) {
                // Agar qarzga yozilsa va oldin to'lov bo'lgan bo'lsa, to'lov o'chiriladi
                transactionRepository.deleteWorkerPayment(existing)
            }
            closePaymentSheet()
        }
    }

    fun deletePayment(payment: WorkerPayment) {
        viewModelScope.launch {
            transactionRepository.deleteWorkerPayment(payment)
            closePaymentSheet()
        }
    }

    companion object {
        fun provideFactory(
            workerId: String,
            workerRepository: WorkerRepository,
            workerDayRepository: WorkerDayRepository,
            transactionRepository: TransactionRepository,
            getWorkerStatsUseCase: GetWorkerStatsUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WorkerDetailViewModel(
                    workerId,
                    workerRepository,
                    workerDayRepository,
                    transactionRepository,
                    getWorkerStatsUseCase
                ) as T
            }
        }
    }
}
