package com.example.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey val id: String,
    val title: String,
    val amount: Double,
    val currency: String = "SAR",
    val frequency: String = "MONTHLY", // MONTHLY, WEEKLY, YEARLY
    val nextDueDate: Long = System.currentTimeMillis() + 14L * 24 * 3600 * 1000,
    val category: String = "Utilities",
    val accountId: String = "",
    val status: String = "SCHEDULED", // SCHEDULED, UPCOMING, DUE, PAID, OVERDUE
    val isAutoPay: Boolean = false
)
