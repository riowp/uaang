package com.rio.keuanganku.ui

import android.app.DatePickerDialog
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.rio.keuanganku.data.Plan
import com.rio.keuanganku.formatDate
import com.rio.keuanganku.formatRp
import com.rio.keuanganku.label
import com.rio.keuanganku.localeID
import com.rio.keuanganku.toYearMonth
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.ln
import kotlin.math.pow

// ---------- Perhitungan proyeksi ----------

private fun monthlyRate(annualPct: Double): Double =
    if (annualPct > 0) (1 + annualPct / 100).pow(1.0 / 12.0) - 1 else 0.0

/** Berapa bulan sampai target tercapai dengan setoran tetap per bulan */
fun monthsToTarget(current: Double, target: Double, pmt: Double, annualPct: Double): Int? {
    if (target <= 0) return null
    if (current >= target) return 0
    val r = monthlyRate(annualPct)
    return if (r == 0.0) {
        if (pmt <= 0) null else ceil((target - current) / pmt).toInt()
    } else {
        if (pmt <= 0 && current <= 0) return null
        val num = target * r + pmt
        val den = current * r + pmt
        if (den <= 0 || num <= den && pmt <= 0) return null
        val n = ln(num / den) / ln(1 + r)
        if (n.isNaN() || n.isInfinite() || n < 0) null else ceil(n).toInt()
    }
}

/** Setoran wajib per bulan agar target tercapai dalam n bulan */
fun requiredMonthly(current: Double, target: Double, months: Int, annualPct: Double): Double? {
    if (months <= 0) return null
    if (current >= target) return 0.0
    val r = monthlyRate(annualPct)
    return if (r == 0.0) {
        (target - current) / months
    } else {
        val f = (1 + r).pow(months)
        (((target - current * f) * r) / (f - 1)).coerceAtLeast(0.0)
    }
}

fun fmtDuration(months: Int): String {
    val y = months / 12
    val m = months % 12
    return when {
        months == 0 -> "sudah tercapai 🎉"
        y == 0 -> "$m bulan"
        m == 0 -> "$y tahun"
        else -> "$y tahun $m bulan"
    }
}

// ---------- UI ----------

@Composable
fun PlanScreen(vm: FinanceViewModel) {
    val plans by vm.plans.collectAsState()
    val assets by vm.assets.collectAsState()
    val assetMap = assets.associateBy { it.id }

    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Plan?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showDialog = true }) {
                Icon(Icons.Default.Add, "Tambah rencana")
            }
        }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            SectionTitle("Rencana Keuangan")
            if (plans.isEmpty()) {
                EmptyHint(
                    "Belum ada rencana. Tekan + untuk membuat target, misalnya: Beli Rumah, " +
                        "Beli Mobil, Umroh, atau Dana Pendidikan. Aplikasi akan menghitung proyeksi " +
                        "waktu tercapai dan setoran wajib per bulan secara otomatis."
                )
            }
            LazyColumn {
                items(plans, key = { it.id }) { plan ->
                    PlanCard(
                        plan = plan,
                        linkedAsset = assetMap[plan.linkedAssetId],
                        onEdit = { editing = plan; showDialog = true },
                        onDelete = { vm.deletePlan(plan) }
                    )
                }
            }
        }
    }

    if (showDialog) {
        PlanDialog(
            initial = editing,
            assets = assets,
            onDismiss = { showDialog = false },
            onSave = { plan ->
                if (plan.id == 0L) vm.addPlan(plan) else vm.updatePlan(plan)
                showDialog = false
            }
        )
    }
}

@Composable
private fun PlanCard(
    plan: Plan,
    linkedAsset: Asset?,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val current = linkedAsset?.value ?: plan.currentAmount
    val pct = if (plan.targetAmount > 0) (current / plan.targetAmount * 100).coerceIn(0.0, 100.0) else 0.0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clickable { onEdit() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("🎯 ${plan.name}", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Target: ${formatRp(plan.targetAmount)}" +
                            (linkedAsset?.let { " • tautan aset: ${it.name}" } ?: ""),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Hapus", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Progress
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Terkumpul: ${formatRp(current)}", fontSize = 12.sp)
                Text(
                    String.format(localeID, "%.1f%%", pct),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = { (pct / 100.0).toFloat() },
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .padding(top = 2.dp)
            )

            // ---------- Proyeksi 1: dengan setoran saat ini ----------
            if (plan.monthlyDeposit > 0 && current < plan.targetAmount) {
                val months = monthsToTarget(current, plan.targetAmount, plan.monthlyDeposit, plan.annualReturnPct)
                if (months != null) {
                    val eta = YearMonth.now().plusMonths(months.toLong())
                    Text(
                        "📈 Setoran ${formatRp(plan.monthlyDeposit)}/bln" +
                            (if (plan.annualReturnPct > 0) " (imbal ${plan.annualReturnPct}%/thn)" else "") +
                            " → tercapai dalam ${fmtDuration(months)} (± ${eta.label()})",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Text(
                        "📈 Setoran belum cukup untuk mencapai target — naikkan setoran per bulan.",
                        fontSize = 12.sp,
                        color = RedMoney,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // ---------- Proyeksi 2: setoran wajib agar tercapai sesuai target tanggal ----------
            plan.targetDate?.let { td ->
                val monthsLeft = ChronoUnit.MONTHS.between(YearMonth.now(), td.toYearMonth()).toInt()
                if (monthsLeft > 0 && current < plan.targetAmount) {
                    val need = requiredMonthly(current, plan.targetAmount, monthsLeft, plan.annualReturnPct)
                    if (need != null) {
                        val enough = plan.monthlyDeposit >= need && plan.monthlyDeposit > 0
                        Text(
                            "🗓️ Agar tercapai ${formatDate(td)} (${fmtDuration(monthsLeft)} lagi), " +
                                "wajib setor ${formatRp(need)}/bln" +
                                if (plan.monthlyDeposit > 0) {
                                    if (enough) " — setoran Anda sudah cukup ✅"
                                    else " — kurang ${formatRp(need - plan.monthlyDeposit)}/bln ⚠️"
                                } else "",
                            fontSize = 12.sp,
                            color = if (plan.monthlyDeposit > 0 && !enough) RedMoney
                            else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                } else if (monthsLeft <= 0 && current < plan.targetAmount) {
                    Text(
                        "🗓️ Target tanggal sudah lewat — perbarui rencana.",
                        fontSize = 12.sp,
                        color = RedMoney,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (current >= plan.targetAmount && plan.targetAmount > 0) {
                Text(
                    "🎉 Target tercapai! Selamat!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenBright,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (plan.note.isNotBlank()) {
                Text(
                    plan.note,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun PlanDialog(
    initial: Plan?,
    assets: List<Asset>,
    onDismiss: () -> Unit,
    onSave: (Plan) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var targetText by remember { mutableStateOf(initial?.targetAmount?.toLongString() ?: "") }
    var currentText by remember { mutableStateOf(initial?.currentAmount?.toLongString() ?: "") }
    var depositText by remember { mutableStateOf(initial?.monthlyDeposit?.toLongString() ?: "") }
    var returnText by remember { mutableStateOf(initial?.annualReturnPct?.takeIf { it > 0 }?.toString() ?: "") }
    var targetDate by remember { mutableStateOf(initial?.targetDate) }
    var linkedAssetId by remember { mutableStateOf(initial?.linkedAssetId ?: 0L) }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var assetMenuOpen by remember { mutableStateOf(false) }

    val linkedAsset = assets.find { it.id == linkedAssetId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Rencana Baru" else "Edit Rencana") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama target (mis. Beli Rumah)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() } },
                    label = { Text("Budget / target dana (Rp)") },
                    singleLine = true
                )
                Box {
                    OutlinedButton(onClick = { assetMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(linkedAsset?.let { "Tautan aset: ${it.name}" } ?: "Tautkan ke aset (opsional)")
                    }
                    DropdownMenu(expanded = assetMenuOpen, onDismissRequest = { assetMenuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Tidak ditautkan (isi manual)") },
                            onClick = { linkedAssetId = 0L; assetMenuOpen = false }
                        )
                        assets.forEach { a ->
                            DropdownMenuItem(
                                text = { Text("${a.name} — ${formatRp(a.value)}") },
                                onClick = { linkedAssetId = a.id; assetMenuOpen = false }
                            )
                        }
                    }
                }
                if (linkedAssetId == 0L) {
                    OutlinedTextField(
                        value = currentText,
                        onValueChange = { currentText = it.filter { c -> c.isDigit() } },
                        label = { Text("Dana terkumpul saat ini (Rp)") },
                        singleLine = true
                    )
                } else {
                    Text(
                        "Dana terkumpul otomatis mengikuti nilai aset \"${linkedAsset?.name}\".",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = depositText,
                    onValueChange = { depositText = it.filter { c -> c.isDigit() } },
                    label = { Text("Rencana setoran per bulan (Rp)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = returnText,
                    onValueChange = { returnText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Asumsi imbal hasil %/tahun (opsional)") },
                    singleLine = true
                )
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { targetDate?.let { timeInMillis = it } }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                cal.set(y, m, d)
                                targetDate = cal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(targetDate?.let { "Target tercapai: ${formatDate(it)}" } ?: "Atur target tanggal (opsional)")
                }
                if (targetDate != null) {
                    TextButton(onClick = { targetDate = null }) { Text("Hapus target tanggal") }
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
                val target = targetText.toDoubleOrNull()
                if (name.isNotBlank() && target != null && target > 0) {
                    onSave(
                        Plan(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            targetAmount = target,
                            currentAmount = currentText.toDoubleOrNull() ?: 0.0,
                            monthlyDeposit = depositText.toDoubleOrNull() ?: 0.0,
                            annualReturnPct = returnText.toDoubleOrNull() ?: 0.0,
                            targetDate = targetDate,
                            linkedAssetId = linkedAssetId,
                            note = note.trim(),
                            createdAt = initial?.createdAt ?: System.currentTimeMillis()
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

private fun Double.toLongString(): String? =
    takeIf { it > 0 }?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }
