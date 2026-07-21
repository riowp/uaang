package com.rio.keuanganku.ui

import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.rio.keuanganku.security.SecurityManager

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val pinLen = remember { SecurityManager.pinLength(context) }
    val bioAvailable = remember {
        SecurityManager.isBiometricEnabled(context) && SecurityManager.canUseBiometric(context)
    }

    fun showBiometric() {
        val act = activity ?: return
        val executor = ContextCompat.getMainExecutor(act)
        val prompt = BiometricPrompt(
            act, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Buka KeuanganKu")
            .setSubtitle("Gunakan sidik jari")
            .setNegativeButtonText("Pakai Passcode")
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(Unit) {
        if (bioAvailable) showBiometric()
    }

    fun press(d: String) {
        if (entered.length >= pinLen) return
        error = false
        entered += d
        if (entered.length == pinLen) {
            if (SecurityManager.verify(context, entered)) {
                onUnlocked()
            } else {
                error = true
                entered = ""
            }
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AdaroGreen, AdaroDark)))
    ) {
        Text("🔒", fontSize = 42.sp)
        Text(
            "KeuanganKu",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            "Masukkan passcode",
            color = Color(0xCCFFFFFF),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
        Spacer(Modifier.height(24.dp))

        // Titik indikator PIN
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            repeat(pinLen) { i ->
                Box(
                    Modifier
                        .size(16.dp)
                        .background(
                            if (i < entered.length) AdaroYellow else Color(0x44FFFFFF),
                            CircleShape
                        )
                )
            }
        }
        Text(
            if (error) "Passcode salah, coba lagi" else " ",
            color = Color(0xFFFFD6D6),
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
        Spacer(Modifier.height(16.dp))

        // Keypad
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9")
        )
        rows.forEach { r ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                r.forEach { d -> KeyButton(d) { press(d) } }
            }
            Spacer(Modifier.height(14.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            if (bioAvailable) {
                KeyButton("👆") { showBiometric() }
            } else {
                Spacer(Modifier.size(72.dp))
            }
            KeyButton("0") { press("0") }
            KeyButton("⌫") { entered = entered.dropLast(1) }
        }
    }
}

@Composable
private fun KeyButton(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(72.dp)
            .background(Color(0x26FFFFFF), CircleShape)
            .clickable { onClick() }
    ) {
        Text(label, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
    }
}
