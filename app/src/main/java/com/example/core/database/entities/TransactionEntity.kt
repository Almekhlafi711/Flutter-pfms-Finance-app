package com.example.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: String, // INCOME, EXPENSE, TRANSFER, ASSET_PURCHASE, ASSET_SALE, DEBT_PAYMENT, DEBT_CREATION, GOAL_CONTRIBUTION, BILL_PAYMENT
    val amount: Double,
    val currency: String = "SAR",
    val sourceAccountId: String,
    val destinationAccountId: String? = null,
    val category: String,
    val party: String? = null,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val relatedEntityId: String? = null,
    val status: String = "POSTED" // POSTED, DRAFT, ARCHIVED
)
