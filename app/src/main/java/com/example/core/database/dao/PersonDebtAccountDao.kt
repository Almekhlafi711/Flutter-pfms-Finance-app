package com.example.core.database.dao

import androidx.room.*
import com.example.core.database.entities.PersonDebtAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonDebtAccountDao {
    @Query("SELECT * FROM person_debt_accounts ORDER BY createdAt DESC")
    fun getAllDebtAccounts(): Flow<List<PersonDebtAccountEntity>>

    @Query("SELECT * FROM person_debt_accounts WHERE personId = :personId")
    fun getAccountsForPerson(personId: String): Flow<List<PersonDebtAccountEntity>>

    @Query("SELECT * FROM person_debt_accounts WHERE id = :id")
    suspend fun getAccountById(id: String): PersonDebtAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebtAccount(account: PersonDebtAccountEntity)

    @Update
    suspend fun updateDebtAccount(account: PersonDebtAccountEntity)

    @Delete
    suspend fun deleteDebtAccount(account: PersonDebtAccountEntity)
}
