package com.example.domain.model

data class DebtLedgerEntry(
    val id: String,
    val debtAccountId: String = "",
    val debtId: String,
    val personId: String,
    val type: DebtType, // RECEIVABLE (owed to user) or PAYABLE (user owes)
    val isPayment: Boolean = false,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val description: String = "",
    val category: String = "General",
    val paymentMethod: String = "Cash",
    val accountId: String? = null,
    val status: String = "CLEARED"
)

data class PersonDebtAccount(
    val id: String = "", // DebtAccountID
    val personId: String = "",
    val person: Person,
    val currency: String = "SAR",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val mainDebt: Debt,
    val entries: List<DebtLedgerEntry> = emptyList()
) {
    val totalOriginalAmount: Double get() = mainDebt.originalAmount
    val totalRemainingAmount: Double get() = mainDebt.remainingAmount
    val totalPaidAmount: Double get() = (mainDebt.originalAmount - mainDebt.remainingAmount).coerceAtLeast(0.0)
    val progressPercentage: Float get() = mainDebt.progressPercentage
    val lastTransactionDate: Long get() = entries.maxOfOrNull { it.date } ?: mainDebt.createdAt
    val transactionCount: Int get() = entries.size.coerceAtLeast(1)
}
