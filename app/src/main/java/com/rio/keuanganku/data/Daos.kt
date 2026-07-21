package com.rio.keuanganku.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TxnDao {
    @Insert suspend fun insert(t: Txn): Long
    @Update suspend fun update(t: Txn)
    @Delete suspend fun delete(t: Txn)

    @Query("SELECT * FROM transactions WHERE date BETWEEN :s AND :e ORDER BY date DESC, id DESC")
    fun byRange(s: Long, e: Long): Flow<List<Txn>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :s AND :e ORDER BY date DESC, id DESC")
    suspend fun byRangeOnce(s: Long, e: Long): List<Txn>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun allFlow(): Flow<List<Txn>>

    // Pengeluaran yang dananya dari aset (assetId != 0, bukan setoran) tidak mengurangi saldo kas
    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'PEMASUKAN' THEN amount WHEN (assetId != 0 AND assetDeposit = 0) THEN 0 ELSE -amount END), 0) FROM transactions")
    fun totalBalance(): Flow<Double>
}

@Dao
interface CategoryDao {
    @Insert suspend fun insert(c: Category)
    @Update suspend fun update(c: Category)
    @Delete suspend fun delete(c: Category)

    @Query("SELECT * FROM categories ORDER BY type DESC, name ASC")
    fun all(): Flow<List<Category>>

    @Query("SELECT * FROM categories")
    suspend fun allOnce(): List<Category>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int
}

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(b: Budget)

    @Delete suspend fun delete(b: Budget)

    @Query("SELECT * FROM budgets WHERE monthKey = :key")
    fun byMonth(key: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE monthKey = :key")
    suspend fun byMonthOnce(key: String): List<Budget>
}

@Dao
interface DebtDao {
    @Insert suspend fun insert(d: Debt)
    @Update suspend fun update(d: Debt)
    @Delete suspend fun delete(d: Debt)

    @Query("SELECT * FROM debts ORDER BY id DESC")
    fun all(): Flow<List<Debt>>

    @Query("SELECT * FROM debts")
    suspend fun allOnce(): List<Debt>
}

@Dao
interface AssetDao {
    @Insert suspend fun insert(a: Asset): Long
    @Update suspend fun update(a: Asset)
    @Delete suspend fun delete(a: Asset)

    @Query("SELECT * FROM assets ORDER BY `value` DESC")
    fun all(): Flow<List<Asset>>

    @Query("SELECT * FROM assets")
    suspend fun allOnce(): List<Asset>

    @Query("SELECT COALESCE(SUM(`value`), 0) FROM assets")
    fun total(): Flow<Double>

    @Query("SELECT * FROM assets WHERE id = :id")
    suspend fun byId(id: Long): Asset?
}

@Dao
interface AssetHistoryDao {
    @Insert suspend fun insert(h: AssetHistory)

    @Query("SELECT * FROM asset_history ORDER BY date DESC, id DESC")
    fun all(): Flow<List<AssetHistory>>

    @Query("DELETE FROM asset_history WHERE txnId = :txnId AND txnId != 0")
    suspend fun deleteByTxn(txnId: Long)
}

@Dao
interface PlanDao {
    @Insert suspend fun insert(p: Plan)
    @Update suspend fun update(p: Plan)
    @Delete suspend fun delete(p: Plan)

    @Query("SELECT * FROM plans ORDER BY id DESC")
    fun all(): Flow<List<Plan>>
}
