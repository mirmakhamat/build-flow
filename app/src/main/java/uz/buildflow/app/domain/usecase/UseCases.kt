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
    operator fun invoke(workerId: String): Flow<WorkerStats?> {
        return combine(
            workerRepository.getWorkerById(workerId),
            workerDayRepository.getTotalSalaryByWorker(workerId),
            workerDayRepository.getTotalDailyBonusesByWorker(workerId),
            workerDayRepository.getTotalGeneralBonusesByWorker(workerId),
            transactionRepository.getTotalPaidByWorker(workerId),
            workerDayRepository.getWorkedDaysCountByWorker(workerId)
        ) { array ->
            val worker = array[0] as? Worker ?: return@combine null
            val salary = array[1] as? Double ?: 0.0
            val dailyBonus = array[2] as? Double ?: 0.0
            val generalBonus = array[3] as? Double ?: 0.0
            val paid = array[4] as? Double ?: 0.0
            val daysCount = array[5] as? Int ?: 0

            WorkerStats(
                workerId = worker.id,
                workerName = worker.name,
                position = worker.position,
                defaultRate = worker.defaultRate,
                workedDaysCount = daysCount,
                totalSalaryEarned = salary,
                totalDailyBonuses = dailyBonus,
                totalGeneralBonuses = generalBonus,
                totalPaid = paid
            )
        }
    }
}
