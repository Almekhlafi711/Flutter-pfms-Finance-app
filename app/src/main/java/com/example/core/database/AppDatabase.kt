package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.database.dao.*
import com.example.core.database.entities.*

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        AssetEntity::class,
        DebtEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        BillEntity::class,
        PersonEntity::class,
        PersonDebtAccountEntity::class,
        AssetLogEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun assetDao(): AssetDao
    abstract fun assetLogDao(): AssetLogDao
    abstract fun debtDao(): DebtDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
    abstract fun billDao(): BillDao
    abstract fun personDao(): PersonDao
    abstract fun personDebtAccountDao(): PersonDebtAccountDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pfms_finance_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
