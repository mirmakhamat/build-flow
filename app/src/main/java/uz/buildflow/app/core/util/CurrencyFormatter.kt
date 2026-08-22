package uz.buildflow.app.core.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {
    private val symbols = DecimalFormatSymbols(Locale.US).apply {
        groupingSeparator = ' '
    }
    private val decimalFormat = DecimalFormat("#,###", symbols)

    fun formatAmount(amount: Double): String {
        return "${decimalFormat.format(amount.toLong())} so'm"
    }

    fun formatAmountShort(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> {
                val value = amount / 1_000_000_000.0
                String.format(Locale.US, "%.1f mlrd", value).replace(".0", "")
            }
            amount >= 1_000_000 -> {
                val value = amount / 1_000_000.0
                String.format(Locale.US, "%.1f mln", value).replace(".0", "")
            }
            amount >= 1_000 -> {
                val value = amount / 1_000.0
                String.format(Locale.US, "%.1f k", value).replace(".0", "")
            }
            else -> decimalFormat.format(amount.toLong())
        }
    }
}
