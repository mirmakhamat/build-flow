package uz.buildflow.app.presentation.workers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.buildflow.app.domain.model.DuplicateWorkerGroup
import uz.buildflow.app.domain.model.MergeResult
import uz.buildflow.app.domain.model.WorkerCandidateInfo
import uz.buildflow.app.domain.usecase.DetectDuplicateWorkersUseCase
import uz.buildflow.app.domain.usecase.MergeWorkersUseCase

data class MergeWorkersUiState(
    val isLoading: Boolean = true,
    val isMerging: Boolean = false,
    val duplicateGroups: List<DuplicateWorkerGroup> = emptyList(),
    val allCandidates: List<WorkerCandidateInfo> = emptyList(),
    val selectedGroupMasterMap: Map<String, String> = emptyMap(), // groupId -> targetWorkerId
    val manualSelectedWorkerIds: Set<String> = emptySet(),
    val manualMasterWorkerId: String? = null,
    val searchQuery: String = "",
    val activeTab: Int = 0, // 0: Auto-detected, 1: Manual
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val lastMergeResult: MergeResult? = null
)

class MergeWorkersViewModel(
    private val detectDuplicateWorkersUseCase: DetectDuplicateWorkersUseCase,
    private val mergeWorkersUseCase: MergeWorkersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MergeWorkersUiState())
    val uiState: StateFlow<MergeWorkersUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val groups = detectDuplicateWorkersUseCase()
                val allWorkers = detectDuplicateWorkersUseCase.getAllWorkerCandidates()

                // Har bir guruh uchun standart tavsiya etilgan masterni belgilaymiz
                val initialMasterMap = groups.associate { it.id to it.suggestedMasterWorkerId }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        duplicateGroups = groups,
                        allCandidates = allWorkers,
                        selectedGroupMasterMap = initialMasterMap
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Dublikatlarni qidirishda xatolik: ${e.localizedMessage ?: "Noma'lum xato"}"
                    )
                }
            }
        }
    }

    fun setActiveTab(tabIndex: Int) {
        _uiState.update { it.copy(activeTab = tabIndex) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setGroupMaster(groupId: String, masterWorkerId: String) {
        _uiState.update {
            val updatedMap = it.selectedGroupMasterMap.toMutableMap()
            updatedMap[groupId] = masterWorkerId
            it.copy(selectedGroupMasterMap = updatedMap)
        }
    }

    fun toggleManualSelection(workerId: String) {
        _uiState.update { state ->
            val updated = state.manualSelectedWorkerIds.toMutableSet()
            if (updated.contains(workerId)) {
                updated.remove(workerId)
            } else {
                updated.add(workerId)
            }

            var masterId = state.manualMasterWorkerId
            if (masterId != null && !updated.contains(masterId)) {
                masterId = updated.firstOrNull()
            } else if (masterId == null && updated.isNotEmpty()) {
                masterId = updated.first()
            }

            state.copy(
                manualSelectedWorkerIds = updated,
                manualMasterWorkerId = masterId
            )
        }
    }

    fun setManualMaster(workerId: String) {
        _uiState.update { it.copy(manualMasterWorkerId = workerId) }
    }

    fun mergeGroup(group: DuplicateWorkerGroup) {
        val masterId = _uiState.value.selectedGroupMasterMap[group.id] ?: group.suggestedMasterWorkerId
        val sourceIds = group.workers.map { it.worker.id }.filter { it != masterId }

        if (sourceIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isMerging = true, errorMessage = null) }
            val result = mergeWorkersUseCase(targetWorkerId = masterId, sourceWorkerIds = sourceIds)
            result.onSuccess { mergeResult ->
                _uiState.update {
                    it.copy(
                        isMerging = false,
                        successMessage = "${group.displayName} profillari muvaffaqiyatli 1 taga birlashtirildi! (${mergeResult.daysMergedCount} kun, ${mergeResult.paymentsMergedCount} to'lov ko'chirildi)",
                        lastMergeResult = mergeResult
                    )
                }
                loadData()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isMerging = false,
                        errorMessage = "Birlashtirishda xatolik yuz berdi: ${err.localizedMessage ?: "Noma'lum xato"}"
                    )
                }
            }
        }
    }

    fun mergeAllAutoDetected() {
        val groups = _uiState.value.duplicateGroups
        if (groups.isEmpty()) return

        // Xavfsizlik uchun: "Barchasini birlashtirish" faqat TELEFON RAQAMI bilan tasdiqlangan
        // (yuqori ishonchli) guruhlarni avtomatik birlashtiradi. Faqat ismi bir xil (telefonsiz)
        // guruhlar - bular ikki xil odam bo'lishi ham mumkin - alohida, ko'zdan kechirib
        // birma-bir birlashtirilishi kerak.
        val (phoneConfirmedGroups, nameOnlyGroups) = groups.partition {
            it.matchReason.contains("telefon", ignoreCase = true)
        }

        if (phoneConfirmedGroups.isEmpty()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Avtomatik birlashtirish uchun telefon raqami bilan tasdiqlangan dublikat topilmadi. " +
                        "Faqat ismi bir xil ${nameOnlyGroups.size} ta guruh bor - ular boshqa-boshqa odam bo'lishi mumkinligi uchun " +
                        "har birini alohida ko'rib chiqib birlashtiring."
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMerging = true, errorMessage = null) }
            var totalMergedWorkers = 0
            var totalDays = 0
            var totalPayments = 0

            for (group in phoneConfirmedGroups) {
                val masterId = _uiState.value.selectedGroupMasterMap[group.id] ?: group.suggestedMasterWorkerId
                val sourceIds = group.workers.map { it.worker.id }.filter { it != masterId }
                if (sourceIds.isNotEmpty()) {
                    val result = mergeWorkersUseCase(targetWorkerId = masterId, sourceWorkerIds = sourceIds)
                    result.onSuccess { res ->
                        totalMergedWorkers += res.mergedWorkerIds.size
                        totalDays += res.daysMergedCount
                        totalPayments += res.paymentsMergedCount
                    }
                }
            }

            val skippedNote = if (nameOnlyGroups.isNotEmpty()) {
                " Faqat ismi bir xil ${nameOnlyGroups.size} ta guruh xavfsizlik uchun o'tkazib yuborildi - ularni birma-bir ko'rib chiqing."
            } else ""

            _uiState.update {
                it.copy(
                    isMerging = false,
                    successMessage = "Telefon raqami bilan tasdiqlangan $totalMergedWorkers ta dublikat profil birlashtirildi! " +
                        "($totalDays kun, $totalPayments to'lov saqlandi).$skippedNote"
                )
            }
            loadData()
        }
    }

    fun mergeManualSelected() {
        val state = _uiState.value
        val masterId = state.manualMasterWorkerId
        val selectedIds = state.manualSelectedWorkerIds

        if (masterId == null || selectedIds.size < 2) {
            _uiState.update { it.copy(errorMessage = "Birlashtirish uchun kamida 2 ta ishchi va 1 ta asosiy profil tanlang!") }
            return
        }

        val sourceIds = selectedIds.filter { it != masterId }

        viewModelScope.launch {
            _uiState.update { it.copy(isMerging = true, errorMessage = null) }
            val result = mergeWorkersUseCase(targetWorkerId = masterId, sourceWorkerIds = sourceIds)
            result.onSuccess { mergeResult ->
                _uiState.update {
                    it.copy(
                        isMerging = false,
                        manualSelectedWorkerIds = emptySet(),
                        manualMasterWorkerId = null,
                        successMessage = "Tanlangan ${selectedIds.size} ta profil 1 taga birlashtirildi! (${mergeResult.daysMergedCount} kun, ${mergeResult.paymentsMergedCount} to'lov o'tkazildi)",
                        lastMergeResult = mergeResult
                    )
                }
                loadData()
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        isMerging = false,
                        errorMessage = "Birlashtirishda xato: ${err.localizedMessage ?: "Noma'lum xato"}"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    companion object {
        fun provideFactory(
            detectDuplicateWorkersUseCase: DetectDuplicateWorkersUseCase,
            mergeWorkersUseCase: MergeWorkersUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MergeWorkersViewModel(detectDuplicateWorkersUseCase, mergeWorkersUseCase) as T
            }
        }
    }
}
