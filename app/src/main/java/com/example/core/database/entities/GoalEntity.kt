package com.example.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val currency: String = "SAR",
    val targetDate: Long = System.currentTimeMillis() + 180L * 24 * 3600 * 1000,
    val iconName: String = "target",
    val colorHex: String = "#38BDF8",
    val isCompleted: Boolean = false
)
