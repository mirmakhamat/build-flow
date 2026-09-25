package uz.buildflow.app.presentation.workers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class WorkerDetailViewModel(
    private val workerId: String,
    private val contextObjectId: String?,
    private val workerRepository: WorkerRepository,
    private val workerDayRepository: WorkerDayRepository,
    private val transactionRepository: TransactionRepository,
    private val objectRepository: ObjectRepository,
    private val getWorkerStatsUseCase: GetWorkerStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkerDetailUiState())
    val uiState: StateFlow<WorkerDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    // Ishchi qaysi obyekt ekranidan ochilgan bo'lsa, shu obyektga tegishli kun/to'lov/bonus
    // yozuvlarigina ko'rsatiladi va hisoblanadi - boshqa obyektdagi qarz/avans bu yerga aralashmaydi.
    private fun resolveObjectId(worker: Worker?): String? =
        contextObjectId?.takeIf { it.isNotBlank() } ?: worker?.objectId

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

        viewModelScope.launch {
            reconcileUnpaidDays()
        }

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            combine(
                combine(
                    workerRepository.getWorkerById(workerId),
                    workerDayRepository.getDaysByWorker(workerId)
                ) { worker, days -> Pair(worker, days) },
                combine(
                    transactionRepository.getPaymentsByWorker(workerId),
                    workerDayRepository.getGeneralBonusesByWorker(workerId)
                ) { payments, bonuses -> Pair(payments, bonuses) }
            ) { (worker, days), (payments, bonuses) ->
                val effObjId = resolveObjectId(worker)
                val scopedDays = if (effObjId == null) days else days.filter { (it.objectId ?: worker?.objectId) == effObjId }
                val scopedPayments = if (effObjId == null) payments else payments.filter { it.objectId == effObjId }
                val scopedBonuses = if (effObjId == null) bonuses else bonuses.filter { it.objectId == effObjId }
                Triple(worker, scopedDays, Pair(scopedPayments, scopedBonuses))
            }.flatMapLatest { (worker, days, paymentsAndBonuses) ->
                val (payments, bonuses) = paymentsAndBonuses
                getWorkerStatsUseCase(workerId, resolveObjectId(worker)).map { stats ->
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
                }
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
        note: String?,
        targetDate: String? = null
    ) {
        viewModelScope.launch {
            val date = targetDate ?: _uiState.value.selectedDate ?: return@launch
            val worker = _uiState.value.worker ?: return@launch
            val existingDays = workerDayRepository.getDaysByWorker(workerId).firstOrNull() ?: emptyList()
            val existing = existingDays.find { it.date == date }

            if (existing != null) {
                val updated = existing.copy(
                    status = status,
                    paymentAmount = paymentAmount,
                    note = note,
                    updatedAt = System.currentTimeMillis()
                )
                workerDayRepository.saveWorkerDay(updated)
            } else {
                val newRecord = WorkerDay(
                    workerId = workerId,
                    objectId = resolveObjectId(worker) ?: worker.objectId,
                    date = date,
                    status = status,
                    paymentAmount = paymentAmount,
                    paymentStatus = PaymentStatus.UNPAID,
                    note = note
                )
                workerDayRepository.saveWorkerDay(newRecord)
            }

            reconcileUnpaidDays()
            closeDayEditSheet()
        }
    }

    fun deleteDayRecord(record: WorkerDay) {
        viewModelScope.launch {
            workerDayRepository.deleteWorkerDay(record)
            val worker = _uiState.value.worker
            val recordObjectId = record.objectId ?: worker?.objectId
            val existingPayments = transactionRepository.getPaymentsByWorker(workerId).firstOrNull() ?: emptyList()
            val paymentToDelete = existingPayments.find {
                it.date == record.date && it.type == PaymentType.SALARY && it.objectId == recordObjectId
            }
            if (paymentToDelete != null) {
                transactionRepository.deleteWorkerPayment(paymentToDelete)
            }
            reconcileUnpaidDays()
            closeDayEditSheet()
        }
    }

    // BONUSLAR
    fun openAddBonus(date: String? = null) {
        val targetDate = date ?: _uiState.value.selectedDate ?: DateUtil.today()
        val worker = _uiState.value.worker
        val templateBonus = GeneralBonus(workerId = workerId, objectId = resolveObjectId(worker) ?: worker?.objectId ?: "", amount = 0.0, date = targetDate)
        _uiState.update { it.copy(selectedBonus = templateBonus, isBonusSheetOpen = true) }
    }

    fun openEditBonus(bonus: GeneralBonus) {
        _uiState.update { it.copy(selectedBonus = bonus, isBonusSheetOpen = true) }
    }

    fun closeBonusSheet() {
        _uiState.update { it.copy(isBonusSheetOpen = false, selectedBonus = null) }
    }

    fun saveGeneralBonus(amount: Double, date: String, reason: String?) {
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
                    objectId = resolveObjectId(worker) ?: worker.objectId,
                    amount = amount,
                    date = date,
                    reason = reason
                )
                workerDayRepository.insertGeneralBonus(bonus)
            }

            reconcileUnpaidDays()
            closeBonusSheet()
        }
    }

    fun deleteGeneralBonus(bonus: GeneralBonus) {
        viewModelScope.launch {
            workerDayRepository.deleteGeneralBonus(bonus)
            reconcileUnpaidDays()
            closeBonusSheet()
        }
    }

    // TO'LOVLAR (WorkerPayment)
    fun openAddPayment(date: String? = null) {
        val targetDate = date ?: _uiState.value.selectedDate ?: DateUtil.today()
        val worker = _uiState.value.worker
        val templatePayment = WorkerPayment(workerId = workerId, objectId = resolveObjectId(worker) ?: worker?.objectId ?: "", amount = 0.0, date = targetDate)
        _uiState.update { it.copy(selectedPayment = templatePayment, isPaymentSheetOpen = true) }
    }

    fun openEditPayment(payment: WorkerPayment) {
        _uiState.update { it.copy(selectedPayment = payment, isPaymentSheetOpen = true) }
    }

    fun closePaymentSheet() {
        _uiState.update { it.copy(isPaymentSheetOpen = false, selectedPayment = null) }
    }

    fun savePayment(amount: Double, date: String, type: PaymentType, description: String?, isPaid: Boolean = true, payerObjectId: String? = null, paymentDate: String? = null) {
        viewModelScope.launch {
            val worker = _uiState.value.worker ?: return@launch
            val existing = _uiState.value.selectedPayment

            if (isPaid && amount > 0) {
                if (existing != null && existing.amount > 0) {
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
                        objectId = resolveObjectId(worker) ?: worker.objectId,
                        payerObjectId = payerObjectId,
                        amount = amount,
                        date = date.ifBlank { DateUtil.today() },
                        paymentDate = paymentDate,
                        type = type,
                        description = description
                    )
                    transactionRepository.insertWorkerPayment(payment)
                }
            } else if (!isPaid && existing != null) {
                transactionRepository.deleteWorkerPayment(existing)
            }
            reconcileUnpaidDays()
            closePaymentSheet()
        }
    }

    fun deletePayment(payment: WorkerPayment) {
        viewModelScope.launch {
            transactionRepository.deleteWorkerPayment(payment)
            reconcileUnpaidDays()
            closePaymentSheet()
        }
    }

    private suspend fun reconcileUnpaidDays() {
        val worker = workerRepository.getWorkerById(workerId).firstOrNull()
        val effObjId = resolveObjectId(worker)

        val payments = transactionRepository.getPaymentsByWorker(workerId).firstOrNull() ?: emptyList()
        val scopedPayments = if (effObjId == null) payments else payments.filter { it.objectId == effObjId }
        val totalPaid = scopedPayments.sumOf { it.amount }

        val days = workerDayRepository.getDaysByWorker(workerId).firstOrNull()
            ?.filter { it.status != AttendanceStatus.ABSENT && it.paymentAmount > 0 }
            ?.let { list ->
                if (effObjId == null) list else list.filter { (it.objectId ?: worker?.objectId) == effObjId }
            }
            ?.sortedBy { it.date } ?: emptyList()

        var budget = totalPaid
        for (day in days) {
            if (budget >= day.paymentAmount) {
                if (day.paymentStatus != PaymentStatus.PAID) {
                    workerDayRepository.saveWorkerDay(
                        day.copy(paymentStatus = PaymentStatus.PAID, updatedAt = System.currentTimeMillis())
                    )
                }
                budget -= day.paymentAmount
            } else {
                break
            }
        }
    }

    companion object {
        fun provideFactory(
            workerId: String,
            objectId: String? = null,
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
                    objectId,
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
