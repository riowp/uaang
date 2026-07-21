package com.rio.keuanganku.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rio.keuanganku.FinanceViewModel
import com.rio.keuanganku.data.Category
import com.rio.keuanganku.data.TYPE_PENGELUARAN
import com.rio.keuanganku.data.fundedByAsset
import com.rio.keuanganku.formatRp
import com.rio.keuanganku.label

@Composable
fun BudgetScreen(vm: FinanceViewModel) {
    val month by vm.month.collectAsState()
    val categories by vm.categories.collectAsState()
    val budgets by vm.budgets.collectAsState()
    val txns by vm.txns.collectAsState()

    val expenseCats = categories.filter { it.type == TYPE_PENGELUARAN }
    val budgetMap = budgets.associateBy { it.categoryId }

    var editingCat by remember { mutableStateOf<Category?>(null) }

    Column(Modifier.fillMaxSize()) {
        SectionTitle("Anggaran — ${month.label()}")
        Text(
            "Ketuk kategori untuk mengatur anggaran bulan ini. Ganti bulan lewat Dashboard.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
            items(expenseCats, key = { it.id }) { cat ->
                val b = budgetMap[cat.id]
                val spent = txns.filter { it.type == TYPE_PENGELUARAN && !it.fundedByAsset && it.categoryId == cat.id }
                    .sumOf { it.amount }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(cat.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text(
                                if (b != null) "Anggaran: ${formatRp(b.amount)} • Terpakai: ${formatRp(spent)}"
                                else "Belum diatur • Terpakai: ${formatRp(spent)}",
                                fontSize = 12.sp,
                                color = if (b != null && spent > b.amount) RedMoney else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = { editingCat = cat }) {
                            Text(if (b == null) "Atur" else "Ubah")
                        }
                        if (b != null) {
                            IconButton(onClick = { vm.deleteBudget(b) }) {
                                Icon(Icons.Default.Delete, "Hapus anggaran", tint = Color.Gray)
                            }
                        }
                    }
                    if (b != null && b.amount > 0) {
                        val frac = (spent / b.amount).toFloat()
                        LinearProgressIndicator(
                            progress = { frac.coerceIn(0f, 1f) },
                            color = if (spent > b.amount) RedMoney else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        )
                    }
                }
            }
        }
    }

    editingCat?.let { cat ->
        var amountText by remember(cat.id) {
            mutableStateOf(
                budgetMap[cat.id]?.amount?.let {
                    if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
                } ?: ""
            )
        }
        AlertDialog(
            onDismissRequest = { editingCat = null },
            title = { Text("Anggaran: ${cat.name}") },
            text = {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Jumlah anggaran (Rp)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        vm.setBudget(cat.id, amount)
                        editingCat = null
                    }
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { editingCat = null }) { Text("Batal") }
            }
        )
    }
}
