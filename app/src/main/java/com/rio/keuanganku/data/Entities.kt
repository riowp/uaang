package com.rio.keuanganku.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val TYPE_PEMASUKAN = "PEMASUKAN"
const val TYPE_PENGELUARAN = "PENGELUARAN"
const val DEBT_UTANG = "UTANG"      // saya berutang ke orang lain
const val DEBT_PIUTANG = "PIUTANG"  // orang lain berutang ke saya

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,               // PEMASUKAN / PENGELUARAN
    val color: Long = 0xFF009540
)

@Entity(tableName = "transactions")
data class Txn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: Double,
    val type: String,               // PEMASUKAN / PENGELUARAN
    val categoryId: Long,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val assetId: Long = 0,          // 0 = transaksi uang bulanan biasa
    val assetDeposit: Boolean = false // true = setor ke aset; false + assetId!=0 = pakai dana aset
)

/** Pengeluaran yang dananya diambil dari aset — tidak mengurangi uang bulanan */
val Txn.fundedByAsset: Boolean
    get() = assetId != 0L && !assetDeposit

@Entity(
    tableName = "budgets",
    indices = [Index(value = ["categoryId", "monthKey"], unique = true)]
)
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val monthKey: String,           // contoh: "2026-07"
    val amount: Double
)

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val kind: String,               // Tunai / Tabungan / Deposito / Reksa Dana / Saham / Emas / Properti / Kendaraan / Lainnya
    val value: Double,
    val target: Double = 0.0,       // target uang terkumpul (0 = tanpa target)
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "plans")
data class Plan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,               // mis. "Beli Rumah", "Beli Mobil", "Umroh"
    val targetAmount: Double,
    val currentAmount: Double = 0.0,    // dana terkumpul (manual, dipakai jika tidak ditautkan ke aset)
    val monthlyDeposit: Double = 0.0,   // rencana setoran per bulan
    val annualReturnPct: Double = 0.0,  // asumsi imbal hasil %/tahun (0 = tabungan biasa)
    val targetDate: Long? = null,       // target tanggal tercapai (opsional)
    val linkedAssetId: Long = 0,        // jika != 0, dana terkumpul dibaca dari nilai aset
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val person: String,
    val amount: Double,
    val paid: Double = 0.0,
    val type: String,               // UTANG / PIUTANG
    val dueDate: Long? = null,
    val note: String = ""
) {
    val remaining: Double get() = (amount - paid).coerceAtLeast(0.0)
    val isSettled: Boolean get() = paid >= amount
}

@Entity(tableName = "asset_history")
data class AssetHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val assetId: Long,
    val assetName: String,          // disimpan terpisah agar riwayat tetap ada walau aset dihapus
    val delta: Double,              // + = dana masuk ke aset, - = dana keluar dari aset
    val balanceAfter: Double,       // nilai aset setelah perubahan
    val note: String = "",
    val source: String,             // MANUAL / TRANSAKSI / PENYESUAIAN / SALDO_AWAL
    val txnId: Long = 0,            // tautan ke transaksi (jika sumber = TRANSAKSI)
    val date: Long = System.currentTimeMillis()
)
