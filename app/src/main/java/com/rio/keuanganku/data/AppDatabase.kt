package com.rio.keuanganku.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Category::class, Txn::class, Budget::class, Debt::class, Asset::class, Plan::class, AssetHistory::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun txnDao(): TxnDao
    abstract fun categoryDao(): CategoryDao
    abstract fun budgetDao(): BudgetDao
    abstract fun debtDao(): DebtDao
    abstract fun assetDao(): AssetDao
    abstract fun planDao(): PlanDao
    abstract fun assetHistoryDao(): AssetHistoryDao

    companion object {
        /** v1 → v2: tabel assets baru; data lama tetap aman */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `assets` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`kind` TEXT NOT NULL, " +
                        "`value` REAL NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`updatedAt` INTEGER NOT NULL)"
                )
            }
        }

        /** v2 → v3: target aset, transaksi berbasis aset, tabel rencana keuangan */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `assets` ADD COLUMN `target` REAL NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `assetId` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `assetDeposit` INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `plans` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`targetAmount` REAL NOT NULL, " +
                        "`currentAmount` REAL NOT NULL, " +
                        "`monthlyDeposit` REAL NOT NULL, " +
                        "`annualReturnPct` REAL NOT NULL, " +
                        "`targetDate` INTEGER, " +
                        "`linkedAssetId` INTEGER NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
            }
        }

        /** v3 → v4: tabel riwayat perubahan aset */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `asset_history` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`assetId` INTEGER NOT NULL, " +
                        "`assetName` TEXT NOT NULL, " +
                        "`delta` REAL NOT NULL, " +
                        "`balanceAfter` REAL NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`source` TEXT NOT NULL, " +
                        "`txnId` INTEGER NOT NULL, " +
                        "`date` INTEGER NOT NULL)"
                )
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null

        /** Tutup koneksi database — dipakai sebelum restore backup */
        fun closeInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "keuanganku.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
