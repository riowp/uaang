package com.rio.keuanganku.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rio.keuanganku.security.SecurityManager

@Composable
fun SecurityScreen() {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val passSet = remember(refresh) { SecurityManager.isPasscodeSet(context) }
    var bio by remember(refresh) { mutableStateOf(SecurityManager.isBiometricEnabled(context)) }
    val canBio = remember { SecurityManager.canUseBiometric(context) }

    var showSet by remember { mutableStateOf(false) }
    var showChange by remember { mutableStateOf(false) }
    var showDisable by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp)
    ) {
        SectionTitle("Keamanan")
        Text(
            "Lindungi data keuangan Anda dengan passcode. Passcode diminta setiap kali aplikasi dibuka.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (!passSet) {
            Button(
                onClick = { showSet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) { Text("Aktifkan Passcode") }
        } else {
            Text(
                "✅ Passcode aktif",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Button(
                onClick = { showChange = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) { Text("Ubah Passcode") }
            OutlinedButton(
                onClick = { showDisable = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) { Text("Nonaktifkan Passcode") }

            if (canBio) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Buka dengan sidik jari", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Gunakan biometrik sebagai alternatif passcode",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = bio,
                        onCheckedChange = {
                            bio = it
                            SecurityManager.setBiometricEnabled(context, it)
                        }
                    )
                }
            } else {
                Text(
                    "Perangkat ini tidak mendukung / belum mendaftarkan sidik jari.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    // ---------- Dialog: aktifkan passcode ----------
    if (showSet) {
        PinDialog(
            title = "Buat Passcode",
            fields = listOf("Passcode baru (4-8 digit)", "Ulangi passcode"),
            onDismiss = { showSet = false },
            onConfirm = { values ->
                val (p1, p2) = values
                when {
                    p1.length !in 4..8 -> Toast.makeText(context, "Passcode harus 4-8 digit", Toast.LENGTH_SHORT).show()
                    p1 != p2 -> Toast.makeText(context, "Passcode tidak sama", Toast.LENGTH_SHORT).show()
                    else -> {
                        SecurityManager.setPasscode(context, p1)
                        Toast.makeText(context, "Passcode aktif", Toast.LENGTH_SHORT).show()
                        showSet = false
                        refresh++
                    }
                }
            }
        )
    }

    // ---------- Dialog: ubah passcode ----------
    if (showChange) {
        PinDialog(
            title = "Ubah Passcode",
            fields = listOf("Passcode lama", "Passcode baru (4-8 digit)", "Ulangi passcode baru"),
            onDismiss = { showChange = false },
            onConfirm = { values ->
                val old = values[0]; val p1 = values[1]; val p2 = values[2]
                when {
                    !SecurityManager.verify(context, old) -> Toast.makeText(context, "Passcode lama salah", Toast.LENGTH_SHORT).show()
                    p1.length !in 4..8 -> Toast.makeText(context, "Passcode harus 4-8 digit", Toast.LENGTH_SHORT).show()
                    p1 != p2 -> Toast.makeText(context, "Passcode baru tidak sama", Toast.LENGTH_SHORT).show()
                    else -> {
                        SecurityManager.setPasscode(context, p1)
                        Toast.makeText(context, "Passcode diubah", Toast.LENGTH_SHORT).show()
                        showChange = false
                        refresh++
                    }
                }
            }
        )
    }

    // ---------- Dialog: nonaktifkan ----------
    if (showDisable) {
        PinDialog(
            title = "Nonaktifkan Passcode",
            fields = listOf("Masukkan passcode saat ini"),
            onDismiss = { showDisable = false },
            onConfirm = { values ->
                if (SecurityManager.verify(context, values[0])) {
                    SecurityManager.clearPasscode(context)
                    Toast.makeText(context, "Passcode dinonaktifkan", Toast.LENGTH_SHORT).show()
                    showDisable = false
                    refresh++
                } else {
                    Toast.makeText(context, "Passcode salah", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
private fun PinDialog(
    title: String,
    fields: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    val values = remember { fields.map { mutableStateOf("") } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                fields.forEachIndexed { i, label ->
                    OutlinedTextField(
                        value = values[i].value,
                        onValueChange = { v -> values[i].value = v.filter { it.isDigit() }.take(8) },
                        label = { Text(label) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(values.map { it.value }) }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
