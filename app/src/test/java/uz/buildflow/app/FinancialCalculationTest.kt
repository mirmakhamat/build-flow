package uz.buildflow.app

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.buildflow.app.core.util.CurrencyFormatter
import uz.buildflow.app.domain.model.CategoryExpenseBreakdown
import uz.buildflow.app.domain.model.ObjectFinancialSummary
import uz.buildflow.app.domain.model.WorkerStats

class FinancialCalculationTest {

    @Test
    fun testObjectFinancialFormulas() {
        val summary = ObjectFinancialSummary(
            objectId = "obj-1",
            objectName = "Chilonzor 12-uy remonti",
            totalPrice = 150_000_000.0,
            totalReceivedIncome = 100_000_000.0,
            totalWorkerSalary = 40_000_000.0,
            totalDailyBonuses = 2_000_000.0,
            totalGeneralBonuses = 3_000_000.0,
            totalPaidToWorkers = 30_000_000.0,
            totalOtherExpenses = 15_000_000.0,
            categoryBreakdowns = listOf(
                CategoryExpenseBreakdown("Yo'l kira", 5_000_000.0),
                CategoryExpenseBreakdown("Material", 8_000_000.0),
                CategoryExpenseBreakdown("Santexnika", 2_000_000.0)
            ),
            totalWorkerCount = 12,
            totalWorkDaysCount = 183
        )

        // 1. Jami bonuslar = 2 mln + 3 mln = 5 mln
        assertEquals(5_000_000.0, summary.totalBonuses, 0.0)

        // 2. Boshqa xarajatlar = 15 mln
        assertEquals(15_000_000.0, summary.totalOtherExpenses, 0.0)

        // 3. Jami hisoblangan xarajat = 40 mln (ish haqi) + 5 mln (bonus) + 15 mln (boshqa) = 60 mln
        assertEquals(60_000_000.0, summary.totalExpenses, 0.0)

        // 4. Kutilayotgan pul = 150 mln - 100 mln = 50 mln
        assertEquals(50_000_000.0, summary.remainingReceivable, 0.0)

        // 5. Qo'ldagi pul (Kassa) = 100 mln (tushgan) - (30 mln berilgan ish haqi + 15 mln xarajat) = 55 mln
        assertEquals(55_000_000.0, summary.cashBalance, 0.0)

        // 6. Ishchilarga qolgan qarz = 45 mln (ish haqi + bonus) - 30 mln (to'langan) = 15 mln
        assertEquals(15_000_000.0, summary.totalWorkerDebt, 0.0)

        // 7. Taxminiy foyda = 150 mln (obyekt narxi) - 60 mln (jami xarajat) = 90 mln
        assertEquals(90_000_000.0, summary.estimatedProfit, 0.0)
    }

    @Test
    fun testWorkerStatsCalculation() {
        val stats = WorkerStats(
            workerId = "worker-1",
            workerName = "Ali Valiyev",
            position = "Usta",
            defaultRate = 250_000.0,
            workedDaysCount = 18,
            totalSalaryEarned = 4_800_000.0,
            totalDailyBonuses = 500_000.0,
            totalGeneralBonuses = 1_000_000.0,
            totalPaid = 3_000_000.0
        )

        assertEquals(6_300_000.0, stats.totalEarned, 0.0)
        assertEquals(3_300_000.0, stats.remainingDebtToWorker, 0.0)
    }

    @Test
    fun testCurrencyFormatting() {
        assertEquals("250 000 so'm", CurrencyFormatter.formatAmount(250000.0))
        assertEquals("150 000 000 so'm", CurrencyFormatter.formatAmount(150000000.0))
        assertEquals("150 mln", CurrencyFormatter.formatAmountShort(150000000.0))
        assertEquals("250 k", CurrencyFormatter.formatAmountShort(250000.0))
    }
}
