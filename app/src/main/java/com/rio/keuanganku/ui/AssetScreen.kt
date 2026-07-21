package com.rio.keuanganku.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rio.keuanganku.FinanceViewModel
import com.rio.keuanganku.data.Asset
import com.rio.keuanganku.formatDate
import com.rio.keuanganku.formatRp

val ASSET_KINDS = listOf(
    "Tunai", "Tabungan", "Deposito", "Reksa Dana", "Saham",
    "Emas", "Properti", "Kendaraan", "Lainnya"
)

@Composable
fun AssetScreen(vm: FinanceViewModel) {
    val assets by vm.assets.collectAsState()
    val totalAssets by vm.totalAssets.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Asset?>(null) }
    var adjusting by remember { mutableStateOf<Pair<Asset, Boolean>?>(null) } // (aset, isAdd)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, "Tambah aset")
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            SectionTitle("Aset")

            // Kartu total aset
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Total Aset",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        formatRp(totalAssets),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "${assets.size} aset tercatat",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            if (assets.isEmpty()) {
                EmptyHint("Belum ada aset. Tekan + untuk menambah, misalnya tabungan, reksa dana, emas, atau kendaraan.")
            }

            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(assets, key = { it.id }) { a ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clickable { editing = a; showDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(a.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text(
                                        "${a.kind} • diperbarui ${formatDate(a.updatedAt)}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (a.note.isNotBlank()) {
                                        Text(
                                            a.note,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    formatRp(a.value),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { vm.deleteAsset(a) }) {
                                    Icon(
                                        Icons.Default.Delete, "Hapus",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            // Tombol tambah / kurangi dana langsung
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { adjusting = a to true }) {
                                    Text("➕ Tambah", fontSize = 12.sp, color = GreenBright)
                                }
                                TextButton(onClick = { adjusting = a to false }) {
                                    Text("➖ Kurangi", fontSize = 12.sp, color = RedMoney)
                                }
                            }
                            // Target uang terkumpul
                            if (a.target > 0) {
                                val pct = (a.value / a.target * 100).coerceIn(0.0, 100.0)
                                LinearProgressIndicator(
                                    progress = { (pct / 100.0).toFloat() },
                                    color = if (a.value >= a.target) GreenBright else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                )
                                Text(
                                    if (a.value >= a.target)
                                        "🎉 Target ${formatRp(a.target)} tercapai!"
                                    else
                                        "Target ${formatRp(a.target)} — tercapai " +
                                            String.format(com.rio.keuanganku.localeID, "%.1f%%", pct) +
                                            ", kurang ${formatRp(a.target - a.value)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    adjusting?.let { (asset, isAdd) ->
        var amountText by remember(asset.id, isAdd) { mutableStateOf("") }
        var adjNote by remember(asset.id, isAdd) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { adjusting = null },
            title = { Text(if (isAdd) "Tambah Dana: ${asset.name}" else "Kurangi Dana: ${asset.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nilai saat ini: ${formatRp(asset.value)}", fontSize = 13.sp)
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(if (isAdd) "Jumlah dana masuk (Rp)" else "Jumlah dana keluar (Rp)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = adjNote,
                        onValueChange = { adjNote = it },
                        label = { Text("Catatan (opsional)") },
                        singleLine = true
                    )
                    if (!isAdd) {
                        Text(
                            "Catatan: jika pengeluaran ini terkait transaksi, lebih baik catat lewat " +
                                "menu Transaksi dengan sumber dana \"Dari Aset\" agar masuk laporan.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        vm.adjustAssetValue(asset, amount, adjNote.trim(), isAdd)
                        adjusting = null
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { adjusting = null }) { Text("Batal") }
            }
        )
    }

    if (showDialog) {
        AssetDialog(
            initial = editing,
            onDismiss = { showDialog = false },
            onSave = { asset ->
                if (asset.id == 0L) vm.addAsset(asset) else vm.updateAsset(asset)
                showDialog = false
            }
        )
    }
}

@Composable
fun AssetDialog(
    initial: Asset?,
    onDismiss: () -> Unit,
    onSave: (Asset) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var kind by remember { mutableStateOf(initial?.kind ?: ASSET_KINDS.first()) }
    var targetText by remember {
        mutableStateOf(
            initial?.target?.takeIf { it > 0 }
                ?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
        )
    }
    var valueText by remember {
        mutableStateOf(
            initial?.value?.takeIf { it > 0 }
                ?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
        )
    }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var kindMenuOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Tambah Aset" else "Edit Aset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama aset (mis. Tabungan BNI)") },
                    singleLine = true
                )
                Box {
                    OutlinedButton(onClick = { kindMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Jenis: $kind")
                    }
                    DropdownMenu(expanded = kindMenuOpen, onDismissRequest = { kindMenuOpen = false }) {
                        ASSET_KINDS.forEach { k ->
                            DropdownMenuItem(
                                text = { Text(k) },
                                onClick = { kind = k; kindMenuOpen = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Nilai saat ini (Rp)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Target uang terkumpul (Rp, opsional)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan (opsional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = valueText.toDoubleOrNull()
                if (name.isNotBlank() && value != null && value >= 0) {
                    onSave(
                        Asset(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            kind = kind,
                            value = value,
                            target = targetText.toDoubleOrNull() ?: 0.0,
                            note = note.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
