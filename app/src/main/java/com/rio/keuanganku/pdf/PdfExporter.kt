package com.rio.keuanganku.pdf

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore
import com.rio.keuanganku.data.Asset
import com.rio.keuanganku.data.Budget
import com.rio.keuanganku.data.Category
import com.rio.keuanganku.data.DEBT_PIUTANG
import com.rio.keuanganku.data.DEBT_UTANG
import com.rio.keuanganku.data.Debt
import com.rio.keuanganku.data.TYPE_PEMASUKAN
import com.rio.keuanganku.data.TYPE_PENGELUARAN
import com.rio.keuanganku.data.Txn
import com.rio.keuanganku.data.fundedByAsset
import com.rio.keuanganku.formatDate
import com.rio.keuanganku.formatRp
import com.rio.keuanganku.label
import com.rio.keuanganku.localeID
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.util.Date

object PdfExporter {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 36f

    private val GREEN = Color.rgb(0, 149, 64)
    private val DARK_GREEN = Color.rgb(0, 88, 64)
    private val YELLOW = Color.rgb(221, 212, 14)
    private val RED = Color.rgb(211, 47, 47)
    private val GREY = Color.rgb(117, 117, 117)
    private val LIGHT = Color.rgb(240, 245, 241)

    fun export(
        context: Context,
        ym: YearMonth,
        txns: List<Txn>,
        categories: List<Category>,
        budgets: List<Budget>,
        debts: List<Debt>,
        assets: List<Asset> = emptyList()
    ): String {
        return try {
            val doc = PdfDocument()
            var pageNum = 1
            var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
            var canvas = page.canvas
            var y: Float

            val catMap = categories.associateBy { it.id }
            fun catName(id: Long) = catMap[id]?.name ?: "Lainnya"

            val titlePaint = Paint().apply {
                color = Color.WHITE; textSize = 20f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
            }
            val subPaint = Paint().apply { color = Color.WHITE; textSize = 12f; isAntiAlias = true }
            val hPaint = Paint().apply {
                color = DARK_GREEN; textSize = 13f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
            }
            val tPaint = Paint().apply { color = Color.BLACK; textSize = 10f; isAntiAlias = true }
            val tBold = Paint().apply {
                color = Color.BLACK; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true
            }
            val small = Paint().apply { color = GREY; textSize = 8f; isAntiAlias = true }
            val fill = Paint().apply { style = Paint.Style.FILL; isAntiAlias = true }

            fun drawHeader() {
                fill.color = GREEN
                canvas.drawRect(0f, 0f, PAGE_W.toFloat(), 80f, fill)
                fill.color = YELLOW
                canvas.drawRect(0f, 80f, PAGE_W.toFloat(), 84f, fill)
                canvas.drawText("Laporan Keuangan Rumah Tangga", MARGIN, 36f, titlePaint)
                canvas.drawText("Periode: ${ym.label()}", MARGIN, 56f, subPaint)
                val gen = SimpleDateFormat("dd MMM yyyy HH:mm", localeID).format(Date())
                canvas.drawText("Dibuat: $gen  •  KeuanganKu", MARGIN, 70f, subPaint)
            }

            fun footer() {
                canvas.drawText("Halaman $pageNum", PAGE_W - MARGIN - 50f, PAGE_H - 20f, small)
            }

            fun newPage() {
                footer()
                doc.finishPage(page)
                pageNum++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
                canvas = page.canvas
            }

            fun ensure(space: Float, currentY: Float): Float {
                return if (currentY + space > PAGE_H - 40f) {
                    newPage(); 50f
                } else currentY
            }

            drawHeader()
            y = 110f

            // ---------- Ringkasan ----------
            val income = txns.filter { it.type == TYPE_PEMASUKAN }.sumOf { it.amount }
            val expense = txns.filter { it.type == TYPE_PENGELUARAN && !it.fundedByAsset }.sumOf { it.amount }
            val balance = income - expense

            val boxW = (PAGE_W - 2 * MARGIN - 20f) / 3f
            val labels = listOf("Pemasukan", "Pengeluaran", "Saldo")
            val values = listOf(income, expense, balance)
            val colors = listOf(GREEN, RED, if (balance >= 0) DARK_GREEN else RED)
            for (i in 0..2) {
                val x = MARGIN + i * (boxW + 10f)
                fill.color = LIGHT
                canvas.drawRoundRect(RectF(x, y, x + boxW, y + 58f), 8f, 8f, fill)
                fill.color = colors[i]
                canvas.drawRoundRect(RectF(x, y, x + boxW, y + 6f), 3f, 3f, fill)
                canvas.drawText(labels[i], x + 10f, y + 24f, tPaint)
                val vp = Paint(tBold).apply { color = colors[i]; textSize = 12f }
                canvas.drawText(formatRp(values[i]), x + 10f, y + 44f, vp)
            }
            y += 80f

            // ---------- Pengeluaran per Kategori ----------
            canvas.drawText("Pengeluaran per Kategori", MARGIN, y, hPaint)
            y += 14f
            val byCat = txns.filter { it.type == TYPE_PENGELUARAN && !it.fundedByAsset }
                .groupBy { it.categoryId }
                .map { (id, list) -> catName(id) to list.sumOf { it.amount } }
                .sortedByDescending { it.second }
            if (byCat.isEmpty()) {
                canvas.drawText("Belum ada pengeluaran bulan ini.", MARGIN, y + 10f, tPaint)
                y += 24f
            } else {
                val maxVal = byCat.maxOf { it.second }
                val barMaxW = PAGE_W - 2 * MARGIN - 220f
                for ((name, amt) in byCat) {
                    y = ensure(18f, y)
                    canvas.drawText(name, MARGIN, y + 9f, tPaint)
                    val barW = if (maxVal > 0) (amt / maxVal * barMaxW).toFloat() else 0f
                    fill.color = GREEN
                    canvas.drawRoundRect(RectF(MARGIN + 140f, y, MARGIN + 140f + barW.coerceAtLeast(2f), y + 11f), 3f, 3f, fill)
                    canvas.drawText(formatRp(amt), MARGIN + 150f + barW, y + 9f, tBold)
                    y += 17f
                }
            }
            y += 16f

            // ---------- Status Anggaran ----------
            y = ensure(40f, y)
            canvas.drawText("Status Anggaran (Budget)", MARGIN, y, hPaint)
            y += 14f
            if (budgets.isEmpty()) {
                canvas.drawText("Belum ada anggaran yang diatur untuk bulan ini.", MARGIN, y + 10f, tPaint)
                y += 24f
            } else {
                // table header
                fill.color = DARK_GREEN
                canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 16f, fill)
                val wp = Paint(tBold).apply { color = Color.WHITE }
                canvas.drawText("Kategori", MARGIN + 6f, y + 12f, wp)
                canvas.drawText("Anggaran", MARGIN + 170f, y + 12f, wp)
                canvas.drawText("Realisasi", MARGIN + 270f, y + 12f, wp)
                canvas.drawText("Sisa", MARGIN + 370f, y + 12f, wp)
                canvas.drawText("Status", MARGIN + 460f, y + 12f, wp)
                y += 16f
                var stripe = false
                for (b in budgets) {
                    y = ensure(16f, y)
                    val spent = txns.filter { it.type == TYPE_PENGELUARAN && !it.fundedByAsset && it.categoryId == b.categoryId }.sumOf { it.amount }
                    val sisa = b.amount - spent
                    if (stripe) {
                        fill.color = LIGHT
                        canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 15f, fill)
                    }
                    stripe = !stripe
                    canvas.drawText(catName(b.categoryId), MARGIN + 6f, y + 11f, tPaint)
                    canvas.drawText(formatRp(b.amount), MARGIN + 170f, y + 11f, tPaint)
                    canvas.drawText(formatRp(spent), MARGIN + 270f, y + 11f, tPaint)
                    val sp = Paint(tBold).apply { color = if (sisa >= 0) GREEN else RED }
                    canvas.drawText(formatRp(sisa), MARGIN + 370f, y + 11f, sp)
                    canvas.drawText(if (sisa >= 0) "Aman" else "Over", MARGIN + 460f, y + 11f, sp)
                    y += 15f
                }
            }
            y += 16f

            // ---------- Utang Piutang ----------
            y = ensure(60f, y)
            canvas.drawText("Utang & Piutang", MARGIN, y, hPaint)
            y += 14f
            val totalUtang = debts.filter { it.type == DEBT_UTANG && !it.isSettled }.sumOf { it.remaining }
            val totalPiutang = debts.filter { it.type == DEBT_PIUTANG && !it.isSettled }.sumOf { it.remaining }
            val up = Paint(tBold).apply { color = RED }
            val pp = Paint(tBold).apply { color = GREEN }
            canvas.drawText("Total Utang (belum lunas): ${formatRp(totalUtang)}", MARGIN, y + 10f, up)
            canvas.drawText("Total Piutang (belum lunas): ${formatRp(totalPiutang)}", MARGIN + 260f, y + 10f, pp)
            y += 26f
            val activeDebts = debts.filter { !it.isSettled }
            for (d in activeDebts) {
                y = ensure(16f, y)
                val tag = if (d.type == DEBT_UTANG) "[Utang]" else "[Piutang]"
                val due = d.dueDate?.let { "  •  Jatuh tempo: ${formatDate(it)}" } ?: ""
                canvas.drawText("$tag ${d.person} — sisa ${formatRp(d.remaining)} dari ${formatRp(d.amount)}$due", MARGIN, y + 10f, tPaint)
                y += 15f
            }
            if (activeDebts.isEmpty()) {
                canvas.drawText("Tidak ada utang/piutang aktif. Mantap!", MARGIN, y + 10f, tPaint)
                y += 20f
            }
            y += 16f

            // ---------- Aset ----------
            if (assets.isNotEmpty()) {
                y = ensure(50f, y)
                canvas.drawText("Aset", MARGIN, y, hPaint)
                y += 14f
                val totalAset = assets.sumOf { it.value }
                val ap2 = Paint(tBold).apply { color = DARK_GREEN; textSize = 11f }
                canvas.drawText("Total Aset: ${formatRp(totalAset)}  (${assets.size} aset)", MARGIN, y + 10f, ap2)
                y += 24f
                for (a in assets) {
                    y = ensure(15f, y)
                    canvas.drawText("• ${a.name} (${a.kind})", MARGIN, y + 10f, tPaint)
                    canvas.drawText(formatRp(a.value), MARGIN + 330f, y + 10f, tBold)
                    y += 14f
                }
                y += 16f
            }

            // ---------- Rincian Transaksi ----------
            y = ensure(40f, y)
            canvas.drawText("Rincian Transaksi (${txns.size} transaksi)", MARGIN, y, hPaint)
            y += 14f
            fill.color = DARK_GREEN
            canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 16f, fill)
            val wp2 = Paint(tBold).apply { color = Color.WHITE }
            canvas.drawText("Tanggal", MARGIN + 6f, y + 12f, wp2)
            canvas.drawText("Kategori", MARGIN + 90f, y + 12f, wp2)
            canvas.drawText("Catatan", MARGIN + 230f, y + 12f, wp2)
            canvas.drawText("Jumlah", MARGIN + 430f, y + 12f, wp2)
            y += 16f
            var stripe2 = false
            for (t in txns) {
                y = ensure(15f, y)
                if (stripe2) {
                    fill.color = LIGHT
                    canvas.drawRect(MARGIN, y, PAGE_W - MARGIN, y + 14f, fill)
                }
                stripe2 = !stripe2
                canvas.drawText(formatDate(t.date), MARGIN + 6f, y + 10f, tPaint)
                canvas.drawText(catName(t.categoryId).take(24), MARGIN + 90f, y + 10f, tPaint)
                val noteTag = if (t.assetId != 0L) (if (t.assetDeposit) "[Setor Aset] " else "[Dana Aset] ") else ""
                canvas.drawText((noteTag + t.note).take(34), MARGIN + 230f, y + 10f, tPaint)
                val ap = Paint(tBold).apply { color = if (t.type == TYPE_PEMASUKAN) GREEN else RED }
                val sign = if (t.type == TYPE_PEMASUKAN) "+" else "-"
                canvas.drawText(sign + formatRp(t.amount), MARGIN + 430f, y + 10f, ap)
                y += 14f
            }

            footer()
            doc.finishPage(page)

            // ---------- Simpan ke Download ----------
            val fileName = "Laporan_Keuangan_${ym.monthKeySafe()}.pdf"
            val values2 = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/KeuanganKu")
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values2)
                ?: return "Gagal membuat file PDF"
            resolver.openOutputStream(uri)?.use { doc.writeTo(it) }
            doc.close()
            "PDF tersimpan: Download/KeuanganKu/$fileName"
        } catch (e: Exception) {
            "Gagal export PDF: ${e.message}"
        }
    }

    private fun YearMonth.monthKeySafe(): String = String.format("%04d_%02d", year, monthValue)
}
