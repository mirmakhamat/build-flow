package uz.buildflow.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import uz.buildflow.app.core.database.AppDatabase
import uz.buildflow.app.domain.model.*

@OptIn(ExperimentalCoroutinesApi::class)
class GetGlobalWorkersReportUseCase(
    private val database: AppDatabase
) {
    operator fun invoke(): Flow<GlobalWorkersReportSummary> {
        val workerDao = database.workerDao()
        val objectDao = database.objectDao()
        val workerDayDao = database.workerDayDao()
        val dailyBonusDao = database.dailyBonusDao()
        val generalBonusDao = database.generalBonusDao()
        val workerPaymentDao = database.workerPaymentDao()

        return combine(
            workerDao.getAllWorkers(),
            objectDao.getAllObjects()
        ) { workerEntities, objectEntities ->
            val objectMap = objectEntities.associate { it.id to it.name }
            Pair(workerEntities, objectMap)
        }.flatMapLatest { (workerEntities, objectMap) ->
            if (workerEntities.isEmpty()) {
                return@flatMapLatest flowOf(
                    GlobalWorkersReportSummary(
                        totalWorkersCount = 0,
                        totalEarnedAll = 0.0,
                        totalPaidAll = 0.0,
                        totalDebtAll = 0.0,
                        totalAdvanceAll = 0.0,
                        workers = emptyList()
                    )
                )
            }

            // Ishchilarni ismi va telefoni bo'yicha guruhlaymiz (agar bitta ishchi bir nechta obyektga ko'chirilgan yoki nusxalangan bo'lsa)
            val workerEntityMap = workerEntities.associateBy { it.id }
            val workerGroups = workerEntities.groupBy { entity ->
                val normName = entity.name.trim().lowercase()
                val normPhone = entity.phone?.trim()?.filter { it.isDigit() } ?: ""
                if (normPhone.isNotEmpty()) "$normName|$normPhone" else normName
            }

            val consolidatedWorkerFlows = workerGroups.values.map { workersInGroup ->
                val primaryWorker = workersInGroup.maxByOrNull { it.updatedAt } ?: workersInGroup.first()
                val workerIds = workersInGroup.map { it.id }

                // Ushbu ishchining barcha id'lari bo'yicha ma'lumotlarni yig'amiz
                val daysFlows = workerIds.map { workerDayDao.getDaysByWorker(it) }
                val dailyBonusFlows = workerIds.map { dailyBonusDao.getBonusesByWorker(it) }
                val generalBonusFlows = workerIds.map { generalBonusDao.getBonusesByWorker(it) }
                val paymentFlows = workerIds.map { workerPaymentDao.getPaymentsByWorker(it) }

                combine(
                    combine(daysFlows) { it.flatMap { list -> list } },
                    combine(dailyBonusFlows) { it.flatMap { list -> list } },
                    combine(generalBonusFlows) { it.flatMap { list -> list } },
                    combine(paymentFlows) { it.flatMap { list -> list } }
                ) { allDays, allDailyBonuses, allGeneralBonuses, allPayments ->
                    val workedDays = allDays.filter { it.status == "WORKED" || it.status == "HALF_DAY" }
                        .sortedBy { it.date }

                    val bonusMap = allDailyBonuses.groupBy { it.workerDayId }

                    // Jami to'langan pullar
                    val totalPaid = allPayments.sumOf { it.amount }
                    var remainingPool = totalPaid

                    val daysHistory = workedDays.map { day ->
                        val dayBonuses = bonusMap[day.id] ?: emptyList()
                        val dayBonusSum = dayBonuses.sumOf { it.amount }
                        val bonusReason = dayBonuses.mapNotNull { it.reason?.ifBlank { null } }.joinToString(", ")
                        val totalDayEarned = day.paymentAmount + dayBonusSum

                        val covered = remainingPool >= totalDayEarned
                        val remainingDebt = if (covered) 0.0 else (totalDayEarned - remainingPool).coerceAtLeast(0.0)
                        remainingPool = (remainingPool - totalDayEarned).coerceAtLeast(0.0)

                        val dayObjId = workerEntityMap[day.workerId]?.objectId ?: primaryWorker.objectId
                        val objName = objectMap[dayObjId] ?: "Noma'lum Obyekt"

                        WorkerDayDetailRecord(
                            date = day.date,
                            objectId = dayObjId,
                            objectName = objName,
                            dailyRate = day.paymentAmount,
                            bonusAmount = dayBonusSum,
                            bonusReason = bonusReason.ifBlank { null },
                            totalDayAmount = totalDayEarned,
                            isFullyCovered = covered,
                            remainingDebt = remainingDebt
                        )
                    }

                    // Umumiy bonuslar
                    val generalBonusTotal = allGeneralBonuses.sumOf { it.amount }
                    val salaryTotal = daysHistory.sumOf { it.totalDayAmount }
                    val totalEarned = salaryTotal + generalBonusTotal
                    val balance = totalEarned - totalPaid

                    val paymentsHistory = allPayments.sortedByDescending { it.date }.map { p ->
                        val objName = objectMap[p.objectId] ?: "Umumiy"
                        val sourceStr = if (p.payerObjectId == "OWN_POCKET") {
                            "👤 O'z hisobimdan"
                        } else {
                            "Kassa"
                        }
                        WorkerPaymentDetailRecord(
                            paymentId = p.id,
                            date = p.date,
                            amount = p.amount,
                            objectId = p.objectId,
                            objectName = objName,
                            paymentSource = sourceStr,
                            note = p.description
                        )
                    }

                    // Obyektlar kesimidagi tahlil
                    val allAssociatedObjectIds = buildSet {
                        workersInGroup.forEach { add(it.objectId) }
                        daysHistory.forEach { add(it.objectId) }
                        allPayments.forEach { add(it.objectId) }
                        allGeneralBonuses.forEach { add(it.objectId) }
                    }

                    val objectsBreakdown = allAssociatedObjectIds.mapNotNull { objId ->
                        val objName = objectMap[objId] ?: "Noma'lum Obyekt"
                        val objDays = daysHistory.filter { it.objectId == objId }
                        val objEarned = objDays.sumOf { it.totalDayAmount } + allGeneralBonuses.filter { it.objectId == objId }.sumOf { it.amount }
                        val objPaid = allPayments.filter { it.objectId == objId }.sumOf { it.amount }

                        if (objDays.isNotEmpty() || objEarned > 0 || objPaid > 0 || objId == primaryWorker.objectId) {
                            WorkerObjectBreakdownItem(
                                objectId = objId,
                                objectName = objName,
                                daysCount = objDays.size,
                                earnedAmount = objEarned,
                                paidAmount = objPaid
                            )
                        } else {
                            null
                        }
                    }.sortedByDescending { it.earnedAmount + it.paidAmount }

                    val workedObjectNames = objectsBreakdown.map { it.objectName }.distinct()
                        .ifEmpty { listOfNotNull(objectMap[primaryWorker.objectId]).ifEmpty { listOf("Obyekt belgilanmagan") } }

                    WorkerGlobalReportItem(
                        workerId = primaryWorker.id,
                        workerName = primaryWorker.name,
                        position = primaryWorker.position ?: "",
                        phone = primaryWorker.phone,
                        status = primaryWorker.status,
                        defaultRate = primaryWorker.defaultRate,
                        workedDaysCount = daysHistory.size,
                        totalEarned = totalEarned,
                        totalPaid = totalPaid,
                        balance = balance,
                        workedObjects = workedObjectNames,
                        daysHistory = daysHistory.sortedByDescending { it.date },
                        paymentsHistory = paymentsHistory,
                        objectsBreakdown = objectsBreakdown
                    )
                }
            }

            combine(consolidatedWorkerFlows) { itemsArray ->
                val items = itemsArray.toList()
                val totalEarnedAll = items.sumOf { it.totalEarned }
                val totalPaidAll = items.sumOf { it.totalPaid }
                val totalDebtAll = items.filter { it.balance > 0 }.sumOf { it.balance }
                val totalAdvanceAll = items.filter { it.balance < 0 }.sumOf { -it.balance }

                GlobalWorkersReportSummary(
                    totalWorkersCount = items.size,
                    totalEarnedAll = totalEarnedAll,
                    totalPaidAll = totalPaidAll,
                    totalDebtAll = totalDebtAll,
                    totalAdvanceAll = totalAdvanceAll,
                    workers = items
                )
            }
        }
    }
}
