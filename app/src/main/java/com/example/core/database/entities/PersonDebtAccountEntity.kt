package com.example.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "person_debt_accounts")
data class PersonDebtAccountEntity(
    @PrimaryKey val id: String, // DebtAccountID
    val personId: String,
    val currency: String = "SAR", // SAR, USD, YER
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
