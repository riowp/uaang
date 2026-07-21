package com.rio.keuanganku.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rio.keuanganku.FinanceViewModel
import com.rio.keuanganku.data.AssetHistory
import com.rio.keuanganku.data.TYPE_PEMASUKAN
import com.rio.keuanganku.data.TYPE_PENGELUARAN
import com.rio.keuanganku.data.fundedByAsset
import com.rio.keuanganku.formatDate
import com.rio.keuanganku.formatRp
import com.rio.keuanganku.label
import com.rio.keuanganku.toYearMonth

@Composable
fun HistoryScreen(vm: FinanceViewModel) {
    val allTxns by vm.allTxns.collectAsState()
    val assetHistory by vm.assetHistory.collectAsState()
    val categories by vm.categories.collectAsState()
    val catMap = categories.associateBy { it.id }

    var tab by remember { mutableStateOf(0) } // 0 = Bulanan, 1 = Aset

    Column(Modifier.fillMaxSize()) {
        SectionTitle("Riwayat")
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            FilterChip(
                selected = tab == 0,
                onClick = { tab = 0 },
                label = { Text("Bulanan") }
            )
            FilterChip(
                selected = tab == 1,
                onClick = { tab = 1 },
                label = { Text("Aset") }
            )
        }

        if (tab == 0) {
            // ---------- Riwayat transaksi bulanan (sepanjang waktu) ----------
            if (allTxns.isEmpty()) {
                EmptyHint("Belum ada transaksi tercatat.")
            }
            val groups = allTxns.groupBy { it.date.toYearMonth() }
                .toSortedMap(compareByDescending { it })
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                groups.forEach { (ym, list) ->
                    val income = list.filter { it.type == TYPE_PEMASUKAN }.sumOf { it.amount }
                    val expense = list.filter { it.type == TYPE_PENGELUARAN && !it.fundedByAsset }
                        .sumOf { it.amount }
                    item(key = "h-${ym}") {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                ym.label(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Masuk ${formatRp(income)}  •  Keluar ${formatRp(expense)}  •  Sisa ${formatRp(income - expense)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    items(list, key = { "t-${it.id}" }) { t ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    catMap[t.categoryId]?.name ?: "Lainnya",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    formatDate(t.date) + if (t.note.isNotBlank()) " • ${t.note}" else "",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (t.assetId != 0L) {
                                    Text(
                                        if (t.assetDeposit) "↑ Setor ke aset" else "💰 Dana dari aset",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (t.assetDeposit) GreenBright else AdaroYellow
                                    )
                                }
                            }
                            Text(
                                (if (t.type == TYPE_PEMASUKAN) "+" else "-") + formatRp(t.amount),
                                color = if (t.type == TYPE_PEMASUKAN) GreenBright else RedMoney,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        } else {
            // ---------- Riwayat perubahan aset ----------
            if (assetHistory.isEmpty()) {
                EmptyHint(
                    "Belum ada riwayat aset. Riwayat tercatat otomatis saat: saldo awal dibuat, " +
                        "dana ditambah/dikurangi, transaksi Setor/Dari Aset, atau nilai aset diedit."
                )
            }
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(assetHistory, key = { it.id }) { h ->
                    AssetHistoryRow(h)
                }
            }
        }
    }
}

@Composable
private fun AssetHistoryRow(h: AssetHistory) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(h.assetName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(
                formatDate(h.date) + " • " + sourceLabel(h.source) +
                    if (h.note.isNotBlank()) " • ${h.note}" else "",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Saldo akhir: ${formatRp(h.balanceAfter)}",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            (if (h.delta >= 0) "+" else "-") + formatRp(kotlin.math.abs(h.delta)),
            color = if (h.delta >= 0) GreenBright else RedMoney,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

private fun sourceLabel(source: String): String = when (source) {
    "MANUAL" -> "Manual"
    "TRANSAKSI" -> "Transaksi"
    "PENYESUAIAN" -> "Penyesuaian"
    "SALDO_AWAL" -> "Saldo awal"
    else -> source
}
