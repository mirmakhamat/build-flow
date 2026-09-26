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
import uz.buildflow.app.domain.usecase.TransferWorkerUseCase

data class WorkerWithStats(
    val worker: Worker,
    val stats: WorkerStats?
)

data class ImportableWorkerItem(
    val worker: Worker,
    val sourceObjectName: String
)

data class WorkersUiState(
    val workers: List<WorkerWithStats> = emptyList(),
    val allPayments: List<WorkerPayment> = emptyList(),
    val availableObjects: List<BuildObject> = emptyList(),
    val selectedObjectId: String? = null,
    val isAddEditSheetOpen: Boolean = false,
    val selectedWorker: Worker? = null,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    // Boshqa obyektdan ishchi olib kelish (Import) holatlari
    val isImportSheetOpen: Boolean = false,
    val importableWorkers: List<ImportableWorkerItem> = emptyList(),
    // Ommaviy ish haqi to'lash (Bulk payout) holatlari
    val isBulkPayoutSheetOpen: Boolean = false,
    // Barcha to'lovlar tarixi Sheet
    val isAllPaymentsSheetOpen: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class WorkersViewModel(
    private val workerRepository: WorkerRepository,
    private val objectRepository: ObjectRepository,
    private val transactionRepository: TransactionRepository,
    private val workerDayRepository: WorkerDayRepository,
    private val getWorkerStatsUseCase: GetWorkerStatsUseCase,
    private val transferWorkerUseCase: TransferWorkerUseCase,
    initialObjectId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkersUiState(selectedObjectId = initialObjectId))
    val uiState: StateFlow<WorkersUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var paymentsJob: Job? = null

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
                        getWorkerStatsUseCase(w.id, objectId).map { stats ->
                            WorkerWithStats(w, stats)
                        }
                    }
                    combine(statsFlows) { it.toList() }
                }
            }.collect { list ->
                _uiState.update {
                    it.copy(
                        // Nofaol (boshqa obyektga o'tgan) ishchilar ro'yxat oxirida
                        workers = list.sortedBy { item -> item.worker.status != uz.buildflow.app.domain.model.WorkerStatus.ACTIVE },
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }
        }

        paymentsJob?.cancel()
        paymentsJob = viewModelScope.launch {
            transactionRepository.getPaymentsByObject(objectId).collect { paymentsList ->
                _uiState.update { it.copy(allPayments = paymentsList) }
            }
        }
    }

    // BARCHA TO'LOVLAR TARIXI (ALL PAYMENTS)
    fun openAllPaymentsSheet() {
        _uiState.update { it.copy(isAllPaymentsSheetOpen = true) }
    }

    fun closeAllPaymentsSheet() {
        _uiState.update { it.copy(isAllPaymentsSheetOpen = false) }
    }

    // OMMAVIY TO'LOV (BULK PAYOUT)
    fun openBulkPayoutSheet() {
        if (_uiState.value.workers.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "To'lov qilish uchun ushbu obyektda ishchilar mavjud emas!") }
            return
        }
        _uiState.update { it.copy(isBulkPayoutSheetOpen = true) }
    }

    fun closeBulkPayoutSheet() {
        _uiState.update { it.copy(isBulkPayoutSheetOpen = false) }
    }

    fun executeBulkPayout(payouts: Map<String, Double>, paymentDate: String, payerObjectId: String?) {
        viewModelScope.launch {
            val targetObjectId = _uiState.value.selectedObjectId ?: return@launch
            if (payouts.isEmpty()) return@launch

            var totalProcessedSum = 0.0
            var workersPaidCount = 0

            payouts.forEach { (workerId, amount) ->
                if (amount > 0) {
                    totalProcessedSum += amount
                    workersPaidCount++

                    // 1. WorkerPayment yozuvini kiritamiz
                    val payment = WorkerPayment(
                        workerId = workerId,
                        objectId = targetObjectId,
                        payerObjectId = payerObjectId,
                        amount = amount,
                        date = DateUtil.today(),
                        paymentDate = paymentDate.ifBlank { DateUtil.today() },
                        type = PaymentType.SALARY,
                        description = "Ommaviy to'lov: ish haqi to'landi"
                    )
                    transactionRepository.insertWorkerPayment(payment)

                    var remainingBudget = amount

                    // 2. Ishchining SHU OBYEKTDAGI to'lanmagan ochiq kunlarini eng eski kundan boshlab yopamiz
                    // (boshqa obyektdagi qarzi shu yerdagi to'lov bilan yopilib ketmasligi kerak)
                    val fallbackObjectId = _uiState.value.workers
                        .find { it.worker.id == workerId }?.worker?.objectId
                        ?: targetObjectId
                    val unpaidDays = workerDayRepository.getDaysByWorker(workerId).first()
                        .filter {
                            it.paymentStatus == PaymentStatus.UNPAID &&
                                it.paymentAmount > 0 &&
                                (it.objectId ?: fallbackObjectId) == targetObjectId
                        }
                        .sortedBy { it.date }

                    for (unpaidDay in unpaidDays) {
                        if (remainingBudget <= 0) break
                        if (remainingBudget >= unpaidDay.paymentAmount) {
                            workerDayRepository.saveWorkerDay(
                                unpaidDay.copy(
                                    paymentStatus = PaymentStatus.PAID,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                            remainingBudget -= unpaidDay.paymentAmount
                        } else {
                            break
                        }
                    }
                }
            }

            _uiState.update {
                it.copy(
                    isBulkPayoutSheetOpen = false,
                    successMessage = "$workersPaidCount nafar ishchiga jami ${uz.buildflow.app.core.util.CurrencyFormatter.formatAmount(totalProcessedSum)} ish haqi to'landi!"
                )
            }

            loadWorkersForObject(targetObjectId)
        }
    }

    // BOSHQA OBYEKTDAN ISHCHILARNI OLIB KELISH (IMPORT)
    fun openImportWorkerSheet() {
        viewModelScope.launch {
            val allObjects = _uiState.value.availableObjects.associateBy { it.id }
            val currentWorkerIds = _uiState.value.workers.map { it.worker.id }.toSet()

            workerRepository.getAllWorkers().firstOrNull()?.let { allWorkers ->
                val candidates = allWorkers
                    .filter { !currentWorkerIds.contains(it.id) }
                    .distinctBy { it.id }
                    .map { w ->
                        val objName = allObjects[w.objectId]?.name ?: "Boshqa obyekt"
                        ImportableWorkerItem(worker = w, sourceObjectName = objName)
                    }

                if (candidates.isEmpty()) {
                    _uiState.update { it.copy(errorMessage = "Boshqa obyektlarda qo'shish uchun yangi ishchi topilmadi!") }
                } else {
                    _uiState.update { it.copy(importableWorkers = candidates, isImportSheetOpen = true) }
                }
            }
        }
    }

    fun closeImportWorkerSheet() {
        _uiState.update { it.copy(isImportSheetOpen = false) }
    }

    fun importWorkersToCurrentObject(sourceWorkers: List<Worker>) {
        viewModelScope.launch {
            val targetObjectId = _uiState.value.selectedObjectId ?: return@launch
            if (sourceWorkers.isEmpty()) return@launch

            // Ishchini joriy obyektga KO'CHIRAMIZ: bundan buyon asosiy obyekti shu bo'ladi,
            // lekin eski obyektdagi tarixi/qarzi saqlanadi va u yerda ham ko'rinishda qoladi
            // (TransferWorkerUseCase orqali - object_id yozilmagan eski kunlarni eski obyektga
            // muhrlab, keyin asosiy obyektni almashtiradi).
            sourceWorkers.forEach { worker ->
                transferWorkerUseCase(worker.id, targetObjectId)
            }

            _uiState.update {
                it.copy(
                    isImportSheetOpen = false,
                    successMessage = if (sourceWorkers.size == 1) {
                        "${sourceWorkers.first().name} ushbu obyektga ko'chirildi!"
                    } else {
                        "${sourceWorkers.size} nafar ishchi ushbu obyektga ko'chirildi!"
                    }
                )
            }

            loadWorkersForObject(targetObjectId)
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

    fun clearSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun saveWorker(objectId: String, name: String, phone: String?, position: String?, defaultRate: Double, startDate: String) {
        viewModelScope.launch {
            val selected = _uiState.value.selectedWorker
            if (selected != null) {
                if (selected.objectId != objectId) {
                    // Obyekti o'zgargan bo'lsa - to'g'ridan-to'g'ri UPDATE emas, balki
                    // eski obyektdagi tarixi/qarzini saqlab qoluvchi ko'chirish orqali o'tkazamiz
                    transferWorkerUseCase(selected.id, objectId)
                }
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
            transactionRepository: TransactionRepository,
            workerDayRepository: WorkerDayRepository,
            getWorkerStatsUseCase: GetWorkerStatsUseCase,
            transferWorkerUseCase: TransferWorkerUseCase,
            initialObjectId: String? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WorkersViewModel(
                    workerRepository,
                    objectRepository,
                    transactionRepository,
                    workerDayRepository,
                    getWorkerStatsUseCase,
                    transferWorkerUseCase,
                    initialObjectId
                ) as T
            }
        }
    }
}
