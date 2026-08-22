package uz.buildflow.app.domain.model

import java.util.UUID

data class BuildObject(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String? = null,
    val totalPrice: Double,
    val currency: String = "UZS",
    val startDate: String,
    val endDate: String? = null,
    val status: ObjectStatus = ObjectStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class Worker(
    val id: String = UUID.randomUUID().toString(),
    val objectId: String,
    val name: String,
    val phone: String? = null,
    val position: String? = null,
    val defaultRate: Double = 0.0,
    val status: WorkerStatus = WorkerStatus.ACTIVE,
    val startDate: String,
    val endDate: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class WorkerDay(
    val id: String = UUID.randomUUID().toString(),
    val workerId: String,
    val date: String,
    val status: AttendanceStatus = AttendanceStatus.WORKED,
    val paymentAmount: Double = 0.0,
    val paymentStatus: PaymentStatus = PaymentStatus.UNPAID,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class DailyBonus(
    val id: String = UUID.randomUUID().toString(),
    val workerId: String,
    val workerDayId: String,
    val amount: Double,
    val date: String,
    val reason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class GeneralBonus(
    val id: String = UUID.randomUUID().toString(),
    val workerId: String,
    val objectId: String,
    val amount: Double,
    val date: String,
    val reason: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val objectId: String,
    val workerId: String? = null,
    val category: String = "Boshqa xarajat",
    val amount: Double,
    val date: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class MoneyTransaction(
    val id: String = UUID.randomUUID().toString(),
    val objectId: String,
    val type: TransactionType = TransactionType.INCOME,
    val amount: Double,
    val date: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class WorkerPayment(
    val id: String = UUID.randomUUID().toString(),
    val workerId: String,
    val objectId: String,
    val amount: Double,
    val date: String,
    val type: PaymentType = PaymentType.SALARY,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class ObjectFinancialSummary(
    val objectId: String,
    val objectName: String,
    val totalPrice: Double,
    val totalReceivedIncome: Double,
    val totalWorkerSalary: Double,
    val totalDailyBonuses: Double,
    val totalGeneralBonuses: Double,
    val totalPaidToWorkers: Double,
    val totalOtherExpenses: Double,
    val categoryBreakdowns: List<CategoryExpenseBreakdown> = emptyList(),
    val totalWorkerCount: Int,
    val totalWorkDaysCount: Int
) {
    val totalBonuses: Double
        get() = totalDailyBonuses + totalGeneralBonuses

    val totalAccruedExpenses: Double
        get() = totalWorkerSalary + totalBonuses + totalOtherExpenses

    val totalExpenses: Double
        get() = totalAccruedExpenses

    val totalWorkerDebt: Double
        get() = ((totalWorkerSalary + totalBonuses) - totalPaidToWorkers).coerceAtLeast(0.0)

    val remainingReceivable: Double
        get() = (totalPrice - totalReceivedIncome).coerceAtLeast(0.0)

    val cashBalance: Double
        get() = totalReceivedIncome - (totalPaidToWorkers + totalOtherExpenses)

    val estimatedProfit: Double
        get() = totalPrice - totalAccruedExpenses
}

data class WorkerStats(
    val workerId: String,
    val workerName: String,
    val position: String?,
    val defaultRate: Double,
    val workedDaysCount: Int,
    val totalSalaryEarned: Double,
    val totalDailyBonuses: Double,
    val totalGeneralBonuses: Double,
    val totalPaid: Double,
    val totalUnpaidAccrued: Double = 0.0
) {
    val totalEarned: Double
        get() = totalSalaryEarned + totalDailyBonuses + totalGeneralBonuses

    val remainingDebtToWorker: Double
        get() = (totalEarned - totalPaid).coerceAtLeast(0.0)
}
