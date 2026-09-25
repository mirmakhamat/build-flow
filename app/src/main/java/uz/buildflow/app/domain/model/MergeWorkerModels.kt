package uz.buildflow.app.domain.model

import uz.buildflow.app.data.local.entity.WorkerEntity

data class WorkerCandidateInfo(
    val worker: WorkerEntity,
    val objectName: String,
    val daysWorkedCount: Int = 0,
    val totalEarned: Double = 0.0,
    val totalPaid: Double = 0.0,
    val balance: Double = 0.0,
    val lastActiveDate: String? = null
)

data class DuplicateWorkerGroup(
    val id: String,
    val matchReason: String,
    val displayName: String,
    val workers: List<WorkerCandidateInfo>,
    val suggestedMasterWorkerId: String
)

data class MergeResult(
    val targetWorkerId: String,
    val mergedWorkerIds: List<String>,
    val daysMergedCount: Int,
    val paymentsMergedCount: Int,
    val bonusesMergedCount: Int
)
