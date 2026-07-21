package com.rio.keuanganku.ui

import android.app.DatePickerDialog
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.rio.keuanganku.FinanceViewModel
import com.rio.keuanganku.data.Asset
import com.rio.keuanganku.data.Category
import com.rio.keuanganku.data.TYPE_PEMASUKAN
import com.rio.keuanganku.data.TYPE_PENGELUARAN
import com.rio.keuanganku.data.Txn
import com.rio.keuanganku.formatDate
import com.rio.keuanganku.formatRp
import com.rio.keuanganku.label
import com.rio.keuanganku.ocr.ReceiptScanner
import java.io.File
import java.util.Calendar

// Sumber dana pengeluaran
private const val SRC_CASH = 0        // uang bulanan
private const val SRC_FROM_ASSET = 1  // pakai dana aset (mengurangi aset, tidak mengurangi uang bulanan)
private const val SRC_TO_ASSET = 2    // setor ke aset (pengeluaran bulanan, menambah aset)

@Composable
fun TransactionScreen(vm: FinanceViewModel) {
    val context = LocalContext.current
    val txns by vm.txns.collectAsState()
    val categories by vm.categories.collectAsState()
    val assets by vm.assets.collectAsState()
    val month by vm.month.collectAsState()
    val catMap = categories.associateBy { it.id }
    val assetMap = assets.associateBy { it.id }

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Txn?>(null) }
    var showScanChoice by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var review by remember { mutableStateOf<ReceiptScanner.Result?>(null) }

    fun handleScan(uri: Uri) {
        scanning = true
        ReceiptScanner.scan(context, uri) { result ->
            scanning = false
            if (result == null || (result.candidates.isEmpty() && result.store.isEmpty())) {
                Toast.makeText(context, "Struk tidak terbaca. Coba foto lebih dekat, lurus, dan terang.", Toast.LENGTH_LONG).show()
            } else {
                review = result
            }
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let { handleScan(it) }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { handleScan(it) }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExtendedFloatingActionButton(
                    onClick = { showScanChoice = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text("📷  Scan Struk", fontSize = 13.sp)
                }
                FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                    Icon(Icons.Default.Add, "Tambah transaksi")
                }
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            SectionTitle("Transaksi — ${month.label()}")
            if (txns.isEmpty()) {
                EmptyHint("Belum ada transaksi bulan ini. Tekan + untuk menambah, atau Scan Struk.")
            }
            LazyColumn {
                items(txns, key = { it.id }) { t ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editing = t; showDialog = true }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                catMap[t.categoryId]?.name ?: "Lainnya",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                formatDate(t.date) + if (t.note.isNotBlank()) " • ${t.note}" else "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (t.assetId != 0L) {
                                val assetName = assetMap[t.assetId]?.name ?: "aset"
                                Text(
                                    if (t.assetDeposit) "↑ Setor ke aset: $assetName"
                                    else "💰 Dana dari aset: $assetName",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (t.assetDeposit) GreenBright else AdaroYellow.copy(alpha = 0.9f)
                                )
                            }
                        }
                        Text(
                            (if (t.type == TYPE_PEMASUKAN) "+" else "-") + formatRp(t.amount),
                            color = if (t.type == TYPE_PEMASUKAN) GreenBright else RedMoney,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        IconButton(onClick = { vm.deleteTxn(t) }) {
                            Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showScanChoice) {
        AlertDialog(
            onDismissRequest = { showScanChoice = false },
            title = { Text("Scan Struk") },
            text = { Text("Foto struk atau pilih dari galeri. Aplikasi akan membaca nominal, tanggal, dan nama toko secara otomatis (offline).") },
            confirmButton = {
                TextButton(onClick = {
                    showScanChoice = false
                    val file = File(context.cacheDir, "scan_struk_${System.currentTimeMillis()}.jpg")
                    val uri = FileProvider.getUriForFile(
                        context, context.packageName + ".fileprovider", file
                    )
                    cameraUri = uri
                    takePicture.launch(uri)
                }) { Text("📷 Kamera") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showScanChoice = false
                    pickImage.launch("image/*")
                }) { Text("🖼️ Galeri") }
            }
        )
    }

    if (scanning) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Membaca struk…") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.padding(end = 16.dp))
                    Text("Mohon tunggu sebentar")
                }
            },
            confirmButton = { }
        )
    }

    // ---------- Dialog verifikasi hasil scan ----------
    review?.let { r ->
        var itemsMode by remember(r) { mutableStateOf(r.items.size >= 2) }
        val expenseCats = categories.filter { it.type == TYPE_PENGELUARAN }

        if (itemsMode) {
            // ===== Mode rincian per item: satu transaksi per barang =====
            var storeText by remember(r) { mutableStateOf(r.store) }
            val checks = remember(r) { mutableStateListOf(*Array(r.items.size) { true }) }
            val cats = remember(r) { mutableStateListOf(*Array(r.items.size) { 0L }) }
            var menuFor by remember(r) { mutableStateOf(-1) } // -1 tutup, -2 kategori semua
            val catNames = categories.associateBy { it.id }

            AlertDialog(
                onDismissRequest = { review = null },
                title = { Text("Rincian per Item (${r.items.size})") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = storeText,
                            onValueChange = { storeText = it },
                            label = { Text("Nama toko") },
                            singleLine = true
                        )
                        Text(
                            r.date?.let { "Tanggal: ${formatDate(it)}" } ?: "Tanggal: hari ini",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Box {
                            OutlinedButton(onClick = { menuFor = -2 }, modifier = Modifier.fillMaxWidth()) {
                                Text("Terapkan kategori ke semua item…", fontSize = 12.sp)
                            }
                            DropdownMenu(expanded = menuFor == -2, onDismissRequest = { menuFor = -1 }) {
                                expenseCats.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c.name) },
                                        onClick = {
                                            for (i in cats.indices) cats[i] = c.id
                                            menuFor = -1
                                        }
                                    )
                                }
                            }
                        }
                        r.items.forEachIndexed { i, item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = checks[i], onCheckedChange = { checks[i] = it })
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        (if (item.qty > 1) "${item.qty}x " else "") + item.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                    Text(formatRp(item.price), fontSize = 12.sp, color = RedMoney)
                                }
                                Box {
                                    TextButton(onClick = { menuFor = i }) {
                                        Text(
                                            catNames[cats[i]]?.name?.take(12) ?: "Kategori",
                                            fontSize = 11.sp
                                        )
                                    }
                                    DropdownMenu(expanded = menuFor == i, onDismissRequest = { menuFor = -1 }) {
                                        expenseCats.forEach { c ->
                                            DropdownMenuItem(
                                                text = { Text(c.name) },
                                                onClick = { cats[i] = c.id; menuFor = -1 }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        val selTotal = r.items.indices.filter { checks[it] }.sumOf { r.items[it].price }
                        Text(
                            "Total dipilih: ${formatRp(selTotal)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { itemsMode = false }) {
                            Text("Simpan sebagai satu total saja", fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val idxs = r.items.indices.filter { checks[it] }
                        when {
                            idxs.isEmpty() ->
                                Toast.makeText(context, "Pilih minimal satu item", Toast.LENGTH_SHORT).show()
                            idxs.any { cats[it] == 0L } ->
                                Toast.makeText(context, "Pilih kategori untuk semua item yang dicentang", Toast.LENGTH_SHORT).show()
                            else -> {
                                val d = r.date ?: System.currentTimeMillis()
                                val prefix = storeText.trim().let { if (it.isNotEmpty()) "$it — " else "" }
                                idxs.forEach { i ->
                                    val item = r.items[i]
                                    vm.addTxn(
                                        Txn(
                                            amount = item.price,
                                            type = TYPE_PENGELUARAN,
                                            categoryId = cats[i],
                                            note = (prefix + (if (item.qty > 1) "${item.qty}x " else "") + item.name).take(60),
                                            date = d
                                        )
                                    )
                                }
                                Toast.makeText(context, "${idxs.size} transaksi tersimpan", Toast.LENGTH_LONG).show()
                                review = null
                            }
                        }
                    }) { Text("Simpan Semua") }
                },
                dismissButton = {
                    TextButton(onClick = { review = null }) { Text("Batal") }
                }
            )
        } else {
            // ===== Mode satu total =====
            var amtText by remember(r) {
                mutableStateOf(
                    r.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
                )
            }
            var storeText by remember(r) { mutableStateOf(r.store) }
            AlertDialog(
                onDismissRequest = { review = null },
                title = { Text("Periksa Hasil Scan") },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        if (r.candidates.isNotEmpty()) {
                            Text("Ketuk nominal yang benar:", fontSize = 12.sp)
                            r.candidates.chunked(2).forEach { rowVals ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    rowVals.forEach { v ->
                                        val vStr = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
                                        FilterChip(
                                            selected = amtText == vStr,
                                            onClick = { amtText = vStr },
                                            label = { Text(formatRp(v), fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }
                        OutlinedTextField(
                            value = amtText,
                            onValueChange = { amtText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Nominal (Rp)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = storeText,
                            onValueChange = { storeText = it },
                            label = { Text("Nama toko") },
                            singleLine = true
                        )
                        Text(
                            r.date?.let { "Tanggal terdeteksi: ${formatDate(it)}" }
                                ?: "Tanggal tidak terdeteksi — memakai hari ini (bisa diubah di langkah berikutnya)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (r.items.size >= 2) {
                            TextButton(onClick = { itemsMode = true }) {
                                Text("🧾 Rincikan per item (${r.items.size})", fontSize = 12.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        editing = Txn(
                            id = 0L,
                            amount = amtText.toDoubleOrNull() ?: 0.0,
                            type = TYPE_PENGELUARAN,
                            categoryId = 0L,
                            note = storeText.trim(),
                            date = r.date ?: System.currentTimeMillis()
                        )
                        review = null
                        showDialog = true
                    }) { Text("Lanjutkan") }
                },
                dismissButton = {
                    TextButton(onClick = { review = null }) { Text("Batal") }
                }
            )
        }
    }

    if (showDialog) {
        TxnDialog(
            initial = editing,
            categories = categories,
            assets = assets,
            onDismiss = { showDialog = false },
            onSave = { txn ->
                if (txn.id == 0L) vm.addTxn(txn) else vm.updateTxn(editing!!, txn)
                showDialog = false
            }
        )
    }
}

@Composable
fun TxnDialog(
    initial: Txn?,
    categories: List<Category>,
    assets: List<Asset>,
    onDismiss: () -> Unit,
    onSave: (Txn) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(initial?.type ?: TYPE_PENGELUARAN) }
    var amountText by remember {
        mutableStateOf(
            initial?.amount?.takeIf { it > 0 }
                ?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: ""
        )
    }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var date by remember { mutableStateOf(initial?.date ?: System.currentTimeMillis()) }
    var categoryId by remember { mutableStateOf(initial?.categoryId ?: 0L) }
    var catMenuOpen by remember { mutableStateOf(false) }

    var source by remember {
        mutableIntStateOf(
            when {
                initial == null || initial.assetId == 0L -> SRC_CASH
                initial.assetDeposit -> SRC_TO_ASSET
                else -> SRC_FROM_ASSET
            }
        )
    }
    var assetSel by remember { mutableStateOf(initial?.assetId ?: 0L) }
    var assetMenuOpen by remember { mutableStateOf(false) }

    val filteredCats = categories.filter { it.type == type }
    val selectedCat = filteredCats.find { it.id == categoryId }
    val selectedAsset = assets.find { it.id == assetSel }
    val isNew = initial == null || initial.id == 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Tambah Transaksi" else "Edit Transaksi") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TYPE_PENGELUARAN,
                        onClick = { type = TYPE_PENGELUARAN; categoryId = 0L },
                        label = { Text("Pengeluaran") }
                    )
                    FilterChip(
                        selected = type == TYPE_PEMASUKAN,
                        onClick = {
                            type = TYPE_PEMASUKAN; categoryId = 0L
                            source = SRC_CASH; assetSel = 0L
                        },
                        label = { Text("Pemasukan") }
                    )
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Jumlah (Rp)") },
                    singleLine = true
                )
                Box {
                    OutlinedButton(onClick = { catMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedCat?.name ?: "Pilih kategori")
                    }
                    DropdownMenu(expanded = catMenuOpen, onDismissRequest = { catMenuOpen = false }) {
                        filteredCats.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c.name) },
                                onClick = { categoryId = c.id; catMenuOpen = false }
                            )
                        }
                    }
                }

                // ---------- Sumber dana (khusus pengeluaran) ----------
                if (type == TYPE_PENGELUARAN) {
                    Text("Sumber / tujuan dana:", fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = source == SRC_CASH,
                            onClick = { source = SRC_CASH; assetSel = 0L },
                            label = { Text("Bulanan", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = source == SRC_FROM_ASSET,
                            onClick = { source = SRC_FROM_ASSET },
                            label = { Text("Dari Aset", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = source == SRC_TO_ASSET,
                            onClick = { source = SRC_TO_ASSET },
                            label = { Text("Setor Aset", fontSize = 11.sp) }
                        )
                    }
                    if (source != SRC_CASH) {
                        Box {
                            OutlinedButton(onClick = { assetMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(selectedAsset?.let { "${it.name} (${formatRp(it.value)})" } ?: "Pilih aset")
                            }
                            DropdownMenu(expanded = assetMenuOpen, onDismissRequest = { assetMenuOpen = false }) {
                                assets.forEach { a ->
                                    DropdownMenuItem(
                                        text = { Text("${a.name} — ${formatRp(a.value)}") },
                                        onClick = { assetSel = a.id; assetMenuOpen = false }
                                    )
                                }
                                if (assets.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Belum ada aset — tambah di Lainnya → Aset") },
                                        onClick = { assetMenuOpen = false }
                                    )
                                }
                            }
                        }
                        Text(
                            if (source == SRC_FROM_ASSET)
                                "Nilai aset berkurang; tidak dihitung sebagai pengeluaran bulanan."
                            else
                                "Dihitung pengeluaran bulanan; nilai aset bertambah (menabung).",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = date }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                cal.set(y, m, d)
                                date = cal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Tanggal: ${formatDate(date)}") }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Catatan / nama toko (opsional)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.toDoubleOrNull()
                val assetOk = source == SRC_CASH || assetSel != 0L
                if (amount != null && amount > 0 && categoryId != 0L && assetOk) {
                    onSave(
                        Txn(
                            id = initial?.id ?: 0L,
                            amount = amount,
                            type = type,
                            categoryId = categoryId,
                            note = note.trim(),
                            date = date,
                            assetId = if (source == SRC_CASH) 0L else assetSel,
                            assetDeposit = source == SRC_TO_ASSET
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
