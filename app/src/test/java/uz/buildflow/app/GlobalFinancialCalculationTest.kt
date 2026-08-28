package uz.buildflow.app

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.buildflow.app.domain.model.CategoryExpenseBreakdown
import uz.buildflow.app.domain.model.GlobalFinancialSummary
import uz.buildflow.app.domain.model.ObjectFinancialSummary
import uz.buildflow.app.domain.model.ObjectProfitabilityItem
import uz.buildflow.app.domain.model.ObjectStatus

class GlobalFinancialCalculationTest {

    @Test
    fun testGlobalConsolidatedFormulas() {
        val obj1 = ObjectFinancialSummary(
            objectId = "obj-1",
            objectName = "Chilonzor Villa",
            totalPrice = 200_000_000.0,
            totalReceivedIncome = 120_000_000.0,
            totalClientIncome = 120_000_000.0,
            totalWorkerSalary = 50_000_000.0,
            totalDailyBonuses = 5_000_000.0,
            totalGeneralBonuses = 5_000_000.0,
            totalPaidToWorkers = 40_000_000.0,
            totalCashPaidToWorkers = 40_000_000.0,
            totalOtherExpenses = 20_000_000.0,
            totalPaidOtherExpenses = 20_000_000.0,
            totalPaidFromOwnPocketForThisWorkers = 0.0,
            totalExpensesPaidFromOwnPocket = 0.0,
            totalWorkerCount = 10,
            totalWorkDaysCount = 150
        )

        val obj2 = ObjectFinancialSummary(
            objectId = "obj-2",
            objectName = "Yunusobod Kvartira",
            totalPrice = 100_000_000.0,
            totalReceivedIncome = 80_000_000.0,
            totalClientIncome = 80_000_000.0,
            totalWorkerSalary = 25_000_000.0,
            totalDailyBonuses = 2_000_000.0,
            totalGeneralBonuses = 3_000_000.0,
            totalPaidToWorkers = 20_000_000.0,
            totalCashPaidToWorkers = 20_000_000.0,
            totalOtherExpenses = 10_000_000.0,
            totalPaidOtherExpenses = 10_000_000.0,
            totalPaidFromOwnPocketForThisWorkers = 2_000_000.0,
            totalExpensesPaidFromOwnPocket = 3_000_000.0,
            totalWorkerCount = 6,
            totalWorkDaysCount = 80
        )

        val globalSummary = GlobalFinancialSummary(
            totalObjectsCount = 2,
            activeObjectsCount = 2,
            completedObjectsCount = 0,
            totalAgreedPrice = obj1.totalPrice + obj2.totalPrice, // 300 mln
            totalReceivedIncome = obj1.totalReceivedIncome + obj2.totalReceivedIncome, // 200 mln
            totalClientIncome = obj1.totalClientIncome + obj2.totalClientIncome, // 200 mln
            totalWorkerSalaryEarned = obj1.totalWorkerSalary + obj2.totalWorkerSalary, // 75 mln
            totalBonusesEarned = obj1.totalBonuses + obj2.totalBonuses, // 15 mln
            totalOtherExpenses = obj1.totalOtherExpenses + obj2.totalOtherExpenses, // 30 mln
            totalPaidToWorkers = obj1.totalPaidToWorkers + obj2.totalPaidToWorkers, // 60 mln
            totalCashPaidToWorkers = obj1.totalCashPaidToWorkers + obj2.totalCashPaidToWorkers, // 60 mln
            totalPaidOtherExpenses = obj1.totalPaidOtherExpenses + obj2.totalPaidOtherExpenses, // 30 mln
            totalPaidFromOwnPocket = obj1.totalPaidFromOwnPocket + obj2.totalPaidFromOwnPocket, // 5 mln
            totalWorkerDebt = obj1.totalWorkerDebt + obj2.totalWorkerDebt, // (60 - 40) + (30 - 20) = 30 mln
            totalWorkerCount = 16,
            totalWorkDaysCount = 230,
            objectSummaries = listOf(obj1, obj2)
        )

        // 1. Jami Hisoblangan Xarajatlar = 75 mln (ish haqi) + 15 mln (bonus) + 30 mln (boshqa) = 120 mln
        assertEquals(120_000_000.0, globalSummary.totalExpenses, 0.0)

        // 2. Kutilayotgan qoldiq tushum = 300 mln - 200 mln = 100 mln
        assertEquals(100_000_000.0, globalSummary.remainingReceivable, 0.0)

        // 3. Jami Kassadan Chiqim = 60 mln (ishchilarga) + 30 mln (xarajat) = 90 mln
        assertEquals(90_000_000.0, globalSummary.totalCashOutflow, 0.0)

        // 4. Jami Erkin Kassa Balansi = 200 mln (tushgan) - 90 mln (chiqqan) = 110 mln
        assertEquals(110_000_000.0, globalSummary.totalCashBalance, 0.0)

        // 5. Jami Kutilayotgan Sof Foyda = 300 mln (shartnoma) - 120 mln (xarajat) = 180 mln
        assertEquals(180_000_000.0, globalSummary.estimatedProfit, 0.0)

        // 6. Umumiy Rentabellik Foizi = (180 mln / 300 mln) * 100 = 60.0%
        assertEquals(60.0, globalSummary.overallProfitMargin, 0.001)

        // 7. Tushum Yig'ilish Foizi = (200 mln / 300 mln) * 100 = 66.666%
        assertEquals(66.6666, globalSummary.incomeCollectionRate, 0.001)

        // 8. O'z hisobidan jami to'langan = 5 mln
        assertEquals(5_000_000.0, globalSummary.totalPaidFromOwnPocket, 0.0)
    }
}
