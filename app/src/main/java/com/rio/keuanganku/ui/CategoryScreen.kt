package com.rio.keuanganku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rio.keuanganku.FinanceViewModel
import com.rio.keuanganku.data.Category
import com.rio.keuanganku.data.TYPE_PEMASUKAN
import com.rio.keuanganku.data.TYPE_PENGELUARAN

private val PRESET_COLORS = listOf(
    0xFF009540, 0xFF005840, 0xFFDDD40E, 0xFFE53935, 0xFFF4511E,
    0xFFFB8C00, 0xFF3949AB, 0xFF8E24AA, 0xFF6D4C41, 0xFF00ACC1,
    0xFF7CB342, 0xFFFFB300, 0xFF757575, 0xFF00897B, 0xFF2E7D32
)

@Composable
fun CategoryScreen(vm: FinanceViewModel) {
    val categories by vm.categories.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<Category?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Default.Add, "Tambah kategori")
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            SectionTitle("Kategori")
            Text(
                "Kategori dapat ditambah atau dihapus sesuai kebutuhan rumah tangga.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                val pengeluaran = categories.filter { it.type == TYPE_PENGELUARAN }
                val pemasukan = categories.filter { it.type == TYPE_PEMASUKAN }

                items(listOf("PENGELUARAN_HEADER")) {
                    Text(
                        "Pengeluaran",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = RedMoney,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                items(pengeluaran, key = { "e${it.id}" }) { c ->
                    CategoryRow(c) { confirmDelete = c }
                }
                items(listOf("PEMASUKAN_HEADER")) {
                    Text(
                        "Pemasukan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = GreenBright,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                items(pemasukan, key = { "i${it.id}" }) { c ->
                    CategoryRow(c) { confirmDelete = c }
                }
            }
        }
    }

    if (showDialog) {
        CategoryDialog(
            onDismiss = { showDialog = false },
            onSave = { vm.addCategory(it); showDialog = false }
        )
    }

    confirmDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Hapus kategori?") },
            text = { Text("Kategori \"${c.name}\" akan dihapus. Transaksi lama dengan kategori ini akan tampil sebagai \"Lainnya\".") },
            confirmButton = {
                TextButton(onClick = { vm.deleteCategory(c); confirmDelete = null }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun CategoryRow(c: Category, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Box(
            Modifier
                .size(16.dp)
                .background(Color(c.color), CircleShape)
        )
        Text(c.name, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, "Hapus", tint = Color.Gray)
        }
    }
}

@Composable
fun CategoryDialog(
    onDismiss: () -> Unit,
    onSave: (Category) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TYPE_PENGELUARAN) }
    var color by remember { mutableStateOf(PRESET_COLORS.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Kategori") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TYPE_PENGELUARAN,
                        onClick = { type = TYPE_PENGELUARAN },
                        label = { Text("Pengeluaran") }
                    )
                    FilterChip(
                        selected = type == TYPE_PEMASUKAN,
                        onClick = { type = TYPE_PEMASUKAN },
                        label = { Text("Pemasukan") }
                    )
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama kategori") },
                    singleLine = true
                )
                Text("Warna:", fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PRESET_COLORS.take(8).forEach { c ->
                        ColorDot(c, selected = c == color) { color = c }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PRESET_COLORS.drop(8).forEach { c ->
                        ColorDot(c, selected = c == color) { color = c }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isNotBlank()) {
                    onSave(Category(name = name.trim(), type = type, color = color))
                }
            }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun ColorDot(colorValue: Long, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(28.dp)
            .background(Color(colorValue), CircleShape)
            .border(
                width = if (selected) 3.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() }
    )
}
