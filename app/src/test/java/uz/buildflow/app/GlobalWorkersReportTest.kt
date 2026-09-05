package uz.buildflow.app

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.buildflow.app.domain.model.WorkerDayDetailRecord
import uz.buildflow.app.domain.model.WorkerGlobalReportItem
import uz.buildflow.app.domain.model.WorkerObjectBreakdownItem

class GlobalWorkersReportTest {

    @Test
    fun testWorkerFinancialCalculations() {
        val days = listOf(
            WorkerDayDetailRecord(
                date = "2026-09-01",
                objectId = "obj1",
                objectName = "Novza City",
                dailyRate = 400000.0,
                bonusAmount = 50000.0,
                bonusReason = "Sement",
                totalDayAmount = 450000.0,
                isFullyCovered = true,
                remainingDebt = 0.0
            ),
            WorkerDayDetailRecord(
                date = "2026-09-02",
                objectId = "obj2",
                objectName = "Samarqand Dacha",
                dailyRate = 350000.0,
                bonusAmount = 0.0,
                bonusReason = null,
                totalDayAmount = 350000.0,
                isFullyCovered = false,
                remainingDebt = 200000.0
            )
        )

        val totalEarned = days.sumOf { it.totalDayAmount }
        assertEquals(800000.0, totalEarned, 0.01)

        val totalPaid = 600000.0
        val balance = totalEarned - totalPaid
        assertEquals(200000.0, balance, 0.01)

        val reportItem = WorkerGlobalReportItem(
            workerId = "w1",
            workerName = "Alisher Qodirov",
            position = "Usta",
            phone = "+998901234567",
            status = "ACTIVE",
            defaultRate = 400000.0,
            workedDaysCount = days.size,
            totalEarned = totalEarned,
            totalPaid = totalPaid,
            balance = balance,
            workedObjects = listOf("Novza City", "Samarqand Dacha"),
            daysHistory = days,
            paymentsHistory = emptyList(),
            objectsBreakdown = listOf(
                WorkerObjectBreakdownItem("obj1", "Novza City", 1, 450000.0, 450000.0),
                WorkerObjectBreakdownItem("obj2", "Samarqand Dacha", 1, 350000.0, 150000.0)
            )
        )

        assertEquals(2, reportItem.workedDaysCount)
        assertEquals(2, reportItem.workedObjects.size)
        assertEquals(200000.0, reportItem.balance, 0.01)
    }
}
