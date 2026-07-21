package com.rio.keuanganku.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rio.keuanganku.MainActivity
import com.rio.keuanganku.data.AppDatabase
import com.rio.keuanganku.pdf.PdfExporter
import com.rio.keuanganku.rangeMillis
import com.rio.keuanganku.label
import com.rio.keuanganku.monthKey
import java.time.LocalDate
import java.time.YearMonth

/**
 * Pelaporan otomatis: worker ini berjalan harian.
 * Setiap tanggal 1, laporan PDF bulan sebelumnya dibuat otomatis
 * dan disimpan ke folder Download/KeuanganKu, lalu muncul notifikasi.
 */
class MonthlyReportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now()
        if (today.dayOfMonth != 1) return Result.success()

        // Cegah dobel di hari yang sama
        val prefs = applicationContext.getSharedPreferences("report", Context.MODE_PRIVATE)
        val ym = YearMonth.now().minusMonths(1)
        val doneKey = "done_" + ym.monthKey()
        if (prefs.getBoolean(doneKey, false)) return Result.success()

        val db = AppDatabase.get(applicationContext)
        val (s, e) = ym.rangeMillis()
        val txns = db.txnDao().byRangeOnce(s, e)
        val cats = db.categoryDao().allOnce()
        val budgets = db.budgetDao().byMonthOnce(ym.monthKey())
        val debts = db.debtDao().allOnce()
        val assets = db.assetDao().allOnce()

        val msg = PdfExporter.export(applicationContext, ym, txns, cats, budgets, debts, assets)
        prefs.edit().putBoolean(doneKey, true).apply()
        notify("Laporan ${ym.label()} siap", msg)
        return Result.success()
    }

    private fun notify(title: String, text: String) {
        try {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= 26) {
                nm.createNotificationChannel(
                    NotificationChannel("report", "Laporan Bulanan", NotificationManager.IMPORTANCE_DEFAULT)
                )
            }
            val intent = Intent(applicationContext, MainActivity::class.java)
            val pi = PendingIntent.getActivity(
                applicationContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notif = NotificationCompat.Builder(applicationContext, "report")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            nm.notify(1001, notif)
        } catch (_: SecurityException) {
            // izin notifikasi belum diberikan — laporan tetap tersimpan di Download
        }
    }
}
