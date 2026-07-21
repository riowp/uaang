package com.rio.keuanganku.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun MoreScreen(nav: NavController) {
    Column(Modifier.fillMaxSize()) {
        SectionTitle("Lainnya")
        MoreItem("🎯", "Rencana Keuangan", "Target beli rumah, mobil, dll dengan proyeksi otomatis") {
            nav.navigate("plan")
        }
        MoreItem("💰", "Aset", "Catat dan pantau total aset yang dimiliki") {
            nav.navigate("asset")
        }
        MoreItem("🤝", "Utang & Piutang", "Kelola utang, piutang, dan cicilan pembayaran") {
            nav.navigate("debt")
        }
        MoreItem("🏷️", "Kategori", "Sesuaikan kategori pemasukan & pengeluaran") {
            nav.navigate("category")
        }
        MoreItem("🔒", "Keamanan", "Passcode & sidik jari untuk membuka aplikasi") {
            nav.navigate("security")
        }
        MoreItem("🕘", "Riwayat", "Riwayat transaksi bulanan & perubahan aset sepanjang waktu") {
            nav.navigate("history")
        }
        MoreItem("💾", "Backup & Restore", "Simpan / pulihkan seluruh data — penting sebelum ganti HP") {
            nav.navigate("backup")
        }
        Text(
            "KeuanganKu v1.7 — data tersimpan lokal di perangkat, tanpa internet.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun MoreItem(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(emoji, fontSize = 22.sp)
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
