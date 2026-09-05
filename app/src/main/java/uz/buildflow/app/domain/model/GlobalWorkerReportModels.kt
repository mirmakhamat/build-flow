package uz.buildflow.app.domain.model

data class GlobalWorkersReportSummary(
    val totalWorkersCount: Int,
    val totalEarnedAll: Double,
    val totalPaidAll: Double,
    val totalDebtAll: Double,
    val totalAdvanceAll: Double,
    val workers: List<WorkerGlobalReportItem>
)

data class WorkerGlobalReportItem(
    val workerId: String,
    val workerName: String,
    val position: String,
    val phone: String?,
    val status: String,
    val defaultRate: Double,
    val workedDaysCount: Int,
    val totalEarned: Double,
    val totalPaid: Double,
    val balance: Double, // > 0 => Qarz, < 0 => Avans, == 0 => Yopilgan
    val workedObjects: List<String>,
    val daysHistory: List<WorkerDayDetailRecord>,
    val paymentsHistory: List<WorkerPaymentDetailRecord>,
    val objectsBreakdown: List<WorkerObjectBreakdownItem>
)

data class WorkerDayDetailRecord(
    val date: String,
    val objectId: String,
    val objectName: String,
    val dailyRate: Double,
    val bonusAmount: Double,
    val bonusReason: String?,
    val totalDayAmount: Double,
    val isFullyCovered: Boolean,
    val remainingDebt: Double
)

data class WorkerPaymentDetailRecord(
    val paymentId: String,
    val date: String,
    val amount: Double,
    val objectId: String,
    val objectName: String,
    val paymentSource: String, // "KASSA" yoki "OWN_POCKET"
    val note: String?
)

data class WorkerObjectBreakdownItem(
    val objectId: String,
    val objectName: String,
    val daysCount: Int,
    val earnedAmount: Double,
    val paidAmount: Double
)

enum class WorkerReportFilter {
    ALL,
    HAS_DEBT,
    FULLY_PAID,
    HAS_ADVANCE
}

enum class WorkerReportSort {
    NAME_ASC,
    DEBT_DESC,
    EARNED_DESC,
    DAYS_DESC
}
