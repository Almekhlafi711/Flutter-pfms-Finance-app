package com.example.domain.model

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val balance: Double,
    val currency: String = "SAR",
    val accountNumber: String = "",
    val colorHex: String = "#0EA5E9",
    val iconName: String = "bank",
    val isArchived: Boolean = false
)

data class GroupedAccount(
    val name: String,
    val type: AccountType,
    val colorHex: String,
    val iconName: String,
    val isArchived: Boolean,
    val accounts: List<Account>
)

enum class AccountType {
    CASH, BANK, WALLET, SAVINGS, CREDIT_CARD, INVESTMENT, CRYPTO
}

data class Transaction(
    val id: String,
    val type: TransactionType,
    val amount: Double,
    val currency: String = "SAR",
    val sourceAccountId: String,
    val destinationAccountId: String? = null,
    val category: String,
    val party: String? = null,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val relatedEntityId: String? = null,
    val status: String = "POSTED"
)

enum class TransactionType {
    INCOME, EXPENSE, TRANSFER, ASSET_PURCHASE, ASSET_SALE, DEBT_PAYMENT, DEBT_CREATION, GOAL_CONTRIBUTION, BILL_PAYMENT
}

enum class AssetStatus {
    ACTIVE, SOLD
}

data class Asset(
    val id: String,
    val name: String,
    val type: AssetType,
    val purchaseValue: Double,
    val currentValue: Double,
    val quantity: Double = 1.0,
    val unit: String = "Unit",
    val currency: String = "SAR",
    val notes: String = "",
    val purchaseDate: Long = System.currentTimeMillis(),
    val status: AssetStatus = AssetStatus.ACTIVE,
    val purchaseAccountId: String = "",
    val purchaseAccountName: String = "",
    val soldPrice: Double? = null,
    val soldDate: Long? = null,
    val soldAccountId: String? = null,
    val soldAccountName: String? = null
) {
    val totalCurrentValue: Double get() = if (status == AssetStatus.SOLD) (soldPrice ?: currentValue) * quantity else currentValue * quantity
    val totalPurchaseValue: Double get() = purchaseValue * quantity
    val netGainLoss: Double get() = if (status == AssetStatus.SOLD) ((soldPrice ?: currentValue) - purchaseValue) * quantity else totalCurrentValue - totalPurchaseValue

    fun getLocalizedName(isArabic: Boolean): String {
        if (!isArabic) return name
        return when (name.trim()) {
            "Riyadh Villa Quarter" -> "حي فيلا الرياض"
            "Toyota Camry 2024" -> "تويوتا كامري 2024"
            "Gold Bullion Bar" -> "سبيكة ذهب صافي"
            else -> name
        }
    }
}

enum class AssetLogType {
    PURCHASE, VALUE_UPDATE, SALE
}

data class AssetLog(
    val id: String,
    val assetId: String,
    val type: AssetLogType,
    val title: String,
    val amount: Double,
    val previousValue: Double = 0.0,
    val newValue: Double = 0.0,
    val accountName: String = "",
    val date: Long = System.currentTimeMillis(),
    val notes: String = ""
)

enum class AssetType {
    REAL_ESTATE, VEHICLE, GOLD, STOCKS, CRYPTO, PROJECT, COLLECTIBLE, ELECTRONICS, BUSINESS, INVESTMENTS, OTHER
}

data class Debt(
    val id: String,
    val debtAccountId: String = "",
    val personId: String,
    val partyName: String = "", // Kept for legacy/fallback if needed, or we just rely on personId
    val partyPhone: String? = null,
    val type: DebtType,
    val originalAmount: Double,
    val remainingAmount: Double,
    val currency: String = "SAR",
    val dueDate: Long,
    val status: DebtStatus = DebtStatus.ACTIVE,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val progressPercentage: Float
        get() = if (originalAmount > 0) ((originalAmount - remainingAmount) / originalAmount).toFloat().coerceIn(0f, 1f) else 0f
}

enum class DebtType {
    RECEIVABLE, PAYABLE // RECEIVABLE = user is owed money; PAYABLE = user owes money
}

enum class DebtStatus {
    ACTIVE, PARTIAL, COMPLETED, ARCHIVED
}

data class Budget(
    val id: String,
    val category: String,
    val monthlyLimit: Double,
    val spentAmount: Double = 0.0,
    val currency: String = "SAR",
    val period: String = "MONTHLY",
    val accountId: String? = null
) {
    val usagePercentage: Float
        get() = if (monthlyLimit > 0) (spentAmount / monthlyLimit).toFloat().coerceIn(0f, 2f) else 0f
}

data class Goal(
    val id: String,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val currency: String = "SAR",
    val targetDate: Long,
    val iconName: String = "target",
    val colorHex: String = "#38BDF8",
    val isCompleted: Boolean = false
) {
    val progressPercentage: Float
        get() = if (targetAmount > 0) (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f
}

data class Bill(
    val id: String,
    val title: String,
    val amount: Double,
    val currency: String = "SAR",
    val frequency: String = "MONTHLY",
    val nextDueDate: Long,
    val category: String = "Utilities",
    val accountId: String = "",
    val status: BillStatus = BillStatus.SCHEDULED,
    val isAutoPay: Boolean = false
)

enum class BillStatus {
    SCHEDULED, UPCOMING, DUE, PAID, OVERDUE
}

data class NetWorthSummary(
    val totalCashAndAccounts: Double,
    val totalAssetsValue: Double,
    val totalReceivables: Double,
    val totalLiabilitiesAndPayables: Double,
    val currency: String = "SAR"
) {
    val netWorth: Double
        get() = (totalCashAndAccounts + totalAssetsValue + totalReceivables) - totalLiabilitiesAndPayables
}
