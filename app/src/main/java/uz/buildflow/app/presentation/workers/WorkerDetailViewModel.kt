package uz.buildflow.app.presentation.workers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.buildflow.app.core.util.DateUtil
import uz.buildflow.app.domain.model.*
import uz.buildflow.app.domain.repository.ObjectRepository
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
    val availableObjects: List<BuildObject> = emptyList(),
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
    private val objectRepository: ObjectRepository,
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
        viewModelScope.launch {
            objectRepository.getAllObjects().collect { objList ->
                _uiState.update { it.copy(availableObjects = objList) }
            }
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                combine(
                    workerRepository.getWorkerById(workerId),
                    getWorkerStatsUseCase(workerId),
                    workerDayRepository.getDaysByWorker(workerId)
                ) { worker, stats, days -> Triple(worker, stats, days) },
                combine(
                    transactionRepository.getPaymentsByWorker(workerId),
                    workerDayRepository.getGeneralBonusesByWorker(workerId)
                ) { payments, bonuses -> Pair(payments, bonuses) }
            ) { (worker, stats, days), (payments, bonuses) ->
                WorkerDetailUiState(
                    worker = worker,
                    stats = stats,
                    days = days,
                    payments = payments,
                    generalBonuses = bonuses,
                    availableObjects = _uiState.value.availableObjects,
                    selectedDate = _uiState.value.selectedDate,
                    selectedDayRecord = _uiState.value.selectedDayRecord,
                    selectedBonus = _uiState.value.selectedBonus,
                    selectedPayment = _uiState.value.selectedPayment,
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
        val existingRecord = _uiState.value.days.find { it.date == date }
        _uiState.update {
            it.copy(
                selectedDate = date,
                selectedDayRecord = existingRecord,
                isDayEditSheetOpen = true
            )
        }
    }

    fun closeDayEditSheet() {
        _uiState.update {
            it.copy(
                isDayEditSheetOpen = false,
                selectedDate = null,
                selectedDayRecord = null
            )
        }
    }

    fun saveDayRecord(
        status: AttendanceStatus,
        paymentAmount: Double,
        isPaid: Boolean,
        note: String?,
        payerObjectId: String? = null
    ) {
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
                    transactionRepository.updateWorkerPayment(
                        existingDaySalaryPayment.copy(
                            amount = paymentAmount,
                            payerObjectId = payerObjectId,
                            description = "${DateUtil.formatToDisplay(date)} kunlik ish haqi to'landi"
                        )
                    )
                } else {
                    val payment = WorkerPayment(
                        workerId = workerId,
                        objectId = worker.objectId,
                        payerObjectId = payerObjectId,
                        amount = paymentAmount,
                        date = date,
                        type = PaymentType.SALARY,
                        description = "${DateUtil.formatToDisplay(date)} kunlik ish haqi to'landi"
                    )
                    transactionRepository.insertWorkerPayment(payment)
                }
            } else if (!isPaid && existingDaySalaryPayment != null) {
                transactionRepository.deleteWorkerPayment(existingDaySalaryPayment)
            }

            closeDayEditSheet()
        }
    }

    fun payForDaySalary(record: WorkerDay, payerObjectId: String? = null) {
        viewModelScope.launch {
            val worker = _uiState.value.worker ?: return@launch
            val updated = record.copy(paymentStatus = PaymentStatus.PAID, updatedAt = System.currentTimeMillis())
            workerDayRepository.saveWorkerDay(updated)

            val existingPayments = transactionRepository.getPaymentsByWorker(workerId).firstOrNull() ?: emptyList()
            val existingDaySalaryPayment = existingPayments.find { it.date == record.date && it.type == PaymentType.SALARY }

            if (existingDaySalaryPayment != null) {
                transactionRepository.updateWorkerPayment(
                    existingDaySalaryPayment.copy(
                        amount = record.paymentAmount,
                        payerObjectId = payerObjectId,
                        description = "${DateUtil.formatToDisplay(record.date)} kunlik ish haqi to'landi"
                    )
                )
            } else {
                val payment = WorkerPayment(
                    workerId = workerId,
                    objectId = worker.objectId,
                    payerObjectId = payerObjectId,
                    amount = record.paymentAmount,
                    date = record.date,
                    type = PaymentType.SALARY,
                    description = "${DateUtil.formatToDisplay(record.date)} kunlik ish haqi to'landi"
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
        val targetDate = date ?: _uiState.value.selectedDate ?: DateUtil.today()
        val templateBonus = GeneralBonus(workerId = workerId, objectId = _uiState.value.worker?.objectId ?: "", amount = 0.0, date = targetDate)
        _uiState.update { it.copy(selectedBonus = templateBonus, isBonusSheetOpen = true) }
    }

    fun openEditBonus(bonus: GeneralBonus) {
        _uiState.update { it.copy(selectedBonus = bonus, isBonusSheetOpen = true) }
    }

    fun closeBonusSheet() {
        _uiState.update { it.copy(isBonusSheetOpen = false, selectedBonus = null) }
    }

    fun saveGeneralBonus(amount: Double, date: String, reason: String?, isPaid: Boolean, payerObjectId: String? = null) {
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
                val desc = reason?.ifBlank { null } ?: "${DateUtil.formatToDisplay(date)} bonusi to'landi"
                if (existingBonusPayment != null) {
                    transactionRepository.updateWorkerPayment(
                        existingBonusPayment.copy(
                            amount = amount,
                            payerObjectId = payerObjectId,
                            description = desc
                        )
                    )
                } else {
                    val payment = WorkerPayment(
                        workerId = workerId,
                        objectId = worker.objectId,
                        payerObjectId = payerObjectId,
                        amount = amount,
                        date = date,
                        type = PaymentType.BONUS_PAYOUT,
                        description = desc
                    )
                    transactionRepository.insertWorkerPayment(payment)
                }
            } else if (!isPaid && existingBonusPayment != null) {
                transactionRepository.deleteWorkerPayment(existingBonusPayment)
            }

            closeBonusSheet()
        }
    }

    fun payForBonus(bonus: GeneralBonus, payerObjectId: String? = null) {
        viewModelScope.launch {
            val worker = _uiState.value.worker ?: return@launch
            val existingPayments = transactionRepository.getPaymentsByWorker(workerId).firstOrNull() ?: emptyList()
            val existingBonusPayment = existingPayments.find { it.date == bonus.date && it.type == PaymentType.BONUS_PAYOUT }

            val desc = bonus.reason?.ifBlank { null } ?: "${DateUtil.formatToDisplay(bonus.date)} bonusi to'landi"
            if (existingBonusPayment != null) {
                transactionRepository.updateWorkerPayment(
                    existingBonusPayment.copy(
                        amount = bonus.amount,
                        payerObjectId = payerObjectId,
                        description = desc
                    )
                )
            } else {
                val payment = WorkerPayment(
                    workerId = workerId,
                    objectId = worker.objectId,
                    payerObjectId = payerObjectId,
                    amount = bonus.amount,
                    date = bonus.date,
                    type = PaymentType.BONUS_PAYOUT,
                    description = desc
                )
                transactionRepository.insertWorkerPayment(payment)
            }
        }
    }

    fun markBonusUnpaid(bonus: GeneralBonus) {
        viewModelScope.launch {
            val existingPayments = transactionRepository.getPaymentsByWorker(workerId).firstOrNull() ?: emptyList()
            val bonusPayment = existingPayments.find { it.date == bonus.date && it.type == PaymentType.BONUS_PAYOUT }
            if (bonusPayment != null) {
                transactionRepository.deleteWorkerPayment(bonusPayment)
            }
        }
    }

    fun markBonusAsUnpaid(bonus: GeneralBonus) {
        markBonusUnpaid(bonus)
    }

    fun deleteGeneralBonus(bonus: GeneralBonus) {
        viewModelScope.launch {
            workerDayRepository.deleteGeneralBonus(bonus)
            val existingPayments = transactionRepository.getPaymentsByWorker(workerId).firstOrNull() ?: emptyList()
            val bonusPayment = existingPayments.find { it.date == bonus.date && it.type == PaymentType.BONUS_PAYOUT }
            if (bonusPayment != null) {
                transactionRepository.deleteWorkerPayment(bonusPayment)
            }
            closeBonusSheet()
        }
    }

    // TO'LOVLAR (WorkerPayment)
    fun openAddPayment(date: String? = null) {
        val targetDate = date ?: _uiState.value.selectedDate ?: DateUtil.today()
        val templatePayment = WorkerPayment(workerId = workerId, objectId = _uiState.value.worker?.objectId ?: "", amount = 0.0, date = targetDate)
        _uiState.update { it.copy(selectedPayment = templatePayment, isPaymentSheetOpen = true) }
    }

    fun openEditPayment(payment: WorkerPayment) {
        _uiState.update { it.copy(selectedPayment = payment, isPaymentSheetOpen = true) }
    }

    fun closePaymentSheet() {
        _uiState.update { it.copy(isPaymentSheetOpen = false, selectedPayment = null) }
    }

    fun savePayment(amount: Double, date: String, type: PaymentType, description: String?, isPaid: Boolean = true, payerObjectId: String? = null) {
        viewModelScope.launch {
            val worker = _uiState.value.worker ?: return@launch
            val existing = _uiState.value.selectedPayment

            if (isPaid && amount > 0) {
                if (existing != null && existing.amount > 0) {
                    val updated = existing.copy(
                        amount = amount,
                        date = date.ifBlank { DateUtil.today() },
                        type = type,
                        payerObjectId = payerObjectId,
                        description = description
                    )
                    transactionRepository.updateWorkerPayment(updated)
                } else {
                    val payment = WorkerPayment(
                        workerId = workerId,
                        objectId = worker.objectId,
                        payerObjectId = payerObjectId,
                        amount = amount,
                        date = date.ifBlank { DateUtil.today() },
                        type = type,
                        description = description
                    )
                    transactionRepository.insertWorkerPayment(payment)
                }
            } else if (!isPaid && existing != null) {
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
            objectRepository: ObjectRepository,
            getWorkerStatsUseCase: GetWorkerStatsUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WorkerDetailViewModel(
                    workerId,
                    workerRepository,
                    workerDayRepository,
                    transactionRepository,
                    objectRepository,
                    getWorkerStatsUseCase
                ) as T
            }
        }
    }
}
