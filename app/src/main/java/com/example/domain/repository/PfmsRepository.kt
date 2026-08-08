package com.example.domain.repository

import com.example.core.database.entities.PersonDebtAccountEntity
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow

interface PfmsRepository {
    fun getAccounts(): Flow<List<Account>>
    fun getTransactions(): Flow<List<Transaction>>
    fun getTransactionsForAccount(accountId: String): Flow<List<Transaction>>
    fun getAssets(): Flow<List<Asset>>
    fun getDebts(): Flow<List<Debt>>
    fun getBudgets(): Flow<List<Budget>>
    fun getGoals(): Flow<List<Goal>>
    fun getBills(): Flow<List<Bill>>

    suspend fun addAccount(account: Account)
    suspend fun updateAccount(account: Account)
    suspend fun archiveAccount(accountId: String)
    suspend fun addTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: String)
    suspend fun addAsset(asset: Asset)
    suspend fun updateAsset(asset: Asset)
    suspend fun deleteAsset(id: String)
    fun getAssetLogs(assetId: String): Flow<List<AssetLog>>
    suspend fun addAssetLog(log: AssetLog)

    fun getPersons(): Flow<List<Person>>
    suspend fun addPerson(person: Person)
    suspend fun updatePerson(person: Person)

    fun getDebtAccounts(): Flow<List<PersonDebtAccountEntity>>
    suspend fun addDebtAccount(account: PersonDebtAccountEntity)
    suspend fun updateDebtAccount(account: PersonDebtAccountEntity)

    suspend fun addDebt(debt: Debt)
    suspend fun recordDebtPayment(debtId: String, paymentAmount: Double, accountId: String)
    suspend fun deleteDebt(id: String)

    suspend fun addBudget(budget: Budget)
    suspend fun deleteBudget(id: String)

    suspend fun addGoal(goal: Goal)
    suspend fun contributeToGoal(goalId: String, amount: Double, accountId: String)
    suspend fun deleteGoal(id: String)

    suspend fun addBill(bill: Bill)
    suspend fun payBill(billId: String, accountId: String)
    suspend fun deleteBill(id: String)

    suspend fun seedInitialSampleDataIfEmpty()
    suspend fun isAccountInUse(accountId: String): Boolean
    suspend fun deleteAccount(accountId: String)
}
