package uz.buildflow.app

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.buildflow.app.core.util.CurrencyFormatter

class PrivacyModeUnitTest {

    @Test
    fun testCurrencyFormatterPrivacyMasking() {
        val amount = 1_500_000.0

        // 1. Maxfiylik o'chiq bo'lganda (isPrivacyMode = false)
        val normalFormatted = CurrencyFormatter.formatAmount(amount, isPrivacyMode = false)
        val normalShortFormatted = CurrencyFormatter.formatAmountShort(amount, isPrivacyMode = false)

        assertEquals("1 500 000 so'm", normalFormatted)
        assertEquals("1.5 mln", normalShortFormatted)

        // 2. Maxfiylik yoqilganda (isPrivacyMode = true)
        val maskedFormatted = CurrencyFormatter.formatAmount(amount, isPrivacyMode = true)
        val maskedShortFormatted = CurrencyFormatter.formatAmountShort(amount, isPrivacyMode = true)

        assertEquals("•••••• so'm", maskedFormatted)
        assertEquals("••••••", maskedShortFormatted)
    }

    @Test
    fun testZeroAndNegativeAmountsWithPrivacyMode() {
        val zeroAmount = 0.0
        val negativeAmount = -250_000.0

        assertEquals("•••••• so'm", CurrencyFormatter.formatAmount(zeroAmount, isPrivacyMode = true))
        assertEquals("••••••", CurrencyFormatter.formatAmountShort(zeroAmount, isPrivacyMode = true))

        assertEquals("•••••• so'm", CurrencyFormatter.formatAmount(negativeAmount, isPrivacyMode = true))
        assertEquals("••••••", CurrencyFormatter.formatAmountShort(negativeAmount, isPrivacyMode = true))
    }
}
