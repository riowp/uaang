package com.rio.keuanganku.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rio.keuanganku.formatRp
import com.rio.keuanganku.localeID
import java.time.YearMonth
import java.time.format.TextStyle
import kotlin.math.abs

data class Slice(val label: String, val value: Double, val color: Color)

/** Donut chart + legend dengan persentase per kategori */
@Composable
fun DonutWithLegend(slices: List<Slice>, centerLabel: String = "") {
    val total = slices.sumOf { it.value }
    if (total <= 0) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val stroke = 34f
                var start = -90f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(stroke / 2, stroke / 2)
                slices.forEach { s ->
                    val sweep = (s.value / total * 360.0).toFloat()
                    drawArc(
                        color = s.color,
                        startAngle = start,
                        sweepAngle = (sweep - 1.5f).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke)
                    )
                    start += sweep
                }
            }
            if (centerLabel.isNotEmpty()) {
                Text(
                    centerLabel,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(80.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            slices.take(7).forEach { s ->
                val pct = s.value / total * 100
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(s.color, CircleShape)
                    )
                    Text(
                        s.label,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 6.dp)
                    )
                    Text(
                        String.format(localeID, "%.1f%%", pct),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(3.dp))
            }
            if (slices.size > 7) {
                Text(
                    "+${slices.size - 7} kategori lain",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Grafik batang tren 6 bulan: pemasukan vs pengeluaran */
@Composable
fun TrendBarChart(
    months: List<YearMonth>,
    income: List<Double>,
    expense: List<Double>
) {
    val maxVal = (income + expense).maxOrNull() ?: 0.0
    if (maxVal <= 0) {
        EmptyHint("Belum ada data 6 bulan terakhir.")
        return
    }
    val chartH = 140.dp
    Column(Modifier.padding(horizontal = 16.dp)) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .height(chartH + 24.dp)
        ) {
            months.forEachIndexed { i, ym ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Box(
                            Modifier
                                .width(11.dp)
                                .height(((income[i] / maxVal) * chartH.value).dp.coerceAtLeast(2.dp))
                                .background(GreenBright, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                        Spacer(Modifier.width(3.dp))
                        Box(
                            Modifier
                                .width(11.dp)
                                .height(((expense[i] / maxVal) * chartH.value).dp.coerceAtLeast(2.dp))
                                .background(RedMoney, RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                        )
                    }
                    Text(
                        ym.month.getDisplayName(TextStyle.SHORT, localeID),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            LegendDot(GreenBright, "Pemasukan")
            Spacer(Modifier.width(16.dp))
            LegendDot(RedMoney, "Pengeluaran")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(9.dp)
                .background(color, CircleShape)
        )
        Text(label, fontSize = 11.sp, modifier = Modifier.padding(start = 5.dp))
    }
}

/** Kartu perbandingan bulan ini vs bulan lalu dengan panah naik/turun */
@Composable
fun CompareRow(label: String, now: Double, prev: Double, goodWhenUp: Boolean) {
    val diff = now - prev
    val pct = if (prev > 0) diff / prev * 100 else null
    val up = diff >= 0
    val good = if (goodWhenUp) up else !up
    val color = if (diff == 0.0) MaterialTheme.colorScheme.onSurfaceVariant
    else if (good) GreenBright else RedMoney
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Bulan lalu: ${formatRp(prev)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatRp(now), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                (if (up) "▲ " else "▼ ") + formatRp(abs(diff)) +
                        (pct?.let { String.format(localeID, " (%.1f%%)", abs(it)) } ?: ""),
                fontSize = 11.sp,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
