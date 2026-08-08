package com.example.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey val id: String,
    val debtAccountId: String = "",
    val personId: String = "",
    val partyName: String = "",
    val partyPhone: String? = null,
    val type: String, // RECEIVABLE (money owed to user), PAYABLE (money owed by user)
    val originalAmount: Double,
    val remainingAmount: Double,
    val currency: String = "SAR",
    val dueDate: Long = System.currentTimeMillis() + 30L * 24 * 3600 * 1000,
    val status: String = "ACTIVE", // ACTIVE, PARTIAL, COMPLETED, ARCHIVED
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
