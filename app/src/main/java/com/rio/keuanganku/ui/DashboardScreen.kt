package com.rio.keuanganku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rio.keuanganku.FinanceViewModel
import com.rio.keuanganku.data.DEBT_PIUTANG
import com.rio.keuanganku.data.DEBT_UTANG
import com.rio.keuanganku.data.TYPE_PEMASUKAN
import com.rio.keuanganku.data.TYPE_PENGELUARAN
import com.rio.keuanganku.data.fundedByAsset
import com.rio.keuanganku.formatRp
import com.rio.keuanganku.label

@Composable
fun DashboardScreen(vm: FinanceViewModel) {
    val context = LocalContext.current
    val month by vm.month.collectAsState()
    val txns by vm.txns.collectAsState()
    val categories by vm.categories.collectAsState()
    val budgets by vm.budgets.collectAsState()
    val debts by vm.debts.collectAsState()
    val totalBalance by vm.totalBalance.collectAsState()
    val totalAssets by vm.totalAssets.collectAsState()
    val dark by vm.darkMode.collectAsState()

    val income = txns.filter { it.type == TYPE_PEMASUKAN }.sumOf { it.amount }
    val expense = txns.filter { it.type == TYPE_PENGELUARAN && !it.fundedByAsset }.sumOf { it.amount }
    val remaining = income - expense
    val catMap = categories.associateBy { it.id }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ---------- Header gradient ----------
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(AdaroGreen, AdaroDark)),
                    RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                )
                .padding(bottom = 20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 16.dp)
            ) {
                Text(
                    "KeuanganKu",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { vm.toggleDark() }) {
                    Text(if (dark) "☀️" else "🌙", fontSize = 18.sp)
                }
            }
            Text(
                "Saldo Total",
                color = Color(0xCCFFFFFF),
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 20.dp, top = 8.dp)
            )
            Text(
                formatRp(totalBalance),
                color = if (totalBalance >= 0) Color.White else Color(0xFFFFB3B3),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                modifier = Modifier.padding(start = 20.dp)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { vm.prevMonth() }) {
                    Icon(Icons.Default.KeyboardArrowLeft, "Bulan sebelumnya", tint = Color.White)
                }
                Text(
                    month.label(),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(170.dp)
                )
                IconButton(onClick = { vm.nextMonth() }) {
                    Icon(Icons.Default.KeyboardArrowRight, "Bulan berikutnya", tint = Color.White)
                }
            }
        }

        // ---------- Ringkasan bulan ini ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard("Pemasukan", income, GreenBright, Modifier.weight(1f))
            SummaryCard("Pengeluaran", expense, RedMoney, Modifier.weight(1f))
            SummaryCard(
                "Sisa Uang", remaining,
                if (remaining >= 0) MaterialTheme.colorScheme.primary else RedMoney,
                Modifier.weight(1f)
            )
        }

        // ---------- Grafik pengeluaran (donut) ----------
        SectionTitle("Grafik Pengeluaran Bulan Ini")
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                DonutWithLegend(slices, centerLabel = "Total\n${formatRp(expense)}")
            }
        }

        // ---------- Status anggaran ----------
        SectionTitle("Status Anggaran")
        if (budgets.isEmpty()) {
            EmptyHint("Belum ada anggaran. Atur di menu Anggaran.")
        } else {
            budgets.forEach { b ->
                val spent = txns.filter { it.type == TYPE_PENGELUARAN && !it.fundedByAsset && it.categoryId == b.categoryId }
                    .sumOf { it.amount }
                val frac = if (b.amount > 0) (spent / b.amount).toFloat() else 0f
                val over = spent > b.amount
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(catMap[b.categoryId]?.name ?: "Lainnya", fontSize = 13.sp)
                        Text(
                            "${formatRp(spent)} / ${formatRp(b.amount)}",
                            fontSize = 12.sp,
                            color = if (over) RedMoney else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { frac.coerceIn(0f, 1f) },
                        color = if (over) RedMoney else MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .padding(top = 2.dp)
                    )
                    if (over) Text("Over budget!", color = RedMoney, fontSize = 11.sp)
                }
            }
        }

        // ---------- Utang piutang ----------
        SectionTitle("Utang & Piutang Aktif")
        val totalUtang = debts.filter { it.type == DEBT_UTANG && !it.isSettled }.sumOf { it.remaining }
        val totalPiutang = debts.filter { it.type == DEBT_PIUTANG && !it.isSettled }.sumOf { it.remaining }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard("Total Utang", totalUtang, RedMoney, Modifier.weight(1f))
            SummaryCard("Total Piutang", totalPiutang, GreenBright, Modifier.weight(1f))
        }

        // ---------- Aset & kekayaan bersih ----------
        SectionTitle("Aset & Kekayaan")
        val netWorth = totalAssets + totalBalance - totalUtang + totalPiutang
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryCard("Total Aset", totalAssets, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            SummaryCard(
                "Kekayaan Bersih", netWorth,
                if (netWorth >= 0) MaterialTheme.colorScheme.primary else RedMoney,
                Modifier.weight(1f)
            )
        }
        Text(
            "Kekayaan bersih = aset + saldo + piutang − utang aktif. Kelola aset di menu Lainnya → Aset.",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(Modifier.height(16.dp))

        // ---------- Export PDF ----------
        Button(
            onClick = { vm.exportPdf(context) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp)
        ) {
            Text("Export Dashboard ke PDF", fontWeight = FontWeight.SemiBold)
        }
        Text(
            "Laporan otomatis aktif: PDF bulan sebelumnya dibuat otomatis setiap tanggal 1 dan tersimpan di Download/KeuanganKu.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryCard(title: String, value: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                formatRp(value),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
