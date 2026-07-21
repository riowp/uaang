package com.rio.keuanganku.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rio.keuanganku.data.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

@Composable
fun BackupScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { pendingRestoreUri = it }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        SectionTitle("Backup & Restore Data")
        Text(
            "Backup menyimpan SELURUH data (transaksi, aset, anggaran, utang piutang, rencana, " +
                "kategori) ke satu file di folder Download/KeuanganKu. Simpan file ini di tempat aman " +
                "(mis. Google Drive) sebelum ganti HP atau uninstall aplikasi.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Button(
            onClick = {
                scope.launch {
                    val msg = withContext(Dispatchers.IO) { BackupManager.backup(context) }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) { Text("💾 Backup Sekarang") }

        OutlinedButton(
            onClick = { pickBackup.launch("*/*") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) { Text("📂 Pulihkan dari File Backup") }

        Text(
            "Catatan: pemulihan akan MENGGANTI seluruh data saat ini dengan isi file backup, " +
                "lalu aplikasi dibuka ulang otomatis.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("Pulihkan data?") },
            text = {
                Text(
                    "Seluruh data saat ini akan diganti dengan isi file backup. " +
                        "Tindakan ini tidak bisa dibatalkan. Lanjutkan?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val result = withContext(Dispatchers.IO) { BackupManager.restore(context, uri) }
                        pendingRestoreUri = null
                        if (result == "OK") {
                            Toast.makeText(context, "Data dipulihkan. Membuka ulang…", Toast.LENGTH_SHORT).show()
                            restartApp(context)
                        } else {
                            Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Ya, Pulihkan") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreUri = null }) { Text("Batal") }
            }
        )
    }
}

private fun restartApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    exitProcess(0)
}
