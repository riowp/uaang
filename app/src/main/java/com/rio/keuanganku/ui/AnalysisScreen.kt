package com.rio.keuanganku.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rio.keuanganku.FinanceViewModel
import com.rio.keuanganku.data.TYPE_PEMASUKAN
import com.rio.keuanganku.data.TYPE_PENGELUARAN
import com.rio.keuanganku.data.fundedByAsset
import com.rio.keuanganku.formatDate
import com.rio.keuanganku.formatRp
import com.rio.keuanganku.label
import com.rio.keuanganku.toYearMonth
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@Composable
fun AnalysisScreen(vm: FinanceViewModel) {
    val month by vm.month.collectAsState()
    val txns by vm.txns.collectAsState()
    val prevTxns by vm.prevMonthTxns.collectAsState()
    val trendTxns by vm.trendTxns.collectAsState()
    val categories by vm.categories.collectAsState()
    val plans by vm.plans.collectAsState()
    val assets by vm.assets.collectAsState()
    val catMap = categories.associateBy { it.id }

    val income = txns.filter { it.type == TYPE_PEMASUKAN }.sumOf { it.amount }
    val expense = txns.filter { it.type == TYPE_PENGELUARAN && !it.fundedByAsset }.sumOf { it.amount }
    val prevIncome = prevTxns.filter { it.type == TYPE_PEMASUKAN }.sumOf { it.amount }
    val prevExpense = prevTxns.filter { it.type == TYPE_PENGELUARAN && !it.fundedByAsset }.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        SectionTitle("Analisis — ${month.label()}")

        // ---------- Perbandingan bulan lalu ----------
        AnalysisCard("Perbandingan dengan Bulan Lalu") {
            CompareRow("Pemasukan", income, prevIncome, goodWhenUp = true)
            CompareRow("Pengeluaran", expense, prevExpense, goodWhenUp = false)
            CompareRow("Sisa Uang", income - expense, prevIncome - prevExpense, goodWhenUp = true)
        }

        // ---------- Analisis pencapaian target ----------
        AnalysisCard("Pencapaian Target") {
            val targetAssets = assets.filter { it.target > 0 }
            if (plans.isEmpty() && targetAssets.isEmpty()) {
                EmptyHint(
                    "Belum ada target. Buat rencana di Lainnya → Rencana Keuangan, " +
                        "atau atur target uang terkumpul pada aset."
                )
            } else {
                plans.forEach { plan ->
                    val current = if (plan.linkedAssetId != 0L)
                        assets.find { it.id == plan.linkedAssetId }?.value ?: plan.currentAmount
                    else plan.currentAmount
                    val pct = if (plan.targetAmount > 0)
                        (current / plan.targetAmount * 100).coerceIn(0.0, 100.0) else 0.0
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("🎯 ${plan.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(
                                String.format(com.rio.keuanganku.localeID, "%.1f%%", pct),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (pct / 100.0).toFloat() },
                            color = if (current >= plan.targetAmount) GreenBright
                            else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                        )
                        Text(
                            "${formatRp(current)} dari ${formatRp(plan.targetAmount)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (current >= plan.targetAmount) {
                            Text("🎉 Tercapai!", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenBright)
                        } else {
                            if (plan.monthlyDeposit > 0) {
                                val months = monthsToTarget(
                                    current, plan.targetAmount,
                                    plan.monthlyDeposit, plan.annualReturnPct
                                )
                                Text(
                                    if (months != null)
                                        "📈 Dengan setoran ${formatRp(plan.monthlyDeposit)}/bln → " +
                                            "${fmtDuration(months)} lagi (± ${YearMonth.now().plusMonths(months.toLong()).label()})"
                                    else "📈 Setoran belum cukup untuk mencapai target",
                                    fontSize = 11.sp,
                                    color = if (months == null) RedMoney else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            plan.targetDate?.let { td ->
                                val monthsLeft = ChronoUnit.MONTHS
                                    .between(YearMonth.now(), td.toYearMonth()).toInt()
                                if (monthsLeft > 0) {
                                    val need = requiredMonthly(
                                        current, plan.targetAmount, monthsLeft, plan.annualReturnPct
                                    )
                                    if (need != null) {
                                        val enough = plan.monthlyDeposit >= need && plan.monthlyDeposit > 0
                                        Text(
                                            "🗓️ Wajib ${formatRp(need)}/bln agar tercapai ${formatDate(td)}" +
                                                if (plan.monthlyDeposit > 0)
                                                    (if (enough) " ✅" else " (kurang ${formatRp(need - plan.monthlyDeposit)}) ⚠️")
                                                else "",
                                            fontSize = 11.sp,
                                            color = if (plan.monthlyDeposit > 0 && !enough) RedMoney
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else {
                                    Text(
                                        "🗓️ Target tanggal sudah lewat — perbarui rencana",
                                        fontSize = 11.sp, color = RedMoney
                                    )
                                }
                            }
                        }
                    }
                }
                if (targetAssets.isNotEmpty()) {
                    Text(
                        "Target Aset",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    targetAssets.forEach { a ->
                        val pct = (a.value / a.target * 100).coerceIn(0.0, 100.0)
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(a.name, fontSize = 12.sp)
                                Text(
                                    String.format(com.rio.keuanganku.localeID, "%.1f%%", pct),
                                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (pct / 100.0).toFloat() },
                                color = if (a.value >= a.target) GreenBright
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                            )
                            Text(
                                if (a.value >= a.target) "🎉 Tercapai!"
                                else "Kurang ${formatRp(a.target - a.value)} dari target ${formatRp(a.target)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // ---------- Persentase kategori ----------
        AnalysisCard("Persentase Kategori Pengeluaran") {
            val slices = txns.filter { it.type == TYPE_PENGELUARAN && !it.fundedByAsset }
                .groupBy { it.categoryId }
                .map { (id, list) ->
                    Slice(
                        label = catMap[id]?.name ?: "Lainnya",
                        value = list.sumOf { it.amount },
                        color = Color(catMap[id]?.color ?: 0xFF757575)
                    )
                }
                .sortedByDescending { it.value }
            if (slices.isEmpty()) {
                EmptyHint("Belum ada pengeluaran bulan ini.")
            } else {
                DonutWithLegend(slices, centerLabel = "Total\n${formatRp(expense)}")
            }
        }

        // ---------- Tren 6 bulan ----------
        AnalysisCard("Tren 6 Bulan Terakhir") {
            val months = (5 downTo 0).map { month.minusMonths(it.toLong()) }
            val grouped = trendTxns.groupBy { it.date.toYearMonth() }
            val incomes = months.map { ym ->
                grouped[ym]?.filter { it.type == TYPE_PEMASUKAN }?.sumOf { it.amount } ?: 0.0
            }
            val expenses = months.map { ym ->
                grouped[ym]?.filter { it.type == TYPE_PENGELUARAN && !it.fundedByAsset }?.sumOf { it.amount } ?: 0.0
            }
            TrendBarChart(months, incomes, expenses)
            Spacer(Modifier.height(8.dp))
        }

        // ---------- Pengeluaran terbesar ----------
        AnalysisCard("Pengeluaran Terbesar Bulan Ini") {
            val topCats = txns.filter { it.type == TYPE_PENGELUARAN && !it.fundedByAsset }
                .groupBy { it.categoryId }
                .map { (id, list) -> (catMap[id]?.name ?: "Lainnya") to list.sumOf { it.amount } }
                .sortedByDescending { it.second }
                .take(5)
            if (topCats.isEmpty()) {
                EmptyHint("Belum ada pengeluaran bulan ini.")
            } else {
                Text(
                    "Per Kategori",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                topCats.forEachIndexed { i, (name, amt) ->
                    RankRow(i + 1, name, "", amt)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Per Transaksi",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                val topTxns = txns.filter { it.type == TYPE_PENGELUARAN && !it.fundedByAsset }
                    .sortedByDescending { it.amount }
                    .take(5)
                topTxns.forEachIndexed { i, t ->
                    RankRow(
                        i + 1,
                        if (t.note.isNotBlank()) t.note else (catMap[t.categoryId]?.name ?: "Lainnya"),
                        formatDate(t.date),
                        t.amount
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AnalysisCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(vertical = 10.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            content()
        }
    }
}

@Composable
private fun RankRow(rank: Int, title: String, subtitle: String, amount: Double) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text("$rank. $title", fontSize = 13.sp, maxLines = 1)
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(
            formatRp(amount),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = RedMoney
        )
    }
}
