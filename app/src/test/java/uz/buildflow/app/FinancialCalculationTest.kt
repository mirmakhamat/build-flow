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
        assertEquals(0.0, stats.workerDebtToUs, 0.0)
        assertEquals(-3_300_000.0, stats.netBalance, 0.0)

        // Ortiqcha to'langan holat (Avans)
        val overpaidStats = stats.copy(totalPaid = 7_000_000.0)
        assertEquals(0.0, overpaidStats.remainingDebtToWorker, 0.0)
        assertEquals(700_000.0, overpaidStats.workerDebtToUs, 0.0)
        assertEquals(700_000.0, overpaidStats.netBalance, 0.0)
    }

    @Test
    fun testCurrencyFormatting() {
        assertEquals("250 000 so'm", CurrencyFormatter.formatAmount(250000.0))
        assertEquals("150 000 000 so'm", CurrencyFormatter.formatAmount(150000000.0))
        assertEquals("150 mln", CurrencyFormatter.formatAmountShort(150000000.0))
        assertEquals("250 k", CurrencyFormatter.formatAmountShort(250000.0))
    }

    @Test
    fun testBulkPayoutSettlement() {
        // Ishchining 3 ta to'lanmagan kuni bor: 200k, 200k, 200k (jami 600k)
        val unpaidDayRates = listOf(200_000.0, 200_000.0, 200_000.0)
        val payoutAmount = 450_000.0 // Foydalanuvchi 450k to'ladi

        var remainingBudget = payoutAmount
        var closedDaysCount = 0

        for (dayRate in unpaidDayRates) {
            if (remainingBudget >= dayRate) {
                closedDaysCount++
                remainingBudget -= dayRate
            } else {
                break
            }
        }

        // 2 ta kun to'liq yopildi (400k), 50k esa ortib qoldi (qisman yoki keyingisiga)
        assertEquals(2, closedDaysCount)
        assertEquals(50_000.0, remainingBudget, 0.0)
    }

    @Test
    fun testPartialSalaryAndBonusFifoSettlement() {
        // Misol: 250k stavka va 100k bonus (Jami 350k kutilmoqda)
        val day = uz.buildflow.app.domain.model.WorkerDay(
            id = "d1",
            workerId = "w1",
            date = "2026-08-27",
            status = uz.buildflow.app.domain.model.AttendanceStatus.WORKED,
            paymentAmount = 250_000.0
        )
        val bonus = uz.buildflow.app.domain.model.GeneralBonus(
            id = "b1",
            workerId = "w1",
            objectId = "obj1",
            amount = 100_000.0,
            date = "2026-08-27",
            reason = "Erta bitirgani uchun"
        )

        // 1. Agar 50k to'lansa:
        // - Asosiy stavkadan: 50k to'lanadi, 200k qarz qoladi
        // - Bonusdan: 0k to'lanadi, 100k qarz qoladi
        // - Jami shu kunda qolgan qarz: 300k
        val payment50k = listOf(
            uz.buildflow.app.domain.model.WorkerPayment(id = "p1", workerId = "w1", objectId = "obj1", amount = 50_000.0, date = "2026-08-27")
        )
        val (settlementMap50k, _) = uz.buildflow.app.presentation.workers.computeSettlementMap(listOf(day), payment50k, listOf(bonus))
        val info50k = settlementMap50k["2026-08-27"]!!
        assertEquals(300_000.0, info50k.totalRemainingDebt, 0.0)
        assertEquals(200_000.0, info50k.salaryDebt, 0.0)
        assertEquals(100_000.0, info50k.bonusDebt, 0.0)
        assertEquals(false, info50k.isFullyPaid)

        // 2. Agar 150k to'lansa:
        // - Asosiy stavkadan: 150k to'lanadi, 100k qarz qoladi
        // - Bonusdan: 0k to'lanadi, 100k qarz qoladi
        // - Jami shu kunda qolgan qarz: 200k
        val payment150k = listOf(
            uz.buildflow.app.domain.model.WorkerPayment(id = "p2", workerId = "w1", objectId = "obj1", amount = 150_000.0, date = "2026-08-27")
        )
        val (settlementMap150k, _) = uz.buildflow.app.presentation.workers.computeSettlementMap(listOf(day), payment150k, listOf(bonus))
        val info150k = settlementMap150k["2026-08-27"]!!
        assertEquals(200_000.0, info150k.totalRemainingDebt, 0.0)
        assertEquals(100_000.0, info150k.salaryDebt, 0.0)
        assertEquals(100_000.0, info150k.bonusDebt, 0.0)
        assertEquals(false, info150k.isFullyPaid)

        // 3. Agar 250k to'lansa:
        // - Asosiy stavka to'liq yopiladi (0 qarz)
        // - Bonusdan 100k qarz qoladi
        // - Jami shu kunda qolgan qarz: 100k
        val payment250k = listOf(
            uz.buildflow.app.domain.model.WorkerPayment(id = "p3", workerId = "w1", objectId = "obj1", amount = 250_000.0, date = "2026-08-27")
        )
        val (settlementMap250k, _) = uz.buildflow.app.presentation.workers.computeSettlementMap(listOf(day), payment250k, listOf(bonus))
        val info250k = settlementMap250k["2026-08-27"]!!
        assertEquals(100_000.0, info250k.totalRemainingDebt, 0.0)
        assertEquals(0.0, info250k.salaryDebt, 0.0)
        assertEquals(100_000.0, info250k.bonusDebt, 0.0)
        assertEquals(false, info250k.isFullyPaid)

        // 4. Agar 350k to'lansa:
        // - Barchasi to'liq yopiladi (0 qarz) -> isFullyPaid = true
        val payment350k = listOf(
            uz.buildflow.app.domain.model.WorkerPayment(id = "p4", workerId = "w1", objectId = "obj1", amount = 350_000.0, date = "2026-08-27")
        )
        val (settlementMap350k, _) = uz.buildflow.app.presentation.workers.computeSettlementMap(listOf(day), payment350k, listOf(bonus))
        val info350k = settlementMap350k["2026-08-27"]!!
        assertEquals(0.0, info350k.totalRemainingDebt, 0.0)
        assertEquals(0.0, info350k.salaryDebt, 0.0)
        assertEquals(0.0, info350k.bonusDebt, 0.0)
        assertEquals(true, info350k.isFullyPaid)
    }
}

