package com.rio.keuanganku.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.rio.keuanganku.localeID
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Backup & restore seluruh data (database Room) ke/dari file.
 * Berguna saat pindah HP atau sebelum reinstall aplikasi.
 */
object BackupManager {

    private const val DB_NAME = "keuanganku.db"

    fun backup(context: Context): String {
        return try {
            // Pastikan semua perubahan WAL tertulis ke file utama
            val db = AppDatabase.get(context)
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }

            val src = context.getDatabasePath(DB_NAME)
            if (!src.exists()) return "Belum ada data untuk di-backup"

            val name = "KeuanganKu_backup_" +
                SimpleDateFormat("yyyyMMdd_HHmm", localeID).format(Date()) + ".db"
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/KeuanganKu")
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return "Gagal membuat file backup"
            context.contentResolver.openOutputStream(uri)?.use { out ->
                src.inputStream().use { it.copyTo(out) }
            }
            "Backup tersimpan: Download/KeuanganKu/$name"
        } catch (e: Exception) {
            "Gagal backup: ${e.message}"
        }
    }

    /** Mengembalikan "OK" jika sukses; setelah itu aplikasi WAJIB di-restart. */
    fun restore(context: Context, uri: Uri): String {
        return try {
            AppDatabase.closeInstance()
            val dbFile = context.getDatabasePath(DB_NAME)
            dbFile.parentFile?.mkdirs()
            File(dbFile.path + "-wal").delete()
            File(dbFile.path + "-shm").delete()
            val input = context.contentResolver.openInputStream(uri)
                ?: return "Gagal membaca file backup"
            input.use { ins ->
                dbFile.outputStream().use { ins.copyTo(it) }
            }
            "OK"
        } catch (e: Exception) {
            "Gagal restore: ${e.message}"
        }
    }
}
