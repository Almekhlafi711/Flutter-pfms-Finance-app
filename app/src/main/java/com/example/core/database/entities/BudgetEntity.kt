package com.example.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey val id: String,
    val category: String,
    val monthlyLimit: Double,
    val currency: String = "SAR",
    val period: String = "MONTHLY", // MONTHLY, WEEKLY, YEARLY
    val accountId: String? = null
)
