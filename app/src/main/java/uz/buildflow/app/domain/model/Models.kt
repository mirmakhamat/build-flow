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
    val objectId: String? = null,
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
    val payerObjectId: String? = null,
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
    val payerObjectId: String? = null,
    val amount: Double,
    val date: String,
    val paymentDate: String? = null,
    val type: PaymentType = PaymentType.SALARY,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class ObjectFinancialSummary(
    val objectId: String,
    val objectName: String,
    val totalPrice: Double,
    val totalReceivedIncome: Double,
    val totalClientIncome: Double = totalReceivedIncome,
    val totalTransfersIn: Double = 0.0,
    val totalWorkerSalary: Double,
    val totalDailyBonuses: Double,
    val totalGeneralBonuses: Double,
    val totalPaidToWorkers: Double,
    val totalCashPaidToWorkers: Double = totalPaidToWorkers,
    val totalPaidForOtherObjectsWorkers: Double = 0.0,
    val totalPaidByOtherObjectsForThisWorkers: Double = 0.0,
    val totalPaidFromOwnPocketForThisWorkers: Double = 0.0,
    val totalOtherExpenses: Double,
    val totalTransfersOut: Double = 0.0,
    val totalPaidOtherExpenses: Double = totalOtherExpenses,
    val totalExpensesPaidForOtherObjects: Double = 0.0,
    val totalExpensesPaidByOtherObjects: Double = 0.0,
    val totalExpensesPaidFromOwnPocket: Double = 0.0,
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
        get() = (totalPrice - totalClientIncome).coerceAtLeast(0.0)

    val totalCashOutflow: Double
        get() = totalCashPaidToWorkers + totalPaidOtherExpenses

    val cashBalance: Double
        get() = totalReceivedIncome - totalCashOutflow

    val totalPaidFromOwnPocket: Double
        get() = totalPaidFromOwnPocketForThisWorkers + totalExpensesPaidFromOwnPocket

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

    val netBalance: Double
        get() = totalPaid - totalEarned

    val remainingDebtToWorker: Double
        get() = (totalEarned - totalPaid).coerceAtLeast(0.0)

    val workerDebtToUs: Double
        get() = (totalPaid - totalEarned).coerceAtLeast(0.0)
}

data class ObjectProfitabilityItem(
    val objectId: String,
    val objectName: String,
    val status: ObjectStatus,
    val totalPrice: Double,
    val totalIncome: Double,
    val totalExpenses: Double,
    val cashBalance: Double,
    val estimatedProfit: Double,
    val profitMargin: Double, // Foizda (masalan, 35.5%)
    val completionPercentage: Double // Shartnomaga nisbatan tushum foizi
)

data class InterObjectDebtItem(
    val sourceObjectId: String,
    val sourceObjectName: String,
    val targetObjectId: String,
    val targetObjectName: String,
    val amount: Double,
    val reason: String // Masalan: "Ishchilar ish haqi uchun" yoki "Material xarajati uchun"
)

data class GlobalFinancialSummary(
    val totalObjectsCount: Int,
    val activeObjectsCount: Int,
    val completedObjectsCount: Int,
    val totalAgreedPrice: Double,
    val totalReceivedIncome: Double,
    val totalClientIncome: Double,
    val totalWorkerSalaryEarned: Double,
    val totalBonusesEarned: Double,
    val totalOtherExpenses: Double,
    val totalPaidToWorkers: Double,
    val totalCashPaidToWorkers: Double,
    val totalPaidOtherExpenses: Double,
    val totalPaidFromOwnPocket: Double,
    val totalWorkerDebt: Double,
    val totalWorkerCount: Int,
    val totalWorkDaysCount: Int,
    val objectSummaries: List<ObjectFinancialSummary> = emptyList(),
    val objectProfitabilities: List<ObjectProfitabilityItem> = emptyList(),
    val globalCategoryBreakdowns: List<CategoryExpenseBreakdown> = emptyList(),
    val interObjectDebts: List<InterObjectDebtItem> = emptyList()
) {
    val totalAccruedExpenses: Double
        get() = totalWorkerSalaryEarned + totalBonusesEarned + totalOtherExpenses

    val totalExpenses: Double
        get() = totalAccruedExpenses

    val remainingReceivable: Double
        get() = (totalAgreedPrice - totalClientIncome).coerceAtLeast(0.0)

    val totalCashOutflow: Double
        get() = totalCashPaidToWorkers + totalPaidOtherExpenses

    val totalCashBalance: Double
        get() = totalReceivedIncome - totalCashOutflow

    val estimatedProfit: Double
        get() = totalAgreedPrice - totalAccruedExpenses

    val overallProfitMargin: Double
        get() = if (totalAgreedPrice > 0) (estimatedProfit / totalAgreedPrice) * 100.0 else 0.0

    val incomeCollectionRate: Double
        get() = if (totalAgreedPrice > 0) (totalClientIncome / totalAgreedPrice) * 100.0 else 0.0
}
