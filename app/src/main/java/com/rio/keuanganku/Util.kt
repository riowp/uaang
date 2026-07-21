package com.rio.keuanganku

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale

val localeID = Locale("in", "ID")

fun formatRp(v: Double): String {
    val nf = NumberFormat.getNumberInstance(localeID)
    nf.maximumFractionDigits = 0
    return "Rp " + nf.format(v)
}

fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy", localeID).format(Date(millis))

fun YearMonth.monthKey(): String = String.format("%04d-%02d", year, monthValue)

fun YearMonth.label(): String =
    month.getDisplayName(TextStyle.FULL, localeID).replaceFirstChar { it.uppercase() } + " " + year

fun Long.toYearMonth(): YearMonth {
    val date = java.time.Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
    return YearMonth.from(date)
}

fun YearMonth.rangeMillis(): Pair<Long, Long> {
    val zone = ZoneId.systemDefault()
    val start = atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val end = plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    return start to end
}
