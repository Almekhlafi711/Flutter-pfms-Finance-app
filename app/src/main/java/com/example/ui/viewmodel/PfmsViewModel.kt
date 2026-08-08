package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.AppDatabase
import com.example.core.database.entities.PersonDebtAccountEntity
import com.example.core.security.SecurityManager
import com.example.core.util.PdfReportGenerator
import com.example.data.repository.PfmsRepositoryImpl
import com.example.domain.model.*
import com.example.domain.repository.PfmsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class QuickActionSheetType {
    INCOME, EXPENSE, TRANSFER, ASSET, DEBT, GOAL, BUDGET, BILL, REPORT, QUICK_ADD, DEPOSIT
}

class PfmsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository: PfmsRepository = PfmsRepositoryImpl(db)
    val securityManager = SecurityManager(application)

    private val _selectedAccountId = MutableStateFlow<String?>(null)
    val selectedAccountId: StateFlow<String?> = _selectedAccountId.asStateFlow()

    private val _activeBottomSheet = MutableStateFlow<QuickActionSheetType?>(null)
    val activeBottomSheet: StateFlow<QuickActionSheetType?> = _activeBottomSheet.asStateFlow()

    private val _isArabic = MutableStateFlow(securityManager.selectedLanguage == "ar")
    val isArabic: StateFlow<Boolean> = _isArabic.asStateFlow()

    private val _generatedPdfFile = MutableStateFlow<File?>(null)
    val generatedPdfFile: StateFlow<File?> = _generatedPdfFile.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    val accounts: StateFlow<List<Account>> = repository.getAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groupedAccounts: StateFlow<List<GroupedAccount>> = accounts.map { list ->
        list.groupBy { Pair(it.name, it.type) }.map { (key, accs) ->
            GroupedAccount(
                name = key.first,
                type = key.second,
                colorHex = accs.firstOrNull()?.colorHex ?: "#0EA5E9",
                iconName = accs.firstOrNull()?.iconName ?: "bank",
                isArchived = accs.all { it.isArchived },
                accounts = accs
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedAccount: StateFlow<Account?> = combine(accounts, selectedAccountId) { accList, selId ->
        accList.find { it.id == selId } ?: accList.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val transactions: StateFlow<List<Transaction>> = repository.getTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredTransactions: StateFlow<List<Transaction>> = combine(transactions, selectedAccountId) { txList, selId ->
        if (selId == null) txList
        else txList.filter { it.sourceAccountId == selId || it.destinationAccountId == selId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assets: StateFlow<List<Asset>> = repository.getAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val debts: StateFlow<List<Debt>> = repository.getDebts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val persons: StateFlow<List<Person>> = repository.getPersons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _debtLedgerEntries = MutableStateFlow<List<DebtLedgerEntry>>(emptyList())
    val debtLedgerEntries: StateFlow<List<DebtLedgerEntry>> = _debtLedgerEntries.asStateFlow()

    val personDebtAccounts: StateFlow<List<PersonDebtAccount>> = combine(
        debts, persons, _debtLedgerEntries
    ) { debtList, personList, entryList ->
        debtList.map { debt ->
            val matchingPerson = personList.find { it.id == debt.personId }
                ?: Person(
                    id = debt.personId,
                    name = debt.partyName.ifBlank { "Unknown Person" },
                    phone = debt.partyPhone,
                    category = "Personal"
                )

            val matchingEntries = entryList.filter { it.debtId == debt.id || it.personId == matchingPerson.id }
            val defaultEntries = if (matchingEntries.isEmpty()) {
                val entries = mutableListOf<DebtLedgerEntry>()
                // Original principal entry
                entries.add(
                    DebtLedgerEntry(
                        id = "entry_init_${debt.id}",
                        debtId = debt.id,
                        personId = matchingPerson.id,
                        type = debt.type,
                        isPayment = false,
                        amount = debt.originalAmount,
                        date = debt.createdAt,
                        description = debt.notes.ifEmpty { "Initial Debt Creation" },
                        category = "Debt Principal",
                        paymentMethod = "Bank / Cash",
                        status = "CLEARED"
                    )
                )
                // Payment entry if partial/settled
                if (debt.originalAmount > debt.remainingAmount) {
                    val paid = debt.originalAmount - debt.remainingAmount
                    entries.add(
                        DebtLedgerEntry(
                            id = "entry_pay_${debt.id}",
                            debtId = debt.id,
                            personId = matchingPerson.id,
                            type = debt.type,
                            isPayment = true,
                            amount = paid,
                            date = System.currentTimeMillis() - 2 * 24 * 3600 * 1000L,
                            description = "Partial Debt Settlement",
                            category = "Settlement",
                            paymentMethod = "Bank Transfer",
                            status = "CLEARED"
                        )
                    )
                }
                entries
            } else {
                matchingEntries
            }

            PersonDebtAccount(
                id = debt.debtAccountId.ifBlank { "dac_${debt.id}" },
                personId = matchingPerson.id,
                person = matchingPerson,
                currency = debt.currency,
                notes = debt.notes,
                isActive = matchingPerson.isActive,
                createdAt = debt.createdAt,
                mainDebt = debt,
                entries = defaultEntries
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<Budget>> = repository.getBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<Goal>> = repository.getGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bills: StateFlow<List<Bill>> = repository.getBills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val netWorthSummary: StateFlow<NetWorthSummary> = combine(
        accounts, assets, debts
    ) { accList, assetList, debtList ->
        val totalAccounts = accList.sumOf { it.balance }
        val totalAssets = assetList.sumOf { it.totalCurrentValue }
        val totalReceivables = debtList.filter { it.type == DebtType.RECEIVABLE }.sumOf { it.remainingAmount }
        val totalPayables = debtList.filter { it.type == DebtType.PAYABLE }.sumOf { it.remainingAmount }

        NetWorthSummary(
            totalCashAndAccounts = totalAccounts,
            totalAssetsValue = totalAssets,
            totalReceivables = totalReceivables,
            totalLiabilitiesAndPayables = totalPayables
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetWorthSummary(0.0, 0.0, 0.0, 0.0))

    init {
        viewModelScope.launch {
            repository.seedInitialSampleDataIfEmpty()
        }
    }

    fun selectAccount(accountId: String?) {
        _selectedAccountId.value = accountId
    }

    fun openBottomSheet(type: QuickActionSheetType) {
        _activeBottomSheet.value = type
    }

    fun closeBottomSheet() {
        _activeBottomSheet.value = null
    }

    fun addDeposit(amount: Double, accountId: String, category: String, note: String, currency: String = "SAR") {
        viewModelScope.launch {
            try {
                val accCurrency = accounts.value.find { it.id == accountId }?.currency ?: "SAR"
                val tx = Transaction(
                    id = UUID.randomUUID().toString(),
                    type = TransactionType.INCOME,
                    amount = amount,
                    currency = if (currency.isNotBlank()) currency else accCurrency,
                    sourceAccountId = accountId,
                    category = category.ifEmpty { "Cash Deposit" },
                    note = note
                )
                repository.addTransaction(tx)
                closeBottomSheet()
            } catch (e: Exception) {
                _toastMessage.value = e.message ?: "Failed to add deposit"
            }
        }
    }

    fun addIncome(amount: Double, accountId: String, category: String, party: String, note: String, currency: String = "SAR") {
        viewModelScope.launch {
            try {
                val accCurrency = accounts.value.find { it.id == accountId }?.currency ?: "SAR"
                val tx = Transaction(
                    id = UUID.randomUUID().toString(),
                    type = TransactionType.INCOME,
                    amount = amount,
                    currency = if (currency.isNotBlank()) currency else accCurrency,
                    sourceAccountId = accountId,
                    category = category,
                    party = party.ifEmpty { null },
                    note = note
                )
                repository.addTransaction(tx)
                closeBottomSheet()
            } catch (e: Exception) {
                _toastMessage.value = e.message ?: "Failed to add income"
            }
        }
    }

    fun addExpense(amount: Double, accountId: String, category: String, party: String, note: String, currency: String = "SAR") {
        viewModelScope.launch {
            try {
                val accCurrency = accounts.value.find { it.id == accountId }?.currency ?: "SAR"
                val tx = Transaction(
                    id = UUID.randomUUID().toString(),
                    type = TransactionType.EXPENSE,
                    amount = amount,
                    currency = if (currency.isNotBlank()) currency else accCurrency,
                    sourceAccountId = accountId,
                    category = category,
                    party = party.ifEmpty { null },
                    note = note
                )
                repository.addTransaction(tx)
                closeBottomSheet()
            } catch (e: Exception) {
                _toastMessage.value = e.message ?: "Failed to add expense"
            }
        }
    }

    fun addTransfer(amount: Double, sourceAccountId: String, destAccountId: String, note: String, currency: String = "SAR") {
        viewModelScope.launch {
            try {
                val accCurrency = accounts.value.find { it.id == sourceAccountId }?.currency ?: "SAR"
                val tx = Transaction(
                    id = UUID.randomUUID().toString(),
                    type = TransactionType.TRANSFER,
                    amount = amount,
                    currency = if (currency.isNotBlank()) currency else accCurrency,
                    sourceAccountId = sourceAccountId,
                    destinationAccountId = destAccountId,
                    category = "Internal Transfer",
                    note = note
                )
                repository.addTransaction(tx)
                closeBottomSheet()
            } catch (e: Exception) {
                _toastMessage.value = e.message ?: "Failed to complete transfer"
            }
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteAsset(id: String) {
        viewModelScope.launch {
            repository.deleteAsset(id)
        }
    }

    fun getAssetLogs(assetId: String): Flow<List<AssetLog>> {
        return repository.getAssetLogs(assetId)
    }

    fun updateAsset(asset: Asset) {
        viewModelScope.launch {
            repository.updateAsset(asset)
        }
    }

    fun updateAssetValue(asset: Asset, newCurrentValue: Double, notes: String = "") {
        viewModelScope.launch {
            val previousValue = asset.currentValue
            val updatedAsset = asset.copy(currentValue = newCurrentValue)
            repository.updateAsset(updatedAsset)

            val isAr = isArabic.value
            val log = AssetLog(
                id = UUID.randomUUID().toString(),
                assetId = asset.id,
                type = AssetLogType.VALUE_UPDATE,
                title = if (isAr) "تحديث القيمة" else "Value Update",
                amount = newCurrentValue,
                previousValue = previousValue,
                newValue = newCurrentValue,
                date = System.currentTimeMillis(),
                notes = notes
            )
            repository.addAssetLog(log)
        }
    }

    fun sellAsset(asset: Asset, salePrice: Double, destinationAccountId: String, notes: String = "") {
        viewModelScope.launch {
            val account = accounts.value.find { it.id == destinationAccountId } ?: return@launch
            val now = System.currentTimeMillis()
            val updatedAsset = asset.copy(
                status = AssetStatus.SOLD,
                soldPrice = salePrice,
                soldDate = now,
                soldAccountId = destinationAccountId,
                soldAccountName = account.name
            )
            repository.updateAsset(updatedAsset)

            val isAr = isArabic.value
            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                type = TransactionType.ASSET_SALE,
                amount = salePrice,
                currency = "SAR",
                sourceAccountId = destinationAccountId,
                category = "Asset Liquidation",
                party = asset.name,
                relatedEntityId = asset.id,
                note = if (isAr) "بيع أصل: ${asset.name}" else "Sold asset: ${asset.name}",
                date = now
            )
            repository.addTransaction(tx)

            val log = AssetLog(
                id = UUID.randomUUID().toString(),
                assetId = asset.id,
                type = AssetLogType.SALE,
                title = if (isAr) "بيع الأصل" else "Asset Sale",
                amount = salePrice,
                accountName = account.name,
                date = now,
                notes = notes
            )
            repository.addAssetLog(log)
        }
    }

    fun addDetailedAsset(
        name: String,
        type: AssetType,
        purchaseVal: Double,
        currentVal: Double,
        accountId: String,
        currency: String = "SAR",
        purchaseDate: Long = System.currentTimeMillis(),
        notes: String = ""
    ) {
        viewModelScope.launch {
            val account = accounts.value.find { it.id == accountId }
            val accountName = account?.name ?: ""
            val finalCurrentValue = if (currentVal <= 0.0) purchaseVal else currentVal
            val assetId = UUID.randomUUID().toString()
            
            val asset = Asset(
                id = assetId,
                name = name,
                type = type,
                purchaseValue = purchaseVal,
                currentValue = finalCurrentValue,
                currency = "SAR",
                purchaseDate = purchaseDate,
                notes = notes,
                status = AssetStatus.ACTIVE,
                purchaseAccountId = accountId,
                purchaseAccountName = accountName
            )
            repository.addAsset(asset)

            val isAr = isArabic.value
            if (purchaseVal > 0 && accountId.isNotEmpty()) {
                val tx = Transaction(
                    id = UUID.randomUUID().toString(),
                    type = TransactionType.ASSET_PURCHASE,
                    amount = purchaseVal,
                    currency = "SAR",
                    sourceAccountId = accountId,
                    category = "Asset Acquisition",
                    party = name,
                    relatedEntityId = assetId,
                    note = notes.ifEmpty { if (isAr) "شراء أصل $name" else "Purchased asset $name" },
                    date = purchaseDate
                )
                repository.addTransaction(tx)
            }

            val log = AssetLog(
                id = UUID.randomUUID().toString(),
                assetId = assetId,
                type = AssetLogType.PURCHASE,
                title = if (isAr) "شراء الأصل" else "Asset Purchase",
                amount = purchaseVal,
                previousValue = 0.0,
                newValue = purchaseVal,
                accountName = accountName,
                date = purchaseDate,
                notes = notes
            )
            repository.addAssetLog(log)
        }
    }

    fun addAsset(name: String, type: AssetType, purchaseVal: Double, currentVal: Double, accountId: String) {
        addDetailedAsset(name, type, purchaseVal, currentVal, accountId)
        closeBottomSheet()
    }

    fun addPerson(person: Person) {
        viewModelScope.launch {
            repository.addPerson(person)
        }
    }

    fun updatePerson(person: Person) {
        viewModelScope.launch {
            repository.updatePerson(person)
        }
    }

    fun updatePersonStatus(personId: String, isActive: Boolean) {
        val targetPerson = persons.value.find { it.id == personId }
        if (targetPerson != null) {
            updatePerson(targetPerson.copy(isActive = isActive))
        }
    }

    fun addDebtForPerson(
        person: Person,
        type: DebtType,
        amount: Double,
        accountId: String,
        category: String,
        currency: String,
        notes: String
    ) {
        addPerson(person)
        viewModelScope.launch {
            val dacId = "dac_" + UUID.randomUUID().toString().take(8)
            val debtAccountEntity = PersonDebtAccountEntity(
                id = dacId,
                personId = person.id,
                currency = currency,
                notes = notes
            )
            repository.addDebtAccount(debtAccountEntity)

            val debtId = "dbt_" + UUID.randomUUID().toString().take(8)
            val debt = Debt(
                id = debtId,
                debtAccountId = dacId,
                personId = person.id,
                partyName = person.name,
                partyPhone = person.phone,
                type = type,
                originalAmount = amount,
                remainingAmount = amount,
                currency = currency,
                dueDate = System.currentTimeMillis() + 30L * 24 * 3600 * 1000,
                status = DebtStatus.ACTIVE,
                notes = notes
            )
            repository.addDebt(debt)

            // Add ledger entry
            val newEntry = DebtLedgerEntry(
                id = "entry_" + UUID.randomUUID().toString().take(8),
                debtAccountId = dacId,
                debtId = debtId,
                personId = person.id,
                type = type,
                isPayment = false,
                amount = amount,
                date = System.currentTimeMillis(),
                description = notes.ifEmpty { "New Debt Ledger Creation" },
                category = category,
                paymentMethod = if (accountId.isNotEmpty()) "Account Transfer" else "Cash",
                accountId = accountId,
                status = "CLEARED"
            )
            _debtLedgerEntries.value = _debtLedgerEntries.value + newEntry

            if (accountId.isNotEmpty()) {
                val txType = if (type == DebtType.RECEIVABLE) TransactionType.DEBT_CREATION else TransactionType.INCOME
                val tx = Transaction(
                    id = UUID.randomUUID().toString(),
                    type = txType,
                    amount = amount,
                    currency = currency,
                    sourceAccountId = accountId,
                    category = "Debt Center",
                    party = person.name,
                    relatedEntityId = debtId,
                    note = notes.ifEmpty { "Created debt with ${person.name}" }
                )
                repository.addTransaction(tx)
            }
            closeBottomSheet()
        }
    }

    fun addPaymentForPerson(
        person: Person,
        amount: Double,
        accountId: String,
        currency: String,
        notes: String,
        isReceive: Boolean
    ) {
        addPerson(person)
        viewModelScope.launch {
            var matchingDebt = debts.value.find { it.personId == person.id && it.currency.equals(currency, ignoreCase = true) }
            if (matchingDebt == null) {
                val dacId = "dac_" + UUID.randomUUID().toString().take(8)
                val debtAccountEntity = PersonDebtAccountEntity(
                    id = dacId,
                    personId = person.id,
                    currency = currency,
                    notes = notes
                )
                repository.addDebtAccount(debtAccountEntity)

                val debtId = "dbt_" + UUID.randomUUID().toString().take(8)
                matchingDebt = Debt(
                    id = debtId,
                    debtAccountId = dacId,
                    personId = person.id,
                    partyName = person.name,
                    partyPhone = person.phone,
                    type = if (isReceive) DebtType.RECEIVABLE else DebtType.PAYABLE,
                    originalAmount = 0.0,
                    remainingAmount = 0.0,
                    currency = currency,
                    dueDate = System.currentTimeMillis() + 30L * 24 * 3600 * 1000,
                    status = DebtStatus.ACTIVE,
                    notes = notes
                )
                repository.addDebt(matchingDebt)
            } else {
                val updatedRemaining = (matchingDebt.remainingAmount - amount).coerceAtLeast(0.0)
                val updatedStatus = if (updatedRemaining == 0.0) DebtStatus.COMPLETED else DebtStatus.PARTIAL
                repository.addDebt(matchingDebt.copy(remainingAmount = updatedRemaining, status = updatedStatus))
            }

            val debtId = matchingDebt.id
            val dacId = matchingDebt.debtAccountId

            val paymentEntry = DebtLedgerEntry(
                id = "pay_entry_" + UUID.randomUUID().toString().take(8),
                debtAccountId = dacId,
                debtId = debtId,
                personId = person.id,
                type = matchingDebt.type,
                isPayment = true,
                amount = amount,
                date = System.currentTimeMillis(),
                description = notes.ifEmpty { if (isReceive) "استلام مبلغ" else "سداد مبلغ" },
                category = if (isReceive) "Receipt" else "Settlement",
                paymentMethod = if (accountId.isNotEmpty()) "Account Settlement" else "Cash",
                accountId = accountId,
                status = "CLEARED"
            )
            _debtLedgerEntries.value = _debtLedgerEntries.value + paymentEntry

            if (accountId.isNotEmpty()) {
                val txType = if (isReceive) TransactionType.INCOME else TransactionType.EXPENSE
                val tx = Transaction(
                    id = UUID.randomUUID().toString(),
                    type = txType,
                    amount = amount,
                    currency = currency,
                    sourceAccountId = accountId,
                    category = "Debt Settlement",
                    party = person.name,
                    relatedEntityId = debtId,
                    note = notes.ifEmpty { if (isReceive) "Payment received from ${person.name}" else "Payment paid to ${person.name}" }
                )
                repository.addTransaction(tx)
            }
            closeBottomSheet()
        }
    }

    fun addDebt(partyName: String, phone: String, type: DebtType, amount: Double, accountId: String, note: String) {
        val person = persons.value.find { it.name.equals(partyName, ignoreCase = true) }
            ?: Person(
                id = "prs_" + UUID.randomUUID().toString().take(8),
                name = partyName,
                phone = phone.ifEmpty { null },
                category = "General"
            )
        addDebtForPerson(person, type, amount, accountId, "General", "SAR", note)
    }

    fun addGoal(title: String, targetAmount: Double, initialAmount: Double) {
        viewModelScope.launch {
            val goal = Goal(
                id = UUID.randomUUID().toString(),
                title = title,
                targetAmount = targetAmount,
                currentAmount = initialAmount,
                targetDate = System.currentTimeMillis() + 180L * 24 * 3600 * 1000
            )
            repository.addGoal(goal)
            closeBottomSheet()
        }
    }

    fun addBudget(category: String, limit: Double) {
        viewModelScope.launch {
            val budget = Budget(
                id = UUID.randomUUID().toString(),
                category = category,
                monthlyLimit = limit
            )
            repository.addBudget(budget)
            closeBottomSheet()
        }
    }

    fun addBill(title: String, amount: Double, category: String, accountId: String) {
        viewModelScope.launch {
            val bill = Bill(
                id = UUID.randomUUID().toString(),
                title = title,
                amount = amount,
                category = category,
                accountId = accountId,
                nextDueDate = System.currentTimeMillis() + 14L * 24 * 3600 * 1000
            )
            repository.addBill(bill)
            closeBottomSheet()
        }
    }

    fun recordDebtPayment(debt: Debt, paymentAmount: Double, accountId: String) {
        viewModelScope.launch {
            val updatedRemaining = (debt.remainingAmount - paymentAmount).coerceAtLeast(0.0)
            val updatedStatus = if (updatedRemaining == 0.0) DebtStatus.COMPLETED else DebtStatus.PARTIAL
            val updatedDebt = debt.copy(remainingAmount = updatedRemaining, status = updatedStatus)
            repository.addDebt(updatedDebt)

            val person = persons.value.find { it.name.equals(debt.partyName, ignoreCase = true) }
            val personId = person?.id ?: ("prs_" + debt.partyName.hashCode())

            // Add payment ledger entry
            val paymentEntry = DebtLedgerEntry(
                id = "pay_entry_" + UUID.randomUUID().toString().take(8),
                debtId = debt.id,
                personId = personId,
                type = debt.type,
                isPayment = true,
                amount = paymentAmount,
                date = System.currentTimeMillis(),
                description = "Settlement Payment Received/Paid",
                category = "Settlement",
                paymentMethod = if (accountId.isNotEmpty()) "Account Settlement" else "Cash",
                accountId = accountId,
                status = "CLEARED"
            )
            _debtLedgerEntries.value = _debtLedgerEntries.value + paymentEntry

            // Record transaction
            val txType = if (debt.type == DebtType.RECEIVABLE) TransactionType.INCOME else TransactionType.EXPENSE
            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                type = txType,
                amount = paymentAmount,
                sourceAccountId = accountId,
                category = "Debt Settlement",
                party = debt.partyName,
                relatedEntityId = debt.id,
                note = "Payment recorded for debt"
            )
            repository.addTransaction(tx)
        }
    }

    fun updateDebtLedgerEntry(entryId: String, amount: Double, description: String, type: DebtType) {
        _debtLedgerEntries.value = _debtLedgerEntries.value.map { entry ->
            if (entry.id == entryId) {
                entry.copy(amount = amount, description = description, type = type)
            } else {
                entry
            }
        }
    }

    fun contributeToGoal(goal: Goal, amount: Double, accountId: String) {
        viewModelScope.launch {
            val newCurrent = goal.currentAmount + amount
            val isCompleted = newCurrent >= goal.targetAmount
            val updatedGoal = goal.copy(currentAmount = newCurrent, isCompleted = isCompleted)
            repository.addGoal(updatedGoal)

            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                type = TransactionType.GOAL_CONTRIBUTION,
                amount = amount,
                sourceAccountId = accountId,
                category = "Goal Savings",
                party = goal.title,
                relatedEntityId = goal.id,
                note = "Contribution to ${goal.title}"
            )
            repository.addTransaction(tx)
        }
    }

    fun payBill(bill: Bill, accountId: String) {
        viewModelScope.launch {
            val updatedBill = bill.copy(
                status = BillStatus.PAID,
                nextDueDate = bill.nextDueDate + 30L * 24 * 3600 * 1000
            )
            repository.addBill(updatedBill)

            val tx = Transaction(
                id = UUID.randomUUID().toString(),
                type = TransactionType.BILL_PAYMENT,
                amount = bill.amount,
                sourceAccountId = accountId,
                category = bill.category,
                party = bill.title,
                relatedEntityId = bill.id,
                note = "Paid bill ${bill.title}"
            )
            repository.addTransaction(tx)
        }
    }

    fun exportAccountStatementPdf() {
        val acc = selectedAccount.value ?: accounts.value.firstOrNull() ?: return
        val txs = filteredTransactions.value
        val nw = netWorthSummary.value
        val file = PdfReportGenerator.generateAccountStatementPdf(getApplication(), acc, txs, nw)
        _generatedPdfFile.value = file
    }

    fun toggleLanguage() {
        val newAr = !_isArabic.value
        _isArabic.value = newAr
        securityManager.selectedLanguage = if (newAr) "ar" else "en"
    }

    fun addAccount(name: String, type: AccountType, balance: Double, currency: String) {
        viewModelScope.launch {
            val acc = Account(
                id = "acc_" + UUID.randomUUID().toString().take(8),
                name = name,
                type = type,
                balance = balance,
                currency = currency
            )
            repository.addAccount(acc)
        }
    }

    fun addDetailedAccount(
        name: String,
        type: AccountType,
        balance: Double,
        currency: String,
        accountNumber: String,
        colorHex: String,
        iconName: String
    ) {
        viewModelScope.launch {
            val acc = Account(
                id = "acc_" + UUID.randomUUID().toString().take(8),
                name = name,
                type = type,
                balance = balance,
                currency = currency,
                accountNumber = accountNumber,
                colorHex = colorHex,
                iconName = iconName
            )
            repository.addAccount(acc)
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun archiveAccount(accountId: String) {
        viewModelScope.launch {
            repository.archiveAccount(accountId)
            if (_selectedAccountId.value == accountId) {
                _selectedAccountId.value = null
            }
        }
    }

    fun addMultiCurrencyAccount(
        name: String,
        type: AccountType,
        currencyBalances: Map<String, Double>,
        colorHex: String,
        iconName: String,
        notes: String
    ) {
        viewModelScope.launch {
            currencyBalances.forEach { (currency, balance) ->
                val acc = Account(
                    id = "acc_" + UUID.randomUUID().toString().take(8),
                    name = name,
                    type = type,
                    balance = balance,
                    currency = currency,
                    colorHex = colorHex,
                    iconName = iconName
                )
                repository.addAccount(acc)
            }
        }
    }

    fun deleteAccount(accountId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteAccount(accountId)
                if (_selectedAccountId.value == accountId) {
                    _selectedAccountId.value = null
                }
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Account is in use")
            }
        }
    }
}
