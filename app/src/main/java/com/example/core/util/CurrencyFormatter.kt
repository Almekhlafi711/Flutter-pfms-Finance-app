package com.example.core.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    fun format(amount: Double, currency: String = "SAR"): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        val formattedNumber = String.format(Locale.US, "%,.2f", amount)
        return "$formattedNumber $currency"
    }

    fun formatCompact(amount: Double, currency: String = "SAR"): String {
        return when {
            amount >= 1_000_000 -> String.format(Locale.US, "%.2fM %s", amount / 1_000_000, currency)
            amount >= 10_000 -> String.format(Locale.US, "%.1fK %s", amount / 1_000, currency)
            else -> format(amount, currency)
        }
    }
}
