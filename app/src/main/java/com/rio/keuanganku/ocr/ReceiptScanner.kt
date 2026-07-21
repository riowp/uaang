package com.rio.keuanganku.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Calendar
import kotlin.math.abs

/**
 * Mesin pembaca struk cerdas (offline, on-device):
 *
 * 1. REKONSTRUKSI BARIS — ML Kit sering memisahkan kolom label ("Total:") dan
 *    kolom angka ("43,500") menjadi baris berbeda. Di sini semua potongan teks
 *    digabung kembali menjadi baris fisik berdasarkan koordinat vertikal
 *    (bounding box), sehingga "Total" dan angkanya menyatu lagi.
 *
 * 2. SISTEM SKOR NOMINAL — setiap kandidat angka dinilai:
 *    baris TOTAL/pembayaran bernilai tinggi, angka berformat ribuan (43.500)
 *    dipercaya, angka panjang tanpa pemisah (nomor struk/check/telepon)
 *    dihukum, baris berisi kata seperti "Check No", "Kasir", "NPWP" dihukum,
 *    dan angka yang muncul berulang (subtotal=total=payment) diberi bonus.
 *
 * 3. TANGGAL — mendukung format angka (10/05/2019) dan nama bulan
 *    (10 May 19, 10 Mei 2019).
 *
 * 4. NAMA TOKO — dicari di bagian atas struk, memilih baris dengan huruf
 *    paling besar (logo), mengabaikan baris alamat/telepon/website.
 */
object ReceiptScanner {

    data class Item(val name: String, val qty: Int, val price: Double)

    data class Result(
        val store: String,
        val amount: Double?,
        val date: Long?,
        val candidates: List<Double>,
        val items: List<Item>,
        val rawText: String
    )

    fun scan(context: Context, uri: Uri, onResult: (Result?) -> Unit) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { onResult(parse(it)) }
                .addOnFailureListener { onResult(null) }
        } catch (e: Exception) {
            onResult(null)
        }
    }

    private val MONEY =
        Regex("""(?:[Rr][Pp]\.?\s*)?(\d{1,3}(?:[.,]\d{3})+(?:[.,]\d{2})?|\d{4,9})""")

    private data class RawLine(val text: String, val x: Int, val y: Int, val h: Int)

    fun parse(vision: Text): Result {
        // ---------- 1. Kumpulkan potongan teks + posisi ----------
        var fallbackY = 0
        val rawLines = vision.textBlocks.flatMap { it.lines }.mapNotNull { l ->
            val t = l.text.trim()
            if (t.isEmpty()) return@mapNotNull null
            fallbackY += 40
            val b = l.boundingBox
            if (b != null) RawLine(t, b.left, b.centerY(), b.height())
            else RawLine(t, 0, fallbackY, 30)
        }
        if (rawLines.isEmpty()) return Result("", null, null, emptyList(), "")

        // ---------- 2. Rekonstruksi baris fisik ----------
        val sorted = rawLines.sortedBy { it.y }
        val heights = sorted.map { it.h }.sorted()
        val medianH = heights[heights.size / 2].coerceAtLeast(10)
        val rowGroups = mutableListOf<MutableList<RawLine>>()
        for (l in sorted) {
            val last = rowGroups.lastOrNull()
            if (last != null && abs(last.first().y - l.y) < medianH * 0.7) {
                last.add(l)
            } else {
                rowGroups.add(mutableListOf(l))
            }
        }
        val rows = rowGroups.map { g -> g.sortedBy { it.x }.joinToString("  ") { it.text } }
        val rawText = rows.joinToString("\n")

        // ---------- 3. Nominal dengan sistem skor ----------
        val payKeys = listOf(
            "payment", "bayar", "tunai", "cash", "debit", "kredit",
            "credit", "qris", "transfer", "gopay", "ovo", "dana", "bca", "bri", "mandiri", "bni"
        )
        val badKeys = listOf(
            "check", "no.", "no :", "no:", "npwp", "telp", "tel.", "phone", "fax",
            "pos", "kasir", "cashier", "struk", "trx", "ref", "member", "kode",
            "www", "http", ".com", "id:", "invoice"
        )

        data class Cand(val value: Double, var score: Int)

        val cands = mutableListOf<Cand>()
        val freq = HashMap<Double, Int>()

        rows.forEach { row ->
            val low = row.lowercase()
            val isTotalRow = low.contains("total") && !low.contains("sub")
            val isPayRow = payKeys.any { low.contains(it) }
            val isChangeRow = low.contains("kembali") || low.contains("change")
            val isBadRow = badKeys.any { low.contains(it) }

            MONEY.findAll(row).forEach { m ->
                val token = m.groupValues[1]
                val v = toNumber(token) ?: return@forEach
                freq[v] = (freq[v] ?: 0) + 1
                var sc = 0
                val hasSep = token.contains('.') || token.contains(',')
                if (isTotalRow) sc += 100
                if (isPayRow) sc += 60
                if (hasSep) sc += 40
                if (!hasSep && token.length >= 7) sc -= 70  // nomor struk / check / telepon
                if (isBadRow && !isTotalRow && !isPayRow) sc -= 90
                if (isChangeRow) sc -= 100                  // uang kembalian bukan total
                if (v in 1000.0..100_000_000.0) sc += 20
                cands.add(Cand(v, sc))
            }
        }
        // Bonus untuk angka yang muncul berulang (subtotal = total = payment)
        cands.forEach { it.score += ((freq[it.value] ?: 1) - 1) * 25 }

        val best = cands.maxWithOrNull(compareBy({ it.score }, { it.value }))
        val amount = best?.takeIf { it.score > -20 }?.value
        val candidates = cands
            .groupBy { it.value }
            .map { (v, list) -> v to list.maxOf { it.score } }
            .sortedWith(compareByDescending<Pair<Double, Int>> { it.second }.thenByDescending { it.first })
            .map { it.first }
            .take(6)

        // ---------- 4. Tanggal ----------
        val date = findDate(rows)

        // ---------- 5. Nama toko ----------
        val store = findStore(rows, rowGroups)

        // ---------- 6. Rincian item per baris ----------
        val items = extractItems(rows, amount)

        return Result(
            store = store, amount = amount, date = date,
            candidates = candidates, items = items, rawText = rawText
        )
    }

    // ================= rincian item =================

    private val itemSkipKeys = listOf(
        "total", "subtotal", "payment", "bayar", "tunai", "cash", "debit", "kredit",
        "credit", "qris", "transfer", "kembali", "change", "disc", "diskon", "voucher",
        "ppn", "tax", "pajak", "service", "rounding", "pembulatan", "dpp", "npwp",
        "telp", "www", "http", "check", "kasir", "pos", "struk", "member", "point",
        "closed", "invoice"
    )

    /**
     * Deteksi baris belanjaan: "1 Bread Butter Pudding 11,500" atau
     * "AQUA 600ML 2 X 3,500 7,000" (harga terakhir = total baris).
     */
    private fun extractItems(rows: List<String>, grandTotal: Double?): List<Item> {
        val items = mutableListOf<Item>()
        val qtyLead = Regex("""^(\d{1,3})\s*[xX]?\s+""")
        val qtyTailX = Regex("""(\d{1,3})\s*[xX]\s*$""")
        val qtyTailNum = Regex("""\s(\d{1,3})\s*$""")

        for (row in rows) {
            val low = row.lowercase()
            if (itemSkipKeys.any { low.contains(it) }) continue
            val ms = MONEY.findAll(row).toList()
            if (ms.isEmpty()) continue
            val last = ms.last()
            val price = toNumber(last.groupValues[1]) ?: continue
            if (price < 100) continue
            if (grandTotal != null && price > grandTotal + 1) continue

            var name = row.substring(0, last.range.first).trim().trimEnd(':', '-', '=', '.', ',', '@', ' ')
            var qty = 1
            qtyLead.find(name)?.let { m ->
                qty = m.groupValues[1].toIntOrNull() ?: 1
                name = name.removeRange(m.range).trim()
            }
            name = MONEY.replace(name, "").trim()
            // qty di akhir nama (format minimarket "NAMA 2 X 3.500 7.000")
            var tail = qtyTailX.find(name)
            if (tail == null && ms.size >= 2) tail = qtyTailNum.find(name)
            if (tail != null) {
                qty = tail.groupValues[1].toIntOrNull() ?: qty
                name = name.substring(0, tail.range.first).trim()
            }
            name = name.trimEnd('@', 'x', 'X', '-', '.', ' ')
            if (name.count { it.isLetter() } < 2) continue
            items.add(Item(name.take(40), qty.coerceIn(1, 999), price))
        }
        return items.take(30)
    }

    // ================= tanggal =================

    private val monthMap = mapOf(
        "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4, "may" to 5, "mei" to 5,
        "jun" to 6, "jul" to 7, "aug" to 8, "agu" to 8, "agt" to 8, "ags" to 8,
        "sep" to 9, "oct" to 10, "okt" to 10, "nov" to 11, "dec" to 12, "des" to 12
    )

    private fun findDate(rows: List<String>): Long? {
        val numDate = Regex("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})""")
        val nameDate = Regex("""(\d{1,2})[\s\-/.]*([A-Za-z]{3,9})[\s\-/.,]*(\d{2,4})""")
        for (row in rows) {
            numDate.findAll(row).forEach { m ->
                buildDate(
                    m.groupValues[1].toIntOrNull(),
                    m.groupValues[2].toIntOrNull(),
                    m.groupValues[3].toIntOrNull()
                )?.let { return it }
            }
            nameDate.findAll(row).forEach { m ->
                val mo = monthMap[m.groupValues[2].lowercase().take(3)]
                if (mo != null) {
                    buildDate(
                        m.groupValues[1].toIntOrNull(),
                        mo,
                        m.groupValues[3].toIntOrNull()
                    )?.let { return it }
                }
            }
        }
        return null
    }

    private fun buildDate(d: Int?, mo: Int?, yRaw: Int?): Long? {
        if (d == null || mo == null || yRaw == null) return null
        var y = yRaw
        if (y < 100) y += 2000
        if (d !in 1..31 || mo !in 1..12 || y !in 2000..2100) return null
        val cal = Calendar.getInstance()
        cal.set(y, mo - 1, d, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // ================= nama toko =================

    private fun findStore(rows: List<String>, rowGroups: List<List<RawLine>>): String {
        val storeBad = listOf(
            "jl.", "jl ", "jalan", "ruko", "blok", "telp", "tel.", "phone", "fax",
            "www", "http", ".com", "npwp", "@", "kec.", "kab.", "kota "
        )

        fun digitRatio(s: String): Double =
            if (s.isEmpty()) 1.0 else s.count { it.isDigit() }.toDouble() / s.length

        val headerCount = minOf(6, rows.size)
        val header = (0 until headerCount).map { i ->
            Triple(rows[i], rowGroups[i].maxOf { it.h }, i)
        }
        val clean = header.filter { (t, _, _) ->
            val low = t.lowercase()
            t.count { it.isLetter() } >= 3 &&
                digitRatio(t) < 0.4 &&
                storeBad.none { low.contains(it) }
        }
        // Baris dengan huruf paling besar biasanya logo/nama toko
        return clean.maxByOrNull { it.second }?.first?.take(40) ?: ""
    }

    // ================= angka =================

    /** Normalisasi angka Indonesia: "125.500" → 125500, "1.250.000,00" → 1250000 */
    private fun toNumber(raw: String): Double? {
        var s = raw.replace(Regex("[Rr][Pp]\\.?"), "").replace(" ", "").trim()
        if (s.isEmpty()) return null
        val m = Regex("^(.*?)([.,])(\\d{2})$").find(s)
        if (m != null && (m.groupValues[1].contains('.') || m.groupValues[1].contains(','))) {
            s = m.groupValues[1]
        }
        s = s.replace(".", "").replace(",", "")
        val v = s.toDoubleOrNull() ?: return null
        return v.takeIf { it in 100.0..5_000_000_000.0 }
    }
}
