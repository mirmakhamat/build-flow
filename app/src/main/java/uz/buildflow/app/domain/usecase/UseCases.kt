package uz.buildflow.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
            workerDayRepository.getTotalSalaryByObject(objectId),
            workerDayRepository.getTotalDailyBonusesByObject(objectId),
            workerDayRepository.getTotalGeneralBonusesByObject(objectId)
        ) { obj, income, salary, dailyBonus, generalBonus ->
            SummaryPart1(obj, income, salary, dailyBonus, generalBonus)
        }

        val flow2 = combine(
            transactionRepository.getTotalPaidForWorkersOfObject(objectId),
            transactionRepository.getTotalPaidByObject(objectId),
            transactionRepository.getTotalPaidForOtherObjectsWorkers(objectId),
            transactionRepository.getTotalPaidByOtherObjectsForThisWorkers(objectId)
        ) { paidForWorkers, cashPaidWorkers, paidForOtherWorkers, paidByOtherForWorkers ->
            SummaryPart2(paidForWorkers, cashPaidWorkers, paidForOtherWorkers, paidByOtherForWorkers)
        }

        val flow3 = combine(
            expenseRepository.getTotalExpenseByObject(objectId),
            expenseRepository.getTotalCashExpensePaidByObject(objectId),
            expenseRepository.getTotalExpensesPaidForOtherObjects(objectId),
            expenseRepository.getTotalExpensesPaidByOtherObjects(objectId)
        ) { otherExpenses, paidOtherExpenses, expensesForOther, expensesByOther ->
            SummaryPart3(otherExpenses, paidOtherExpenses, expensesForOther, expensesByOther)
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
                totalWorkerSalary = p1.salary,
                totalDailyBonuses = p1.dailyBonus,
                totalGeneralBonuses = p1.generalBonus,
                totalPaidToWorkers = p2.paidForWorkers,
                totalCashPaidToWorkers = p2.cashPaidWorkers,
                totalPaidForOtherObjectsWorkers = p2.paidForOtherWorkers,
                totalPaidByOtherObjectsForThisWorkers = p2.paidByOtherForWorkers,
                totalOtherExpenses = p3.otherExpenses,
                totalPaidOtherExpenses = p3.paidOtherExpenses,
                totalExpensesPaidForOtherObjects = p3.expensesForOther,
                totalExpensesPaidByOtherObjects = p3.expensesByOther,
                categoryBreakdowns = p4.categories,
                totalWorkerCount = p4.workerCount,
                totalWorkDaysCount = p4.workDaysCount
            )
        }
    }

    private data class SummaryPart1(
        val obj: BuildObject?,
        val income: Double,
        val salary: Double,
        val dailyBonus: Double,
        val generalBonus: Double
    )

    private data class SummaryPart2(
        val paidForWorkers: Double,
        val cashPaidWorkers: Double,
        val paidForOtherWorkers: Double,
        val paidByOtherForWorkers: Double
    )

    private data class SummaryPart3(
        val otherExpenses: Double,
        val paidOtherExpenses: Double,
        val expensesForOther: Double,
        val expensesByOther: Double
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
        ) { values ->
            val worker = values[0] as? Worker ?: return@combine null
            val salary = values[1] as Double
            val dailyBonus = values[2] as Double
            val generalBonus = values[3] as Double
            val paid = values[4] as Double
            val daysCount = values[5] as Int

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
