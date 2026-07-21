package com.rio.keuanganku.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rio.keuanganku.FinanceViewModel
import com.rio.keuanganku.data.DEBT_PIUTANG
import com.rio.keuanganku.data.DEBT_UTANG
import com.rio.keuanganku.data.Debt
import com.rio.keuanganku.formatDate
import com.rio.keuanganku.formatRp
import java.util.Calendar

@Composable
fun DebtScreen(vm: FinanceViewModel) {
    val debts by vm.debts.collectAsState()
    var filter by remember { mutableStateOf(DEBT_UTANG) }
    var showAdd by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf<Debt?>(null) }

    val shown = debts.filter { it.type == filter }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Default.Add, "Tambah")
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            SectionTitle("Utang & Piutang")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                FilterChip(
                    selected = filter == DEBT_UTANG,
                    onClick = { filter = DEBT_UTANG },
                    label = { Text("Utang Saya") }
                )
                FilterChip(
                    selected = filter == DEBT_PIUTANG,
                    onClick = { filter = DEBT_PIUTANG },
                    label = { Text("Piutang") }
                )
            }
            if (shown.isEmpty()) {
                EmptyHint(
                    if (filter == DEBT_UTANG) "Tidak ada catatan utang. Alhamdulillah!"
                    else "Tidak ada catatan piutang."
                )
            }
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                items(shown, key = { it.id }) { d ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (d.isSettled) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(10.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(d.person, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(
                                    if (d.isSettled) "LUNAS — total ${formatRp(d.amount)}"
                                    else "Sisa ${formatRp(d.remaining)} dari ${formatRp(d.amount)}",
                                    fontSize = 12.sp,
                                    color = if (d.isSettled) GreenBright
                                    else if (filter == DEBT_UTANG) RedMoney else MaterialTheme.colorScheme.primary
                                )
                                d.dueDate?.let {
                                    Text("Jatuh tempo: ${formatDate(it)}", fontSize = 11.sp, color = Color.Gray)
                                }
                                if (d.note.isNotBlank()) {
                                    Text(d.note, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            if (!d.isSettled) {
                                TextButton(onClick = { paying = d }) { Text("Bayar") }
                            }
                            IconButton(onClick = { vm.deleteDebt(d) }) {
                                Icon(Icons.Default.Delete, "Hapus", tint = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        DebtDialog(
            defaultType = filter,
            onDismiss = { showAdd = false },
            onSave = { vm.addDebt(it); showAdd = false }
        )
    }

    paying?.let { d ->
        var payText by remember(d.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { paying = null },
            title = { Text("Pembayaran: ${d.person}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sisa: ${formatRp(d.remaining)}", fontSize = 13.sp)
                    OutlinedTextField(
                        value = payText,
                        onValueChange = { payText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Jumlah dibayar (Rp)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = payText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        vm.payDebt(d, amount)
                        paying = null
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { paying = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
fun DebtDialog(
    defaultType: String,
    onDismiss: () -> Unit,
    onSave: (Debt) -> Unit
) {
    val context = LocalContext.current
    var type by remember { mutableStateOf(defaultType) }
    var person by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Catatan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == DEBT_UTANG,
                        onClick = { type = DEBT_UTANG },
                        label = { Text("Utang Saya") }
                    )
                    FilterChip(
                        selected = type == DEBT_PIUTANG,
                        onClick = { type = DEBT_PIUTANG },
                        label = { Text("Piutang") }
                    )
                }
                OutlinedTextField(
                    value = person,
                    onValueChange = { person = it },
                    label = { Text(if (type == DEBT_UTANG) "Berutang kepada" else "Nama peminjam") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Jumlah (Rp)") },
                    singleLine = true
                )
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                cal.set(y, m, d)
                                dueDate = cal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(dueDate?.let { "Jatuh tempo: ${formatDate(it)}" } ?: "Atur jatuh tempo (opsional)")
                }
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
                val amount = amountText.toDoubleOrNull()
                if (amount != null && amount > 0 && person.isNotBlank()) {
                    onSave(
                        Debt(
                            person = person.trim(),
                            amount = amount,
                            type = type,
                            dueDate = dueDate,
                            note = note.trim()
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
