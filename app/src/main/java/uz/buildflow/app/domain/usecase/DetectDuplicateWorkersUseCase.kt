package uz.buildflow.app.domain.usecase

import kotlinx.coroutines.flow.first
import uz.buildflow.app.core.database.AppDatabase
import uz.buildflow.app.data.local.entity.WorkerEntity
import uz.buildflow.app.domain.model.DuplicateWorkerGroup
import uz.buildflow.app.domain.model.WorkerCandidateInfo

class DetectDuplicateWorkersUseCase(
    private val database: AppDatabase
) {
    suspend operator fun invoke(): List<DuplicateWorkerGroup> {
        val workerDao = database.workerDao()
        val workerDayDao = database.workerDayDao()
        val dailyBonusDao = database.dailyBonusDao()
        val generalBonusDao = database.generalBonusDao()
        val paymentDao = database.workerPaymentDao()
        val objectDao = database.objectDao()

        val allWorkers = workerDao.getAllWorkersDirect()
        if (allWorkers.size < 2) return emptyList()

        val allObjects = objectDao.getAllObjects().first().associateBy { it.id }

        // Ishchilar haqidagi statistik ma'lumotlarni yig'ish
        val workerCandidates = allWorkers.map { worker ->
            val objectName = allObjects[worker.objectId]?.name ?: "Noma'lum Obyekt"
            val days = workerDayDao.getDaysListByWorker(worker.id)
            val workedDaysCount = days.count { it.status == "WORKED" || it.status == "HALF_DAY" }
            val salaryEarned = days.sumOf { it.paymentAmount }

            val dailyBonuses = dailyBonusDao.getBonusesListByWorker(worker.id).sumOf { it.amount }
            val generalBonuses = generalBonusDao.getBonusesListByWorker(worker.id).sumOf { it.amount }
            val totalEarned = salaryEarned + dailyBonuses + generalBonuses

            val totalPaid = paymentDao.getPaymentsListByWorker(worker.id).sumOf { it.amount }
            val balance = totalEarned - totalPaid
            val lastActiveDate = days.maxOfOrNull { it.date }

            WorkerCandidateInfo(
                worker = worker,
                objectName = objectName,
                daysWorkedCount = workedDaysCount,
                totalEarned = totalEarned,
                totalPaid = totalPaid,
                balance = balance,
                lastActiveDate = lastActiveDate
            )
        }

        // Birlashtirish uchun klasterlarni shakllantirish (Disjoint Set / Union Find)
        val parent = mutableMapOf<String, String>()
        val matchReasons = mutableMapOf<String, MutableSet<String>>()

        fun find(i: String): String {
            if (parent[i] == null) parent[i] = i
            if (parent[i] != i) {
                parent[i] = find(parent[i]!!)
            }
            return parent[i]!!
        }

        fun union(i: String, j: String, reason: String) {
            val rootI = find(i)
            val rootJ = find(j)
            if (rootI != rootJ) {
                parent[rootJ] = rootI
                val reasons = matchReasons.getOrPut(rootI) { mutableSetOf() }
                reasons.addAll(matchReasons[rootJ] ?: emptySet())
                reasons.add(reason)
            } else {
                matchReasons.getOrPut(rootI) { mutableSetOf() }.add(reason)
            }
        }

        // 1. Telefon raqami bo'yicha guruhlash (kamida 7 ta raqam bir xil bo'lsa)
        val phoneGroups = workerCandidates
            .filter { !it.worker.phone.isNullOrBlank() }
            .groupBy { normalizePhone(it.worker.phone) }

        for ((phone, list) in phoneGroups) {
            if (!phone.isNullOrBlank() && list.size >= 2) {
                val first = list.first()
                for (other in list.drop(1)) {
                    union(first.worker.id, other.worker.id, "Bir xil telefon: ${first.worker.phone}")
                }
            }
        }

        // 2. Ism bo'yicha guruhlash (to'liq tozalangan ism bo'yicha)
        val nameGroups = workerCandidates.groupBy { normalizeName(it.worker.name) }
        for ((normName, list) in nameGroups) {
            if (normName.length >= 3 && list.size >= 2) {
                val first = list.first()
                for (other in list.drop(1)) {
                    union(first.worker.id, other.worker.id, "Bir xil ism")
                }
            }
        }

        // Guruhlarni yig'ish
        val groupedMap = mutableMapOf<String, MutableList<WorkerCandidateInfo>>()
        for (candidate in workerCandidates) {
            val root = find(candidate.worker.id)
            groupedMap.getOrPut(root) { mutableListOf() }.add(candidate)
        }

        val duplicateGroups = mutableListOf<DuplicateWorkerGroup>()

        for ((rootId, groupWorkers) in groupedMap) {
            if (groupWorkers.size >= 2) {
                // Eng ko'p ishlagan yoki eng to'liq ma'lumotga ega ishchini tavsiya etilgan Master deb belgilash
                val suggestedMaster = groupWorkers.maxWithOrNull(
                    compareBy<WorkerCandidateInfo> { it.daysWorkedCount }
                        .thenBy { it.totalEarned }
                        .thenBy { if (!it.worker.phone.isNullOrBlank()) 1 else 0 }
                        .thenByDescending { it.worker.createdAt }
                )?.worker?.id ?: groupWorkers.first().worker.id

                val displayName = groupWorkers.first().worker.name
                val reasons = matchReasons[rootId]?.toList() ?: listOf("O'xshash profil")
                val reasonText = reasons.joinToString(", ")

                duplicateGroups.add(
                    DuplicateWorkerGroup(
                        id = rootId,
                        matchReason = reasonText,
                        displayName = displayName,
                        workers = groupWorkers.sortedByDescending { it.daysWorkedCount },
                        suggestedMasterWorkerId = suggestedMaster
                    )
                )
            }
        }

        return duplicateGroups.sortedByDescending { it.workers.size }
    }

    suspend fun getAllWorkerCandidates(): List<WorkerCandidateInfo> {
        val workerDao = database.workerDao()
        val workerDayDao = database.workerDayDao()
        val dailyBonusDao = database.dailyBonusDao()
        val generalBonusDao = database.generalBonusDao()
        val paymentDao = database.workerPaymentDao()
        val objectDao = database.objectDao()

        val allWorkers = workerDao.getAllWorkersDirect()
        val allObjects = objectDao.getAllObjects().first().associateBy { it.id }

        return allWorkers.map { worker ->
            val objectName = allObjects[worker.objectId]?.name ?: "Noma'lum Obyekt"
            val days = workerDayDao.getDaysListByWorker(worker.id)
            val workedDaysCount = days.count { it.status == "WORKED" || it.status == "HALF_DAY" }
            val salaryEarned = days.sumOf { it.paymentAmount }

            val dailyBonuses = dailyBonusDao.getBonusesListByWorker(worker.id).sumOf { it.amount }
            val generalBonuses = generalBonusDao.getBonusesListByWorker(worker.id).sumOf { it.amount }
            val totalEarned = salaryEarned + dailyBonuses + generalBonuses

            val totalPaid = paymentDao.getPaymentsListByWorker(worker.id).sumOf { it.amount }
            val balance = totalEarned - totalPaid
            val lastActiveDate = days.maxOfOrNull { it.date }

            WorkerCandidateInfo(
                worker = worker,
                objectName = objectName,
                daysWorkedCount = workedDaysCount,
                totalEarned = totalEarned,
                totalPaid = totalPaid,
                balance = balance,
                lastActiveDate = lastActiveDate
            )
        }.sortedBy { it.worker.name }
    }

    private fun normalizeName(name: String): String {
        return name
            .lowercase()
            .replace(Regex("\\s*\\([^)]*\\)"), "") // Qavs ichidagi yozuvlarni olib tashlash (masalan "(usta)")
            .replace(Regex("[^a-zа-яёўқғҳ0-9]"), "") // Belgilar va probellarni olib tashlash
            .trim()
    }

    private fun normalizePhone(phone: String?): String? {
        if (phone == null) return null
        val digits = phone.replace(Regex("[^0-9]"), "")
        return if (digits.length >= 7) digits else null
    }
}
