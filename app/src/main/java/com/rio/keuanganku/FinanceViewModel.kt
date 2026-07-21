package com.rio.keuanganku

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rio.keuanganku.data.AppDatabase
import com.rio.keuanganku.data.Asset
import com.rio.keuanganku.data.AssetHistory
import com.rio.keuanganku.data.Plan
import com.rio.keuanganku.data.Budget
import com.rio.keuanganku.data.Category
import com.rio.keuanganku.data.Debt
import com.rio.keuanganku.data.TYPE_PEMASUKAN
import com.rio.keuanganku.data.TYPE_PENGELUARAN
import com.rio.keuanganku.data.Txn
import com.rio.keuanganku.pdf.PdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val txnDao = db.txnDao()
    private val catDao = db.categoryDao()
    private val budgetDao = db.budgetDao()
    private val debtDao = db.debtDao()
    private val assetDao = db.assetDao()
    private val planDao = db.planDao()
    private val historyDao = db.assetHistoryDao()
    private val prefs = app.getSharedPreferences("settings", Context.MODE_PRIVATE)

    val month = MutableStateFlow(YearMonth.now())

    // ---------- Dark mode ----------
    val darkMode = MutableStateFlow(prefs.getBoolean("dark", false))
    fun toggleDark() {
        darkMode.value = !darkMode.value
        prefs.edit().putBoolean("dark", darkMode.value).apply()
    }

    val categories = catDao.all()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val txns = month.flatMapLatest { ym ->
        val (s, e) = ym.rangeMillis()
        txnDao.byRange(s, e)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Transaksi bulan sebelumnya — untuk perbandingan di layar Analisis */
    val prevMonthTxns = month.flatMapLatest { ym ->
        val (s, e) = ym.minusMonths(1).rangeMillis()
        txnDao.byRange(s, e)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Transaksi 6 bulan terakhir (termasuk bulan aktif) — untuk grafik tren */
    val trendTxns = month.flatMapLatest { ym ->
        val s = ym.minusMonths(5).rangeMillis().first
        val e = ym.rangeMillis().second
        txnDao.byRange(s, e)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Saldo total sepanjang waktu (semua pemasukan - semua pengeluaran) */
    val totalBalance = txnDao.totalBalance()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val budgets = month.flatMapLatest { ym ->
        budgetDao.byMonth(ym.monthKey())
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val debts = debtDao.all()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val assets = assetDao.all()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Total nilai seluruh aset tercatat */
    val totalAssets = assetDao.total()
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val plans = planDao.all()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Seluruh transaksi sepanjang waktu — untuk layar Riwayat */
    val allTxns = txnDao.allFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Riwayat perubahan nilai aset */
    val assetHistory = historyDao.all()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            if (catDao.count() == 0) {
                defaultCategories().forEach { catDao.insert(it) }
            }
        }
    }

    fun prevMonth() { month.value = month.value.minusMonths(1) }
    fun nextMonth() { month.value = month.value.plusMonths(1) }

    // ---------- Transaksi ----------
    fun addTxn(t: Txn) = viewModelScope.launch {
        val id = txnDao.insert(t)
        applyAssetEffect(t.copy(id = id), +1)
    }

    fun updateTxn(old: Txn, new: Txn) = viewModelScope.launch {
        txnDao.update(new)
        applyAssetEffect(old, -1)   // batalkan efek lama
        applyAssetEffect(new, +1)   // terapkan efek baru
    }

    fun deleteTxn(t: Txn) = viewModelScope.launch {
        txnDao.delete(t)
        applyAssetEffect(t, -1)
    }

    /**
     * Efek transaksi ke nilai aset:
     * - Setor ke aset (assetDeposit = true): nilai aset bertambah
     * - Pakai dana aset (assetDeposit = false): nilai aset berkurang
     */
    private suspend fun applyAssetEffect(t: Txn, sign: Int) {
        if (t.assetId == 0L || t.type != TYPE_PENGELUARAN) return
        val asset = assetDao.byId(t.assetId) ?: return
        val delta = (if (t.assetDeposit) t.amount else -t.amount) * sign
        val newValue = (asset.value + delta).coerceAtLeast(0.0)
        assetDao.update(
            asset.copy(value = newValue, updatedAt = System.currentTimeMillis())
        )
        if (sign > 0) {
            historyDao.insert(
                AssetHistory(
                    assetId = asset.id,
                    assetName = asset.name,
                    delta = delta,
                    balanceAfter = newValue,
                    note = t.note.ifBlank { if (t.assetDeposit) "Setoran dari transaksi" else "Pemakaian dana" },
                    source = "TRANSAKSI",
                    txnId = t.id,
                    date = t.date
                )
            )
        } else {
            // Transaksi diedit/dihapus: hapus jejak riwayat lama
            historyDao.deleteByTxn(t.id)
        }
    }

    // ---------- Kategori ----------
    fun addCategory(c: Category) = viewModelScope.launch { catDao.insert(c) }
    fun updateCategory(c: Category) = viewModelScope.launch { catDao.update(c) }
    fun deleteCategory(c: Category) = viewModelScope.launch { catDao.delete(c) }

    // ---------- Anggaran ----------
    fun setBudget(categoryId: Long, amount: Double) = viewModelScope.launch {
        budgetDao.upsert(Budget(categoryId = categoryId, monthKey = month.value.monthKey(), amount = amount))
    }
    fun deleteBudget(b: Budget) = viewModelScope.launch { budgetDao.delete(b) }

    // ---------- Utang Piutang ----------
    fun addDebt(d: Debt) = viewModelScope.launch { debtDao.insert(d) }
    fun updateDebt(d: Debt) = viewModelScope.launch { debtDao.update(d) }
    fun deleteDebt(d: Debt) = viewModelScope.launch { debtDao.delete(d) }
    fun payDebt(d: Debt, amount: Double) = viewModelScope.launch {
        debtDao.update(d.copy(paid = (d.paid + amount).coerceAtMost(d.amount)))
    }

    // ---------- Aset ----------
    fun addAsset(a: Asset) = viewModelScope.launch {
        val id = assetDao.insert(a)
        if (a.value > 0) {
            historyDao.insert(
                AssetHistory(
                    assetId = id, assetName = a.name,
                    delta = a.value, balanceAfter = a.value,
                    note = "Saldo awal", source = "SALDO_AWAL"
                )
            )
        }
    }

    fun updateAsset(a: Asset) = viewModelScope.launch {
        val old = assetDao.byId(a.id)
        assetDao.update(a)
        if (old != null && old.value != a.value) {
            historyDao.insert(
                AssetHistory(
                    assetId = a.id, assetName = a.name,
                    delta = a.value - old.value, balanceAfter = a.value,
                    note = "Edit nilai aset", source = "PENYESUAIAN"
                )
            )
        }
    }

    fun deleteAsset(a: Asset) = viewModelScope.launch { assetDao.delete(a) }

    /** Tambah / kurangi dana aset langsung (tanpa lewat menu edit) */
    fun adjustAssetValue(a: Asset, amount: Double, note: String, isAdd: Boolean) =
        viewModelScope.launch {
            val newValue = (a.value + if (isAdd) amount else -amount).coerceAtLeast(0.0)
            assetDao.update(a.copy(value = newValue, updatedAt = System.currentTimeMillis()))
            historyDao.insert(
                AssetHistory(
                    assetId = a.id, assetName = a.name,
                    delta = newValue - a.value, balanceAfter = newValue,
                    note = note.ifBlank { if (isAdd) "Tambah dana" else "Kurangi dana" },
                    source = "MANUAL"
                )
            )
        }

    // ---------- Rencana Keuangan ----------
    fun addPlan(p: Plan) = viewModelScope.launch { planDao.insert(p) }
    fun updatePlan(p: Plan) = viewModelScope.launch { planDao.update(p) }
    fun deletePlan(p: Plan) = viewModelScope.launch { planDao.delete(p) }

    // ---------- Export PDF ----------
    fun exportPdf(context: Context) = viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            PdfExporter.export(
                context = context,
                ym = month.value,
                txns = txns.value,
                categories = categories.value,
                budgets = budgets.value,
                debts = debts.value,
                assets = assets.value
            )
        }
        Toast.makeText(context, result, Toast.LENGTH_LONG).show()
    }

    private fun defaultCategories() = listOf(
        Category(name = "Gaji", type = TYPE_PEMASUKAN, color = 0xFF009540),
        Category(name = "Bonus / THR", type = TYPE_PEMASUKAN, color = 0xFF2E7D32),
        Category(name = "Usaha Sampingan", type = TYPE_PEMASUKAN, color = 0xFF00897B),
        Category(name = "Investasi", type = TYPE_PEMASUKAN, color = 0xFF00695C),
        Category(name = "Pemasukan Lainnya", type = TYPE_PEMASUKAN, color = 0xFF4CAF50),
        Category(name = "Makan & Minum", type = TYPE_PENGELUARAN, color = 0xFFE53935),
        Category(name = "Belanja Rumah Tangga", type = TYPE_PENGELUARAN, color = 0xFFF4511E),
        Category(name = "Transportasi", type = TYPE_PENGELUARAN, color = 0xFFFB8C00),
        Category(name = "Pendidikan", type = TYPE_PENGELUARAN, color = 0xFF3949AB),
        Category(name = "Kesehatan", type = TYPE_PENGELUARAN, color = 0xFF8E24AA),
        Category(name = "Cicilan / KPR", type = TYPE_PENGELUARAN, color = 0xFF6D4C41),
        Category(name = "Listrik & Air", type = TYPE_PENGELUARAN, color = 0xFF00ACC1),
        Category(name = "Internet & Pulsa", type = TYPE_PENGELUARAN, color = 0xFF7CB342),
        Category(name = "Hiburan", type = TYPE_PENGELUARAN, color = 0xFFFFB300),
        Category(name = "Pengeluaran Lainnya", type = TYPE_PENGELUARAN, color = 0xFF757575)
    )
}
