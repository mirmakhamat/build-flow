package uz.buildflow.app

import org.junit.Assert.*
import org.junit.Test
import uz.buildflow.app.data.local.entity.WorkerEntity
import uz.buildflow.app.domain.model.DuplicateWorkerGroup
import uz.buildflow.app.domain.model.MergeResult
import uz.buildflow.app.domain.model.WorkerCandidateInfo

class MergeWorkersTest {

    private fun normalizeName(name: String): String {
        return name
            .lowercase()
            .replace(Regex("\\s*\\([^)]*\\)"), "")
            .replace(Regex("[^a-zа-яёўқғҳ0-9]"), "")
            .trim()
    }

    private fun normalizePhone(phone: String?): String? {
        if (phone == null) return null
        val digits = phone.replace(Regex("[^0-9]"), "")
        return if (digits.length >= 7) digits else null
    }

    @Test
    fun testNameNormalizationDetectsDuplicates() {
        val name1 = "Rustam Toshmatov"
        val name2 = "rustam toshmatov (usta)"
        val name3 = "  Rustam   Toshmatov  "

        assertEquals(normalizeName(name1), normalizeName(name2))
        assertEquals(normalizeName(name1), normalizeName(name3))
    }

    @Test
    fun testPhoneNormalization() {
        val phone1 = "+998 (90) 123-45-67"
        val phone2 = "998901234567"
        val phone3 = "901234567"

        assertEquals("998901234567", normalizePhone(phone1))
        assertEquals("998901234567", normalizePhone(phone2))
        assertEquals("901234567", normalizePhone(phone3))
        assertNull(normalizePhone("123")) // Less than 7 digits
    }

    @Test
    fun testSuggestedMasterSelection() {
        val worker1 = WorkerCandidateInfo(
            worker = WorkerEntity("w1", "obj1", "Rustam Toshmatov", "+998901234567", "Usta", 200000.0, "ACTIVE", "2026-01-01", null, null, 1000L, 1000L),
            objectName = "Novza City",
            daysWorkedCount = 15,
            totalEarned = 3000000.0,
            totalPaid = 2000000.0,
            balance = 1000000.0
        )
        val worker2 = WorkerCandidateInfo(
            worker = WorkerEntity("w2", "obj2", "Rustam Toshmatov", null, null, 250000.0, "ACTIVE", "2026-02-01", null, null, 2000L, 2000L),
            objectName = "Samarqand Dacha",
            daysWorkedCount = 5,
            totalEarned = 1250000.0,
            totalPaid = 1250000.0,
            balance = 0.0
        )

        val group = listOf(worker1, worker2)
        val suggestedMaster = group.maxWithOrNull(
            compareBy<WorkerCandidateInfo> { it.daysWorkedCount }
                .thenBy { it.totalEarned }
        )?.worker?.id

        assertEquals("w1", suggestedMaster)
    }

    @Test
    fun testMergeResultCreation() {
        val result = MergeResult(
            targetWorkerId = "w1",
            mergedWorkerIds = listOf("w2", "w3"),
            daysMergedCount = 20,
            paymentsMergedCount = 4,
            bonusesMergedCount = 2
        )

        assertEquals("w1", result.targetWorkerId)
        assertEquals(2, result.mergedWorkerIds.size)
        assertEquals(20, result.daysMergedCount)
        assertEquals(4, result.paymentsMergedCount)
        assertEquals(2, result.bonusesMergedCount)
    }
}
