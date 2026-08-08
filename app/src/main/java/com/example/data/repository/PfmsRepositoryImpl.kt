package com.example.data.repository

import com.example.core.database.AppDatabase
import com.example.core.database.entities.*
import com.example.domain.model.*
import com.example.domain.repository.PfmsRepository
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class PfmsRepositoryImpl(private val db: AppDatabase) : PfmsRepository {

    override fun getAccounts(): Flow<List<Account>> {
        return db.accountDao().getAllAccounts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactions(): Flow<List<Transaction>> {
        return db.transactionDao().getAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getTransactionsForAccount(accountId: String): Flow<List<Transaction>> {
        return db.transactionDao().getTransactionsForAccount(accountId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAssets(): Flow<List<Asset>> {
        return db.assetDao().getAllAssets().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getDebts(): Flow<List<Debt>> {
        return db.debtDao().getAllDebts().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBudgets(): Flow<List<Budget>> {
        // Calculate spent amount dynamically from transactions for each budget category
        return combine(
            db.budgetDao().getAllBudgets(),
            db.transactionDao().getAllTransactions()
        ) { budgets, transactions ->
            budgets.map { b ->
                val spent = transactions
                    .filter { it.type == TransactionType.EXPENSE.name && it.category.equals(b.category, ignoreCase = true) }
                    .sumOf { it.amount }
                b.toDomain().copy(spentAmount = spent)
            }
        }
    }

    override fun getGoals(): Flow<List<Goal>> {
        return db.goalDao().getAllGoals().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBills(): Flow<List<Bill>> {
        return db.billDao().getAllBills().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addAccount(account: Account) {
        if (account.name.isBlank()) {
            throw IllegalArgumentException("اسم الحساب لا يمكن أن يكون فارغاً / Account name cannot be empty")
        }
        if (account.balance < 0.0 && account.type != AccountType.CREDIT_CARD) {
            throw IllegalArgumentException("لا يمكن بدء حساب عادي برصيد سالب / Standard account cannot start with a negative balance")
        }
        db.accountDao().insertAccount(account.toEntity())
    }

    override suspend fun updateAccount(account: Account) {
        if (account.name.isBlank()) {
            throw IllegalArgumentException("اسم الحساب لا يمكن أن يكون فارغاً / Account name cannot be empty")
        }
        db.accountDao().updateAccount(account.toEntity())
    }

    override suspend fun archiveAccount(accountId: String) {
        db.withTransaction {
            val acc = db.accountDao().getAccountById(accountId)
            acc?.let {
                db.accountDao().updateAccount(it.copy(isArchived = true))
            }
        }
    }

    override suspend fun addTransaction(transaction: Transaction) {
        if (transaction.amount <= 0.0) {
            throw IllegalArgumentException("مبلغ العملية يجب أن يكون أكبر من الصفر / Transaction amount must be greater than zero")
        }
        
        db.withTransaction {
            val sourceAccount = db.accountDao().getAccountById(transaction.sourceAccountId)
                ?: throw IllegalArgumentException("الحساب المصدر غير موجود / Source account does not exist")
            
            if (transaction.currency != sourceAccount.currency) {
                throw IllegalArgumentException("عملة العملية (${transaction.currency}) لا تطابق عملة الحساب (${sourceAccount.currency}) / Currency mismatch")
            }

            val isWithdraw = when (transaction.type) {
                TransactionType.EXPENSE, TransactionType.TRANSFER, TransactionType.GOAL_CONTRIBUTION,
                TransactionType.BILL_PAYMENT, TransactionType.ASSET_PURCHASE, TransactionType.DEBT_CREATION -> true
                else -> false
            }

            if (isWithdraw && sourceAccount.balance < transaction.amount &&
                sourceAccount.type != AccountType.CREDIT_CARD.name) {
                throw IllegalStateException("رصيد غير كافٍ في الحساب '${sourceAccount.name}' / Insufficient balance")
            }

            if (transaction.type == TransactionType.TRANSFER) {
                val destId = transaction.destinationAccountId
                    ?: throw IllegalArgumentException("يجب تحديد الحساب المستلم للتحويل / Destination account must be specified for transfer")
                if (destId == transaction.sourceAccountId) {
                    throw IllegalArgumentException("لا يمكن التحويل لنفس الحساب / Cannot transfer to the same account")
                }
                val destAccount = db.accountDao().getAccountById(destId)
                    ?: throw IllegalArgumentException("الحساب المستلم غير موجود / Destination account does not exist")
                if (sourceAccount.currency != destAccount.currency) {
                    throw IllegalArgumentException("لا يمكن التحويل بين عملات مختلفة مباشرة / Cannot transfer directly between different currencies")
                }
            }

            // Insert transaction record
            db.transactionDao().insertTransaction(transaction.toEntity())

            // Adjust account balances precisely
            val roundedAmount = Math.round(transaction.amount * 100.0) / 100.0
            when (transaction.type) {
                TransactionType.INCOME -> {
                    db.accountDao().updateBalance(transaction.sourceAccountId, roundedAmount)
                }
                TransactionType.EXPENSE, TransactionType.GOAL_CONTRIBUTION, TransactionType.BILL_PAYMENT, TransactionType.ASSET_PURCHASE -> {
                    db.accountDao().updateBalance(transaction.sourceAccountId, -roundedAmount)
                }
                TransactionType.TRANSFER -> {
                    db.accountDao().updateBalance(transaction.sourceAccountId, -roundedAmount)
                    transaction.destinationAccountId?.let { destId ->
                        db.accountDao().updateBalance(destId, roundedAmount)
                    }
                }
                TransactionType.ASSET_SALE -> {
                    db.accountDao().updateBalance(transaction.sourceAccountId, roundedAmount)
                }
                TransactionType.DEBT_PAYMENT -> {
                    // Get the debt to see if it is RECEIVABLE or PAYABLE
                    val debtId = transaction.relatedEntityId ?: throw IllegalArgumentException("معرّف الدين مطلوب لسداد الدين / Debt ID is required for debt payment")
                    val debt = db.debtDao().getDebtById(debtId) ?: throw IllegalArgumentException("الدين غير موجود / Debt not found")
                    val delta = if (debt.type == DebtType.RECEIVABLE.name) roundedAmount else -roundedAmount
                    db.accountDao().updateBalance(transaction.sourceAccountId, delta)
                }
                TransactionType.DEBT_CREATION -> {
                    db.accountDao().updateBalance(transaction.sourceAccountId, -roundedAmount)
                }
            }
        }
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        db.transactionDao().insertTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(id: String) {
        db.transactionDao().deleteTransaction(id)
    }

    override suspend fun addAsset(asset: Asset) {
        if (asset.name.isBlank()) {
            throw IllegalArgumentException("اسم الأصل مطلوب / Asset name cannot be empty")
        }
        if (asset.purchaseValue <= 0.0) {
            throw IllegalArgumentException("قيمة الشراء للأصل يجب أن تكون إيجابية / Purchase value must be positive")
        }
        if (asset.quantity <= 0.0) {
            throw IllegalArgumentException("كمية الأصل يجب أن تكون أكبر من الصفر / Quantity must be greater than zero")
        }
        db.assetDao().insertAsset(asset.toEntity())
    }

    override suspend fun updateAsset(asset: Asset) {
        if (asset.name.isBlank()) {
            throw IllegalArgumentException("اسم الأصل مطلوب / Asset name cannot be empty")
        }
        db.assetDao().updateAsset(asset.toEntity())
    }

    override suspend fun deleteAsset(id: String) {
        db.assetDao().deleteAsset(id)
    }

    override fun getAssetLogs(assetId: String): Flow<List<AssetLog>> {
        return db.assetLogDao().getLogsForAsset(assetId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addAssetLog(log: AssetLog) {
        db.assetLogDao().insertLog(log.toEntity())
    }

    override fun getPersons(): Flow<List<Person>> {
        return db.personDao().getAllPersons().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addPerson(person: Person) {
        if (person.name.isBlank()) {
            throw IllegalArgumentException("اسم الشخص مطلوب / Person name cannot be empty")
        }
        db.personDao().insertPerson(person.toEntity())
    }

    override suspend fun updatePerson(person: Person) {
        if (person.name.isBlank()) {
            throw IllegalArgumentException("اسم الشخص مطلوب / Person name cannot be empty")
        }
        db.personDao().updatePerson(person.toEntity())
    }

    override fun getDebtAccounts(): Flow<List<PersonDebtAccountEntity>> {
        return db.personDebtAccountDao().getAllDebtAccounts()
    }

    override suspend fun addDebtAccount(account: PersonDebtAccountEntity) {
        db.personDebtAccountDao().insertDebtAccount(account)
    }

    override suspend fun updateDebtAccount(account: PersonDebtAccountEntity) {
        db.personDebtAccountDao().updateDebtAccount(account)
    }

    override suspend fun addDebt(debt: Debt) {
        if (debt.partyName.isBlank()) {
            throw IllegalArgumentException("اسم العميل أو الجهة مطلوب / Party name cannot be empty")
        }
        if (debt.originalAmount < 0.0 || debt.remainingAmount < 0.0) {
            throw IllegalArgumentException("مبلغ الدين لا يمكن أن يكون سالباً / Debt amount cannot be negative")
        }
        if (debt.remainingAmount > debt.originalAmount) {
            throw IllegalArgumentException("المبلغ المتبقي لا يمكن أن يتجاوز مبلغ الدين الأصلي / Remaining amount cannot exceed original amount")
        }
        db.debtDao().insertDebt(debt.toEntity())
    }

    override suspend fun recordDebtPayment(debtId: String, paymentAmount: Double, accountId: String) {
        if (paymentAmount <= 0.0) {
            throw IllegalArgumentException("مبلغ السداد يجب أن يكون إيجابياً / Payment amount must be positive")
        }
        db.withTransaction {
            val debtEntity = db.debtDao().getDebtById(debtId) 
                ?: throw IllegalArgumentException("الدين غير موجود / Debt not found")
            val debt = debtEntity.toDomain()

            if (paymentAmount > debt.remainingAmount) {
                throw IllegalArgumentException("مبلغ السداد ($paymentAmount) يتجاوز المبلغ المتبقي من الدين (${debt.remainingAmount}) / Payment exceeds remaining debt")
            }

            val updatedRemaining = (debt.remainingAmount - paymentAmount).coerceAtLeast(0.0)
            val updatedStatus = if (updatedRemaining == 0.0) DebtStatus.COMPLETED else DebtStatus.PARTIAL
            val updatedDebt = debt.copy(remainingAmount = updatedRemaining, status = updatedStatus)
            db.debtDao().insertDebt(updatedDebt.toEntity())

            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                type = TransactionType.DEBT_PAYMENT,
                amount = paymentAmount,
                currency = debt.currency,
                sourceAccountId = accountId,
                category = "Debt Settlement",
                party = debt.partyName,
                date = System.currentTimeMillis(),
                relatedEntityId = debtId,
                note = "سداد دفعة من الدين: ${debt.partyName} / Debt payment recorded"
            )
            addTransaction(tx)
        }
    }

    override suspend fun deleteDebt(id: String) {
        db.debtDao().deleteDebt(id)
    }

    override suspend fun addBudget(budget: Budget) {
        if (budget.category.isBlank()) {
            throw IllegalArgumentException("تصنيف الميزانية مطلوب / Budget category cannot be empty")
        }
        if (budget.monthlyLimit <= 0.0) {
            throw IllegalArgumentException("حد الميزانية الشهري يجب أن يكون أكبر من الصفر / Monthly limit must be positive")
        }
        db.budgetDao().insertBudget(budget.toEntity())
    }

    override suspend fun deleteBudget(id: String) {
        db.budgetDao().deleteBudget(id)
    }

    override suspend fun addGoal(goal: Goal) {
        if (goal.title.isBlank()) {
            throw IllegalArgumentException("عنوان الهدف مطلوب / Goal title cannot be empty")
        }
        if (goal.targetAmount <= 0.0) {
            throw IllegalArgumentException("المبلغ المستهدف يجب أن يكون أكبر من الصفر / Target amount must be positive")
        }
        if (goal.currentAmount < 0.0) {
            throw IllegalArgumentException("المبلغ المجمع الحالي لا يمكن أن يكون سالباً / Current amount cannot be negative")
        }
        db.goalDao().insertGoal(goal.toEntity())
    }

    override suspend fun contributeToGoal(goalId: String, amount: Double, accountId: String) {
        if (amount <= 0.0) {
            throw IllegalArgumentException("مبلغ المساهمة يجب أن يكون أكبر من الصفر / Contribution must be positive")
        }
        db.withTransaction {
            val goalEntity = db.goalDao().getGoalById(goalId)
                ?: throw IllegalArgumentException("الهدف الادخاري غير موجود / Goal not found")
            val goal = goalEntity.toDomain()

            val newCurrent = goal.currentAmount + amount
            val isCompleted = newCurrent >= goal.targetAmount
            val updatedGoal = goal.copy(currentAmount = newCurrent, isCompleted = isCompleted)
            db.goalDao().insertGoal(updatedGoal.toEntity())

            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                type = TransactionType.GOAL_CONTRIBUTION,
                amount = amount,
                currency = goal.currency,
                sourceAccountId = accountId,
                category = "Savings & Goals",
                date = System.currentTimeMillis(),
                relatedEntityId = goalId,
                note = "مساهمة في هدف ادخاري: ${goal.title} / Contribution to savings goal"
            )
            addTransaction(tx)
        }
    }

    override suspend fun deleteGoal(id: String) {
        db.goalDao().deleteGoal(id)
    }

    override suspend fun addBill(bill: Bill) {
        if (bill.title.isBlank()) {
            throw IllegalArgumentException("عنوان الفاتورة مطلوب / Bill title cannot be empty")
        }
        if (bill.amount <= 0.0) {
            throw IllegalArgumentException("مبلغ الفاتورة يجب أن يكون أكبر من الصفر / Bill amount must be positive")
        }
        db.billDao().insertBill(bill.toEntity())
    }

    override suspend fun payBill(billId: String, accountId: String) {
        db.withTransaction {
            val billEntity = db.billDao().getBillById(billId)
                ?: throw IllegalArgumentException("الفاتورة غير موجودة / Bill not found")
            val bill = billEntity.toDomain()

            val updatedBill = bill.copy(
                status = BillStatus.PAID,
                nextDueDate = bill.nextDueDate + 30L * 24 * 3600 * 1000
            )
            db.billDao().insertBill(updatedBill.toEntity())

            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                type = TransactionType.BILL_PAYMENT,
                amount = bill.amount,
                currency = bill.currency,
                sourceAccountId = accountId,
                category = bill.category,
                date = System.currentTimeMillis(),
                relatedEntityId = billId,
                note = "سداد الفاتورة: ${bill.title} / Bill paid"
            )
            addTransaction(tx)
        }
    }

    override suspend fun deleteBill(id: String) {
        db.billDao().deleteBill(id)
    }

    override suspend fun seedInitialSampleDataIfEmpty() {
        // Seed initial rich data if database accounts are empty
        val existingAccounts = db.accountDao().getAccountById("acc_rajhi")
        if (existingAccounts == null) {
            val defaultAccounts = listOf(
                AccountEntity("acc_rajhi", "Al Rajhi Bank", "BANK", 28450.00, "SAR", "**** 8842", "#0EA5E9", "bank"),
                AccountEntity("acc_cash", "Wallet Cash", "CASH", 1250.00, "SAR", "", "#10B981", "wallet"),
                AccountEntity("acc_inma", "Al Inma Savings", "SAVINGS", 45000.00, "SAR", "**** 1209", "#8B5CF6", "savings"),
                AccountEntity("acc_crypto", "Binance Crypto", "CRYPTO", 18200.00, "SAR", "BTC/ETH", "#F59E0B", "crypto")
            )
            defaultAccounts.forEach { db.accountDao().insertAccount(it) }

            val now = System.currentTimeMillis()
            val day = 24 * 3600 * 1000L

            val sampleTransactions = listOf(
                TransactionEntity("tx_1", "INCOME", 18500.00, "SAR", "acc_rajhi", null, "Salary", "Aramco Tech", now - 2 * day, "Monthly Salary Deposit"),
                TransactionEntity("tx_2", "EXPENSE", 4200.00, "SAR", "acc_rajhi", null, "Housing", "Emaar Property", now - 3 * day, "Apartment Rent"),
                TransactionEntity("tx_3", "EXPENSE", 320.00, "SAR", "acc_cash", null, "Dining", "Al Baik Restaurant", now - 1 * day, "Dinner with family"),
                TransactionEntity("tx_4", "TRANSFER", 2000.00, "SAR", "acc_rajhi", "acc_inma", "Transfer", "Internal Savings", now - 4 * day, "Monthly Savings Transfer"),
                TransactionEntity("tx_5", "EXPENSE", 180.00, "SAR", "acc_rajhi", null, "Utilities", "STC Fiber", now - 5 * day, "Internet Bill")
            )
            sampleTransactions.forEach { db.transactionDao().insertTransaction(it) }

            val sampleAssets = listOf(
                AssetEntity("ast_1", "Riyadh Villa Quarter", "REAL_ESTATE", 450000.0, 520000.0, 1.0, "Property", "SAR", "Prime location real estate", now - 365 * day, "ACTIVE", "acc_1", "Al Rajhi Bank"),
                AssetEntity("ast_2", "Toyota Camry 2024", "VEHICLE", 95000.0, 82000.0, 1.0, "Car", "SAR", "Personal Transport", now - 120 * day, "ACTIVE", "acc_1", "Al Rajhi Bank"),
                AssetEntity("ast_3", "Gold Bullion Bar", "GOLD", 22000.0, 26800.0, 100.0, "Grams", "SAR", "24K Fine Gold", now - 200 * day, "ACTIVE", "acc_2", "SNB AlAhli")
            )
            sampleAssets.forEach { db.assetDao().insertAsset(it) }

            val sampleAssetLogs = listOf(
                AssetLogEntity("log_1", "ast_1", "PURCHASE", "شراء الأصل", 450000.0, 0.0, 450000.0, "Al Rajhi Bank", now - 365 * day, "شراء عقار حي الرياض"),
                AssetLogEntity("log_2", "ast_1", "VALUE_UPDATE", "تحديث القيمة", 520000.0, 450000.0, 520000.0, "Al Rajhi Bank", now - 30 * day, "تحديث القيمة حسب التقييم العقاري"),
                AssetLogEntity("log_3", "ast_2", "PURCHASE", "شراء الأصل", 95000.0, 0.0, 95000.0, "Al Rajhi Bank", now - 120 * day, "شراء مركبة جديدة"),
                AssetLogEntity("log_4", "ast_2", "VALUE_UPDATE", "تحديث القيمة", 82000.0, 95000.0, 82000.0, "Al Rajhi Bank", now - 10 * day, "تخفيض القيمة مع الاستهلاك"),
                AssetLogEntity("log_5", "ast_3", "PURCHASE", "شراء الأصل", 22000.0, 0.0, 22000.0, "SNB AlAhli", now - 200 * day, "شراء سبيكة ذهب")
            )
            sampleAssetLogs.forEach { db.assetLogDao().insertLog(it) }

            val samplePersons = listOf(
                PersonEntity("prs_1", "Ahmed Al-Mansoor", "+966 50 123 4567", "Personal", "SAR", "College friend", true, now - 30 * day),
                PersonEntity("prs_2", "Samba Auto Finance", "+966 800 124 8000", "Institutional", "SAR", "Vehicle installment loan", true, now - 40 * day),
                PersonEntity("prs_3", "Mohammed Al-Amri", "+967 77 123 4567", "Personal", "USD", "Software consulting client", true, now - 20 * day),
                PersonEntity("prs_4", "Tariq Yemen Import", "+967 71 987 6543", "Institutional", "YER", "Goods supplier", true, now - 50 * day)
            )
            samplePersons.forEach { db.personDao().insertPerson(it) }

            val sampleDebtAccounts = listOf(
                PersonDebtAccountEntity("dac_1", "prs_1", "SAR", "Personal Loan Ledger", true, now - 30 * day),
                PersonDebtAccountEntity("dac_2", "prs_2", "SAR", "Auto Installments Ledger", true, now - 40 * day),
                PersonDebtAccountEntity("dac_3", "prs_3", "USD", "USD Consulting Ledger", true, now - 20 * day),
                PersonDebtAccountEntity("dac_4", "prs_4", "YER", "Yemeni Goods Ledger", true, now - 50 * day)
            )
            sampleDebtAccounts.forEach { db.personDebtAccountDao().insertDebtAccount(it) }

            val sampleDebts = listOf(
                DebtEntity("dbt_1", "dac_1", "prs_1", "Ahmed Al-Mansoor", "+966 50 123 4567", "RECEIVABLE", 5000.0, 2000.0, "SAR", now + 15 * day, "PARTIAL", "Personal loan for business equipment"),
                DebtEntity("dbt_2", "dac_2", "prs_2", "Samba Auto Finance", "+966 800 124 8000", "PAYABLE", 35000.0, 18500.0, "SAR", now + 45 * day, "PARTIAL", "Car installment loan"),
                DebtEntity("dbt_3", "dac_3", "prs_3", "Mohammed Al-Amri", "+967 77 123 4567", "RECEIVABLE", 1500.0, 1500.0, "USD", now + 30 * day, "ACTIVE", "Software consulting fee"),
                DebtEntity("dbt_4", "dac_4", "prs_4", "Tariq Yemen Import", "+967 71 987 6543", "PAYABLE", 250000.0, 120000.0, "YER", now + 60 * day, "PARTIAL", "Goods supplier invoice")
            )
            sampleDebts.forEach { db.debtDao().insertDebt(it) }

            val sampleBudgets = listOf(
                BudgetEntity("bdg_1", "Dining", 1500.0, "SAR", "MONTHLY"),
                BudgetEntity("bdg_2", "Housing", 5000.0, "SAR", "MONTHLY"),
                BudgetEntity("bdg_3", "Utilities", 800.0, "SAR", "MONTHLY"),
                BudgetEntity("bdg_4", "Entertainment", 1200.0, "SAR", "MONTHLY")
            )
            sampleBudgets.forEach { db.budgetDao().insertBudget(it) }

            val sampleGoals = listOf(
                GoalEntity("gol_1", "Emergency Fund", 60000.0, 42000.0, "SAR", now + 120 * day, "shield", "#10B981"),
                GoalEntity("gol_2", "Summer Europe Trip", 25000.0, 11500.0, "SAR", now + 90 * day, "plane", "#0EA5E9"),
                GoalEntity("gol_3", "New MacBook Pro", 12000.0, 8500.0, "SAR", now + 30 * day, "laptop", "#F59E0B")
            )
            sampleGoals.forEach { db.goalDao().insertGoal(it) }

            val sampleBills = listOf(
                BillEntity("bil_1", "STC Fiber Internet", 287.50, "SAR", "MONTHLY", now + 5 * day, "Utilities", "acc_rajhi", "UPCOMING", true),
                BillEntity("bil_2", "SEC Electricity", 412.00, "SAR", "MONTHLY", now + 10 * day, "Utilities", "acc_rajhi", "SCHEDULED", false),
                BillEntity("bil_3", "Fitness Time Gym", 350.00, "SAR", "MONTHLY", now + 18 * day, "Health", "acc_rajhi", "SCHEDULED", true)
            )
            sampleBills.forEach { db.billDao().insertBill(it) }
        }
    }

    override suspend fun isAccountInUse(accountId: String): Boolean {
        val txs = db.transactionDao().getTransactionsForAccount(accountId).first()
        val bills = db.billDao().getAllBills().first()

        return txs.isNotEmpty() || bills.any { it.accountId == accountId }
    }

    override suspend fun deleteAccount(accountId: String) {
        if (isAccountInUse(accountId)) {
            throw IllegalStateException("AccountInUseException: Cannot delete account because it is referenced in transactions or records.")
        }
        db.accountDao().deleteAccount(accountId)
    }

    // Mappers
    private fun AccountEntity.toDomain() = Account(id, name, AccountType.valueOf(type), balance, currency, accountNumber, colorHex, iconName, isArchived)
    private fun Account.toEntity() = AccountEntity(id, name, type.name, balance, currency, accountNumber, colorHex, iconName, isArchived)

    private fun TransactionEntity.toDomain() = Transaction(id, TransactionType.valueOf(type), amount, currency, sourceAccountId, destinationAccountId, category, party, date, note, relatedEntityId, status)
    private fun Transaction.toEntity() = TransactionEntity(id, type.name, amount, currency, sourceAccountId, destinationAccountId, category, party, date, note, relatedEntityId, status)

    private fun AssetEntity.toDomain() = Asset(
        id = id,
        name = name,
        type = AssetType.valueOf(type),
        purchaseValue = purchaseValue,
        currentValue = currentValue,
        quantity = quantity,
        unit = unit,
        currency = currency,
        notes = notes,
        purchaseDate = purchaseDate,
        status = runCatching { AssetStatus.valueOf(status) }.getOrDefault(AssetStatus.ACTIVE),
        purchaseAccountId = purchaseAccountId,
        purchaseAccountName = purchaseAccountName,
        soldPrice = soldPrice,
        soldDate = soldDate,
        soldAccountId = soldAccountId,
        soldAccountName = soldAccountName
    )
    private fun Asset.toEntity() = AssetEntity(
        id = id,
        name = name,
        type = type.name,
        purchaseValue = purchaseValue,
        currentValue = currentValue,
        quantity = quantity,
        unit = unit,
        currency = currency,
        notes = notes,
        purchaseDate = purchaseDate,
        status = status.name,
        purchaseAccountId = purchaseAccountId,
        purchaseAccountName = purchaseAccountName,
        soldPrice = soldPrice,
        soldDate = soldDate,
        soldAccountId = soldAccountId,
        soldAccountName = soldAccountName
    )

    private fun AssetLogEntity.toDomain() = AssetLog(
        id = id,
        assetId = assetId,
        type = runCatching { AssetLogType.valueOf(type) }.getOrDefault(AssetLogType.VALUE_UPDATE),
        title = title,
        amount = amount,
        previousValue = previousValue,
        newValue = newValue,
        accountName = accountName,
        date = date,
        notes = notes
    )
    private fun AssetLog.toEntity() = AssetLogEntity(
        id = id,
        assetId = assetId,
        type = type.name,
        title = title,
        amount = amount,
        previousValue = previousValue,
        newValue = newValue,
        accountName = accountName,
        date = date,
        notes = notes
    )

    private fun PersonEntity.toDomain() = Person(id, name, phone, category, currency, notes, isActive, createdAt)
    private fun Person.toEntity() = PersonEntity(id, name, phone, category, currency, notes, isActive, createdAt)

    private fun DebtEntity.toDomain() = Debt(id, debtAccountId, personId, partyName, partyPhone, DebtType.valueOf(type), originalAmount, remainingAmount, currency, dueDate, DebtStatus.valueOf(status), notes, createdAt)
    private fun Debt.toEntity() = DebtEntity(id, debtAccountId, personId, partyName, partyPhone, type.name, originalAmount, remainingAmount, currency, dueDate, status.name, notes, createdAt)

    private fun BudgetEntity.toDomain() = Budget(id, category, monthlyLimit, 0.0, currency, period, accountId)
    private fun Budget.toEntity() = BudgetEntity(id, category, monthlyLimit, currency, period, accountId)

    private fun GoalEntity.toDomain() = Goal(id, title, targetAmount, currentAmount, currency, targetDate, iconName, colorHex, isCompleted)
    private fun Goal.toEntity() = GoalEntity(id, title, targetAmount, currentAmount, currency, targetDate, iconName, colorHex, isCompleted)

    private fun BillEntity.toDomain() = Bill(id, title, amount, currency, frequency, nextDueDate, category, accountId, BillStatus.valueOf(status), isAutoPay)
    private fun Bill.toEntity() = BillEntity(id, title, amount, currency, frequency, nextDueDate, category, accountId, status.name, isAutoPay)
}
