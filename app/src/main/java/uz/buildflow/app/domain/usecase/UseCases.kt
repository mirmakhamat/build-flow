package uz.buildflow.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import uz.buildflow.app.domain.model.*
import uz.buildflow.app.domain.repository.ExpenseRepository
import uz.buildflow.app.domain.repository.ObjectRepository
import uz.buildflow.app.domain.repository.TransactionRepository
import uz.buildflow.app.domain.repository.WorkerDayRepository
import uz.buildflow.app.domain.repository.WorkerRepository

class GetObjectFinancialSummaryUseCase(
    private val objectRepository: ObjectRepository,
    private val workerRepository: WorkerRepository,
    private val workerDayRepository: WorkerDayRepository,
    private val expenseRepository: ExpenseRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(objectId: String): Flow<ObjectFinancialSummary?> {
        return combine(
            objectRepository.getObjectById(objectId),
            transactionRepository.getTotalIncomeByObject(objectId),
            workerDayRepository.getTotalSalaryByObject(objectId),
            workerDayRepository.getTotalDailyBonusesByObject(objectId),
            workerDayRepository.getTotalGeneralBonusesByObject(objectId),
            transactionRepository.getTotalPaidByObject(objectId),
            expenseRepository.getTotalExpenseByObject(objectId),
            expenseRepository.getTotalCashExpensePaidByObject(objectId),
            expenseRepository.getCategoryBreakdowns(objectId),
            workerRepository.getActiveWorkerCount(objectId),
            workerDayRepository.getTotalWorkedDaysCountByObject(objectId)
        ) { values ->
            val obj = values[0] as? BuildObject ?: return@combine null
            val income = values[1] as Double
            val salary = values[2] as Double
            val dailyBonus = values[3] as Double
            val generalBonus = values[4] as Double
            val workerPaid = values[5] as Double
            val otherExpenses = values[6] as Double
            val paidOtherExpenses = values[7] as Double
            @Suppress("UNCHECKED_CAST")
            val categories = values[8] as List<CategoryExpenseBreakdown>
            val workerCount = values[9] as Int
            val workDaysCount = values[10] as Int

            ObjectFinancialSummary(
                objectId = obj.id,
                objectName = obj.name,
                totalPrice = obj.totalPrice,
                totalReceivedIncome = income,
                totalWorkerSalary = salary,
                totalDailyBonuses = dailyBonus,
                totalGeneralBonuses = generalBonus,
                totalPaidToWorkers = workerPaid,
                totalOtherExpenses = otherExpenses,
                totalPaidOtherExpenses = paidOtherExpenses,
                categoryBreakdowns = categories,
                totalWorkerCount = workerCount,
                totalWorkDaysCount = workDaysCount
            )
        }
    }
}

class GetWorkerStatsUseCase(
    private val workerRepository: WorkerRepository,
    private val workerDayRepository: WorkerDayRepository,
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(workerId: String): Flow<WorkerStats?> {
        return combine(
            workerRepository.getWorkerById(workerId),
            workerDayRepository.getDaysByWorker(workerId),
            workerDayRepository.getGeneralBonusesByWorker(workerId),
            transactionRepository.getPaymentsByWorker(workerId)
        ) { worker, days, generalBonuses, payments ->
            if (worker == null) return@combine null

            val workedDays = days.count { it.status == AttendanceStatus.WORKED || it.status == AttendanceStatus.HALF_DAY }
            val salary = days.filter { it.status != AttendanceStatus.ABSENT }.sumOf { it.paymentAmount }
            val generalBonus = generalBonuses.sumOf { it.amount }
            val totalPaid = payments.sumOf { it.amount }

            // To'lanmagan (qarzga yozilgan) kunlar stavkalari yig'indisi
            val unpaidDaysSalary = days.filter { 
                it.status != AttendanceStatus.ABSENT && 
                payments.none { p -> p.type == PaymentType.SALARY && p.date == it.date && p.amount > 0 }
            }.sumOf { it.paymentAmount }

            // To'lanmagan (qarzga yozilgan) bonuslar yig'indisi
            val unpaidBonuses = generalBonuses.filter { gb ->
                payments.none { p -> p.type == PaymentType.BONUS_PAYOUT && p.date == gb.date && p.amount == gb.amount }
            }.sumOf { it.amount }

            val totalUnpaidAccrued = unpaidDaysSalary + unpaidBonuses

            WorkerStats(
                workerId = worker.id,
                workerName = worker.name,
                position = worker.position,
                defaultRate = worker.defaultRate,
                workedDaysCount = workedDays,
                totalSalaryEarned = salary,
                totalDailyBonuses = 0.0,
                totalGeneralBonuses = generalBonus,
                totalPaid = totalPaid,
                totalUnpaidAccrued = totalUnpaidAccrued
            )
        }
    }
}
