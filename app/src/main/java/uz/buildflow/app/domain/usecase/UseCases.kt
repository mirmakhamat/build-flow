package uz.buildflow.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import uz.buildflow.app.domain.model.*
import uz.buildflow.app.domain.repository.*

class GetObjectFinancialSummaryUseCase(
    private val objectRepository: ObjectRepository,
    private val workerRepository: WorkerRepository,
    private val workerDayRepository: WorkerDayRepository,
    private val expenseRepository: ExpenseRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(objectId: String): Flow<ObjectFinancialSummary?> {
        val flow1 = combine(
            objectRepository.getObjectById(objectId),
            transactionRepository.getTotalIncomeByObject(objectId),
            transactionRepository.getTotalClientIncomeByObject(objectId),
            transactionRepository.getTotalTransfersInByObject(objectId),
            workerDayRepository.getTotalSalaryByObject(objectId)
        ) { obj, income, clientIncome, transfersIn, salary ->
            SummaryPart1(obj, income, clientIncome, transfersIn, salary)
        }

        val flow2 = combine(
            workerDayRepository.getTotalDailyBonusesByObject(objectId),
            workerDayRepository.getTotalGeneralBonusesByObject(objectId),
            transactionRepository.getTotalPaidForWorkersOfObject(objectId),
            transactionRepository.getTotalPaidByObject(objectId),
            transactionRepository.getTotalPaidForOtherObjectsWorkers(objectId)
        ) { dailyBonus, generalBonus, paidForWorkers, cashPaidWorkers, paidForOtherWorkers ->
            SummaryPart2(dailyBonus, generalBonus, paidForWorkers, cashPaidWorkers, paidForOtherWorkers)
        }

        val flow3 = combine(
            transactionRepository.getTotalPaidByOtherObjectsForThisWorkers(objectId),
            transactionRepository.getTotalPaidFromOwnPocketForThisWorkers(objectId),
            expenseRepository.getTotalBuildingExpenseByObject(objectId),
            expenseRepository.getTotalTransfersOutByObject(objectId),
            combine(
                expenseRepository.getTotalCashExpensePaidByObject(objectId),
                expenseRepository.getTotalExpensesPaidForOtherObjects(objectId),
                expenseRepository.getTotalExpensesPaidByOtherObjects(objectId),
                expenseRepository.getTotalExpensesPaidFromOwnPocket(objectId)
            ) { cashExp, forOther, byOther, ownExp -> Tuple4(cashExp, forOther, byOther, ownExp) }
        ) { paidByOtherForWorkers, paidFromOwnForWorkers, buildingExpenses, transfersOut, (cashExp, expensesForOther, expensesByOther, expensesFromOwn) ->
            SummaryPart3(
                paidByOtherForWorkers,
                paidFromOwnForWorkers,
                buildingExpenses,
                transfersOut,
                cashExp,
                expensesForOther,
                expensesByOther,
                expensesFromOwn
            )
        }

        val flow4 = combine(
            expenseRepository.getCategoryBreakdowns(objectId),
            workerRepository.getActiveWorkerCount(objectId),
            workerDayRepository.getTotalWorkedDaysCountByObject(objectId)
        ) { categories, workerCount, workDaysCount ->
            SummaryPart4(categories, workerCount, workDaysCount)
        }

        return combine(flow1, flow2, flow3, flow4) { p1, p2, p3, p4 ->
            val obj = p1.obj ?: return@combine null

            ObjectFinancialSummary(
                objectId = obj.id,
                objectName = obj.name,
                totalPrice = obj.totalPrice,
                totalReceivedIncome = p1.income,
                totalClientIncome = p1.clientIncome,
                totalTransfersIn = p1.transfersIn,
                totalWorkerSalary = p1.salary,
                totalDailyBonuses = p2.dailyBonus,
                totalGeneralBonuses = p2.generalBonus,
                totalPaidToWorkers = p2.paidForWorkers,
                totalCashPaidToWorkers = p2.cashPaidWorkers,
                totalPaidForOtherObjectsWorkers = p2.paidForOtherWorkers,
                totalPaidByOtherObjectsForThisWorkers = p3.paidByOtherForWorkers,
                totalPaidFromOwnPocketForThisWorkers = p3.paidFromOwnForWorkers,
                totalOtherExpenses = p3.buildingExpenses,
                totalTransfersOut = p3.transfersOut,
                totalPaidOtherExpenses = p3.paidOtherExpenses,
                totalExpensesPaidForOtherObjects = p3.expensesForOther,
                totalExpensesPaidByOtherObjects = p3.expensesByOther,
                totalExpensesPaidFromOwnPocket = p3.expensesFromOwn,
                categoryBreakdowns = p4.categories.filter { it.categoryName != "Kassalararo o'tkazma" },
                totalWorkerCount = p4.workerCount,
                totalWorkDaysCount = p4.workDaysCount
            )
        }
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    private data class SummaryPart1(
        val obj: BuildObject?,
        val income: Double,
        val clientIncome: Double,
        val transfersIn: Double,
        val salary: Double
    )

    private data class SummaryPart2(
        val dailyBonus: Double,
        val generalBonus: Double,
        val paidForWorkers: Double,
        val cashPaidWorkers: Double,
        val paidForOtherWorkers: Double
    )

    private data class SummaryPart3(
        val paidByOtherForWorkers: Double,
        val paidFromOwnForWorkers: Double,
        val buildingExpenses: Double,
        val transfersOut: Double,
        val paidOtherExpenses: Double,
        val expensesForOther: Double,
        val expensesByOther: Double,
        val expensesFromOwn: Double
    )

    private data class SummaryPart4(
        val categories: List<CategoryExpenseBreakdown>,
        val workerCount: Int,
        val workDaysCount: Int
    )
}

class GetWorkerStatsUseCase(
    private val workerRepository: WorkerRepository,
    private val workerDayRepository: WorkerDayRepository,
    private val transactionRepository: TransactionRepository
) {
    /**
     * [objectId] null yoki bo'sh bo'lsa - ishchining BARCHA obyektlardagi umumiy statistikasi qaytadi
     * (masalan Global hisobot yoki dublikatlarni aniqlash uchun).
     * [objectId] berilsa - faqat shu obyektga tegishli kun/bonus/to'lov yozuvlari hisoblanadi,
     * shunda bitta ishchining boshqa obyektdagi qarzi/avansi bu obyekt raqamlariga aralashib ketmaydi.
     */
    operator fun invoke(workerId: String, objectId: String? = null): Flow<WorkerStats?> {
        val effectiveObjectId = objectId?.takeIf { it.isNotBlank() }
        return combine(
            workerRepository.getWorkerById(workerId),
            workerDayRepository.getDaysByWorker(workerId),
            workerDayRepository.getDailyBonusesByWorker(workerId),
            workerDayRepository.getGeneralBonusesByWorker(workerId),
            transactionRepository.getPaymentsByWorker(workerId)
        ) { worker, days, dailyBonuses, generalBonuses, payments ->
            worker ?: return@combine null

            val scopedDays = if (effectiveObjectId == null) {
                days
            } else {
                days.filter { (it.objectId ?: worker.objectId) == effectiveObjectId }
            }
            val scopedDayIds = scopedDays.map { it.id }.toSet()
            val scopedDailyBonuses = if (effectiveObjectId == null) {
                dailyBonuses
            } else {
                dailyBonuses.filter { it.workerDayId in scopedDayIds }
            }
            val scopedGeneralBonuses = if (effectiveObjectId == null) {
                generalBonuses
            } else {
                generalBonuses.filter { it.objectId == effectiveObjectId }
            }
            val scopedPayments = if (effectiveObjectId == null) {
                payments
            } else {
                payments.filter { it.objectId == effectiveObjectId }
            }

            WorkerStats(
                workerId = worker.id,
                workerName = worker.name,
                position = worker.position,
                defaultRate = worker.defaultRate,
                workedDaysCount = scopedDays.count {
                    it.status == AttendanceStatus.WORKED || it.status == AttendanceStatus.HALF_DAY
                },
                totalSalaryEarned = scopedDays.sumOf { it.paymentAmount },
                totalDailyBonuses = scopedDailyBonuses.sumOf { it.amount },
                totalGeneralBonuses = scopedGeneralBonuses.sumOf { it.amount },
                totalPaid = scopedPayments.sumOf { it.amount }
            )
        }
    }
}

class GetGlobalFinancialSummaryUseCase(
    private val objectRepository: ObjectRepository,
    private val getObjectFinancialSummaryUseCase: GetObjectFinancialSummaryUseCase,
    private val transactionRepository: TransactionRepository,
    private val expenseRepository: ExpenseRepository,
    private val workerRepository: WorkerRepository
) {
    operator fun invoke(): Flow<GlobalFinancialSummary> {
        return objectRepository.getAllObjects().flatMapLatest { objects ->
            if (objects.isEmpty()) {
                flowOf(
                    GlobalFinancialSummary(
                        totalObjectsCount = 0,
                        activeObjectsCount = 0,
                        completedObjectsCount = 0,
                        totalAgreedPrice = 0.0,
                        totalReceivedIncome = 0.0,
                        totalClientIncome = 0.0,
                        totalWorkerSalaryEarned = 0.0,
                        totalBonusesEarned = 0.0,
                        totalOtherExpenses = 0.0,
                        totalPaidToWorkers = 0.0,
                        totalCashPaidToWorkers = 0.0,
                        totalPaidOtherExpenses = 0.0,
                        totalPaidFromOwnPocket = 0.0,
                        totalWorkerDebt = 0.0,
                        totalWorkerCount = 0,
                        totalWorkDaysCount = 0
                    )
                )
            } else {
                val objectSummaryFlows = objects.map { obj ->
                    getObjectFinancialSummaryUseCase(obj.id)
                }

                combine(
                    combine(objectSummaryFlows) { it },
                    workerRepository.getAllWorkers()
                ) { summariesArray, allWorkers ->
                    val nonNullSummaries = summariesArray.filterNotNull()
                    val totalObjects = objects.size
                    val activeObjects = objects.count { it.status == ObjectStatus.ACTIVE }
                    val completedObjects = objects.count { it.status == ObjectStatus.COMPLETED }

                    val totalAgreed = nonNullSummaries.sumOf { it.totalPrice }
                    val totalReceived = nonNullSummaries.sumOf { it.totalReceivedIncome }
                    val totalClientInc = nonNullSummaries.sumOf { it.totalClientIncome }
                    val totalSalary = nonNullSummaries.sumOf { it.totalWorkerSalary }
                    val totalDailyBonus = nonNullSummaries.sumOf { it.totalDailyBonuses }
                    val totalGenBonus = nonNullSummaries.sumOf { it.totalGeneralBonuses }
                    val totalBonuses = totalDailyBonus + totalGenBonus
                    val totalOtherExp = nonNullSummaries.sumOf { it.totalOtherExpenses }
                    val totalPaidWorkers = nonNullSummaries.sumOf { it.totalPaidToWorkers }
                    val totalCashPaidWorkers = nonNullSummaries.sumOf { it.totalCashPaidToWorkers }
                    val totalPaidOtherExp = nonNullSummaries.sumOf { it.totalPaidOtherExpenses }
                    val totalOwnPocket = nonNullSummaries.sumOf { it.totalPaidFromOwnPocket }
                    val totalWorkerDebt = nonNullSummaries.sumOf { it.totalWorkerDebt }
                    // Diqqat: obyektlar bo'yicha ishchilar sonini yig'indisi emas, balki
                    // BARCHA obyektlar bo'yicha TAKRORLANMAS ishchilar soni - aks holda bir nechta
                    // obyektga biriktirilgan ishchi bir necha marta hisoblanib ketardi.
                    val totalWorkers = allWorkers.distinctBy { it.id }.size
                    val totalWorkDays = nonNullSummaries.sumOf { it.totalWorkDaysCount }

                    // Obyektlar rentabelligi va reytingi
                    val objProfitabilities = nonNullSummaries.map { s ->
                        val obj = objects.find { it.id == s.objectId }
                        val status = obj?.status ?: ObjectStatus.ACTIVE
                        val margin = if (s.totalPrice > 0) (s.estimatedProfit / s.totalPrice) * 100.0 else 0.0
                        val completion = if (s.totalPrice > 0) (s.totalClientIncome / s.totalPrice) * 100.0 else 0.0

                        ObjectProfitabilityItem(
                            objectId = s.objectId,
                            objectName = s.objectName,
                            status = status,
                            totalPrice = s.totalPrice,
                            totalIncome = s.totalClientIncome,
                            totalExpenses = s.totalExpenses,
                            cashBalance = s.cashBalance,
                            estimatedProfit = s.estimatedProfit,
                            profitMargin = margin,
                            completionPercentage = completion
                        )
                    }.sortedByDescending { it.estimatedProfit }

                    // Global kategoriyalar bo'yicha yig'ma xarajatlar
                    val categoryMap = mutableMapOf<String, Double>()
                    nonNullSummaries.forEach { s ->
                        s.categoryBreakdowns.forEach { cat ->
                            categoryMap[cat.categoryName] = (categoryMap[cat.categoryName] ?: 0.0) + cat.totalAmount
                        }
                    }
                    val globalCategoryList = categoryMap.map { (catName, amount) ->
                        CategoryExpenseBreakdown(categoryName = catName, totalAmount = amount)
                    }.sortedByDescending { it.totalAmount }

                    // Obyektlararo o'zaro qarzdorliklar (boshqa obyekt uchun to'langan ish haqi va xarajatlar)
                    val interDebts = mutableListOf<InterObjectDebtItem>()
                    nonNullSummaries.forEach { s ->
                        if (s.totalPaidForOtherObjectsWorkers > 0) {
                            interDebts.add(
                                InterObjectDebtItem(
                                    sourceObjectId = s.objectId,
                                    sourceObjectName = s.objectName,
                                    targetObjectId = "",
                                    targetObjectName = "Boshqa obyekt ishchilari",
                                    amount = s.totalPaidForOtherObjectsWorkers,
                                    reason = "Ishchilar maoshi uchun to'lab berilgan"
                                )
                            )
                        }
                        if (s.totalExpensesPaidForOtherObjects > 0) {
                            interDebts.add(
                                InterObjectDebtItem(
                                    sourceObjectId = s.objectId,
                                    sourceObjectName = s.objectName,
                                    targetObjectId = "",
                                    targetObjectName = "Boshqa obyekt xarajati",
                                    amount = s.totalExpensesPaidForOtherObjects,
                                    reason = "Material va boshqa xarajatlar uchun qoplangan"
                                )
                            )
                        }
                    }

                    GlobalFinancialSummary(
                        totalObjectsCount = totalObjects,
                        activeObjectsCount = activeObjects,
                        completedObjectsCount = completedObjects,
                        totalAgreedPrice = totalAgreed,
                        totalReceivedIncome = totalReceived,
                        totalClientIncome = totalClientInc,
                        totalWorkerSalaryEarned = totalSalary,
                        totalBonusesEarned = totalBonuses,
                        totalOtherExpenses = totalOtherExp,
                        totalPaidToWorkers = totalPaidWorkers,
                        totalCashPaidToWorkers = totalCashPaidWorkers,
                        totalPaidOtherExpenses = totalPaidOtherExp,
                        totalPaidFromOwnPocket = totalOwnPocket,
                        totalWorkerDebt = totalWorkerDebt,
                        totalWorkerCount = totalWorkers,
                        totalWorkDaysCount = totalWorkDays,
                        objectSummaries = nonNullSummaries,
                        objectProfitabilities = objProfitabilities,
                        globalCategoryBreakdowns = globalCategoryList,
                        interObjectDebts = interDebts
                    )
                }
            }
        }
    }
}
