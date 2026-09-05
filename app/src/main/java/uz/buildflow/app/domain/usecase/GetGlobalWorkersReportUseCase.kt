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

            val workerFlows = workerEntities.map { workerEntity ->
                combine(
                    workerDayDao.getDaysByWorker(workerEntity.id),
                    dailyBonusDao.getBonusesByWorker(workerEntity.id),
                    generalBonusDao.getBonusesByWorker(workerEntity.id),
                    workerPaymentDao.getPaymentsByWorker(workerEntity.id)
                ) { days, dailyBonuses, generalBonuses, payments ->
                    val workedDays = days.filter { it.status == "WORKED" || it.status == "HALF_DAY" }
                        .sortedBy { it.date }

                    val bonusMap = dailyBonuses.groupBy { it.workerDayId }

                    // Jami to'langan pullar
                    val totalPaid = payments.sumOf { it.amount }

                    var remainingPool = totalPaid

                    val daysHistory = workedDays.map { day ->
                        val dayBonuses = bonusMap[day.id] ?: emptyList()
                        val dayBonusSum = dayBonuses.sumOf { it.amount }
                        val bonusReason = dayBonuses.mapNotNull { it.reason?.ifBlank { null } }.joinToString(", ")
                        val totalDayEarned = day.paymentAmount + dayBonusSum

                        val covered = remainingPool >= totalDayEarned
                        val remainingDebt = if (covered) 0.0 else (totalDayEarned - remainingPool).coerceAtLeast(0.0)
                        remainingPool = (remainingPool - totalDayEarned).coerceAtLeast(0.0)

                        val objName = objectMap[workerEntity.objectId] ?: "Noma'lum Obyekt"

                        WorkerDayDetailRecord(
                            date = day.date,
                            objectId = workerEntity.objectId,
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
                    val generalBonusTotal = generalBonuses.sumOf { it.amount }
                    val salaryTotal = daysHistory.sumOf { it.totalDayAmount }
                    val totalEarned = salaryTotal + generalBonusTotal
                    val balance = totalEarned - totalPaid

                    val paymentsHistory = payments.sortedByDescending { it.date }.map { p ->
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

                    val workedObjectNames = listOfNotNull(objectMap[workerEntity.objectId]).ifEmpty { listOf("Obyekt belgilanmagan") }

                    val objectsBreakdown = listOf(
                        WorkerObjectBreakdownItem(
                            objectId = workerEntity.objectId,
                            objectName = workedObjectNames.first(),
                            daysCount = daysHistory.size,
                            earnedAmount = totalEarned,
                            paidAmount = totalPaid
                        )
                    )

                    WorkerGlobalReportItem(
                        workerId = workerEntity.id,
                        workerName = workerEntity.name,
                        position = workerEntity.position ?: "",
                        phone = workerEntity.phone,
                        status = workerEntity.status,
                        defaultRate = workerEntity.defaultRate,
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

            combine(workerFlows) { itemsArray ->
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
