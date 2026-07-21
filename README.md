# KeuanganKu — Aplikasi Pencatatan Keuangan Rumah Tangga

Aplikasi Android (Kotlin + Jetpack Compose + Room) untuk pencatatan keuangan rumah tangga, **100% offline**, dengan export dashboard ke PDF.

## Fitur

1. **Pelaporan otomatis** — setiap tanggal 1, laporan PDF bulan sebelumnya dibuat otomatis (via WorkManager), tersimpan di `Download/KeuanganKu/`, dan muncul notifikasi.
2. **Pencatatan transaksi** — pemasukan & pengeluaran dengan kategori, tanggal, dan catatan. Bisa edit & hapus.
3. **Manajemen anggaran** — atur budget per kategori per bulan, progress bar realisasi, peringatan over budget.
4. **Manajemen utang piutang** — catat utang saya & piutang, pembayaran bertahap (cicilan), jatuh tempo, status lunas otomatis.
5. **Kategori yang dapat disesuaikan** — tambah/hapus kategori pemasukan & pengeluaran dengan pilihan warna. 15 kategori default sudah tersedia.
6. **Export PDF dashboard** — ringkasan pemasukan/pengeluaran/saldo, grafik batang per kategori, tabel status anggaran, daftar utang piutang, dan rincian transaksi. Tersimpan di folder **Download/KeuanganKu**.
7. **Dashboard keuangan** — saldo total sepanjang waktu, pemasukan/pengeluaran/sisa uang bulan ini, dan donut chart pengeluaran per kategori dengan warna kategori.
8. **Grafik analisis** (menu Analisis) — perbandingan dengan bulan lalu (▲▼ + persentase), persentase kategori (donut), tren 6 bulan (pemasukan vs pengeluaran), dan pengeluaran terbesar (top 5 kategori & transaksi).
9. **Scan Struk (OCR)** — foto struk lewat kamera atau pilih dari galeri; aplikasi membaca nominal total, tanggal, dan nama toko secara on-device (ML Kit, tetap offline setelah install), lalu mengisi form transaksi otomatis.
10. **Dark mode** — toggle 🌙/☀️ di header Dashboard, tersimpan permanen.
11. **Manajemen aset** (Lainnya → Aset) — catat tabungan, deposito, reksa dana, saham, emas, properti, kendaraan; total aset dan kekayaan bersih tampil di Beranda dan ikut ter-export di PDF.
12. **Passcode & sidik jari** (Lainnya → Keamanan) — kunci aplikasi dengan PIN 4-8 digit (disimpan sebagai hash SHA-256 + salt, bukan teks asli) dan opsi buka cepat via fingerprint. Bisa diubah/dinonaktifkan kapan saja.

13. **Target aset** — setiap aset bisa punya target uang terkumpul, dengan progress bar dan sisa kekurangan.
14. **Transaksi berbasis aset** — saat mencatat pengeluaran, pilih sumber dana: *Uang Bulanan* (normal), *Dari Aset* (nilai aset berkurang, TIDAK dihitung pengeluaran bulanan/anggaran), atau *Setor Aset* (dihitung pengeluaran bulanan + nilai aset bertambah, cocok untuk menabung/investasi rutin). Nilai aset otomatis menyesuaikan saat transaksi ditambah, diedit, atau dihapus.
15. **Rencana Keuangan** (Lainnya → Rencana Keuangan) — buat target seperti Beli Rumah, Beli Mobil, Umroh, Dana Pendidikan. Aplikasi otomatis menghitung: (a) proyeksi berapa lama target tercapai dengan setoran bulanan saat ini (mendukung asumsi imbal hasil %/tahun, rumus future value anuitas), dan (b) setoran wajib per bulan agar tercapai sesuai target tanggal. Rencana bisa ditautkan ke aset sehingga progress mengikuti nilai aset secara live.

## Catatan Update v1.3
- Database naik ke versi 3 dengan migrasi otomatis (target aset, kolom aset di transaksi, tabel rencana) — data lama aman.
- Saldo kas & semua agregasi bulanan kini mengecualikan pengeluaran yang dananya dari aset.

## Catatan Update v1.2
- Navigasi bawah dirapikan jadi 5 tab (Beranda, Transaksi, Analisis, Anggaran, Lainnya) supaya label tidak terpotong. Utang & Piutang, Kategori, Aset, dan Keamanan pindah ke tab Lainnya.
- Database naik ke versi 2 dengan migrasi otomatis — data lama tetap aman saat update APK.

## Catatan Update v1.7 — Scan Struk per Item
- **Rincian belanjaan otomatis**: scanner kini mendeteksi setiap baris item pada struk (nama, qty, harga) — mendukung format "1 Bread Butter Pudding 11,500" maupun format minimarket "AQUA 600ML 2 X 3,500 7,000" (harga terakhir = total baris; qty di awal atau di tengah terdeteksi).
- **Dialog Rincian per Item**: saat struk berisi ≥2 item, muncul daftar item dengan checkbox, kategori per item (atau "terapkan ke semua"), dan total terpilih. Satu ketukan "Simpan Semua" membuat SATU TRANSAKSI PER ITEM dengan catatan "Toko — Item". Tetap bisa beralih ke mode satu total.

## Catatan Update v1.6 — Scan Struk Jauh Lebih Akurat
- **Mesin pembaca struk baru** (tetap 100% offline): teks OCR direkonstruksi menjadi baris fisik berdasarkan koordinat, sehingga label "Total" dan angkanya yang terpisah kolom kembali menyatu. Setiap kandidat nominal dinilai dengan sistem skor — baris Total/pembayaran diprioritaskan, angka berformat ribuan dipercaya, nomor struk/check/telepon dihukum, angka yang berulang (subtotal=total=payment) diberi bonus.
- Tanggal kini mendukung format nama bulan ("10 May 19", "10 Mei 2026"), dan nama toko dideteksi dari baris berhuruf terbesar di bagian atas struk.
- **Dialog verifikasi hasil scan**: sebelum masuk transaksi, nominal-nominal kandidat ditampilkan sebagai tombol pilihan — jika deteksi utama salah, tinggal ketuk angka yang benar. Nama toko dan tanggal juga bisa dikoreksi.

## Catatan Update v1.5
- **Tambah/Kurangi dana aset langsung** — tombol ➕/➖ di setiap kartu aset; tidak perlu lagi edit dan hitung total manual. Setiap perubahan tercatat di riwayat.
- **Riwayat lengkap** (Lainnya → Riwayat): tab *Bulanan* menampilkan seluruh transaksi sepanjang waktu dikelompokkan per bulan dengan total masuk/keluar/sisa; tab *Aset* menampilkan semua perubahan nilai aset (saldo awal, tambah/kurang manual, dari transaksi, penyesuaian edit) lengkap dengan saldo akhir. Database naik ke v4 (migrasi otomatis, data aman).
- **Analisis Pencapaian Target** di menu Analisis: progress semua rencana keuangan + target aset, proyeksi waktu tercapai, dan setoran wajib per bulan — semua di satu tempat.

## Catatan Update v1.4 — Update APK Tanpa Uninstall
- **Keystore permanen** (`keystore/keuanganku.jks`) kini dipakai untuk menandatangani semua build. Sebelumnya, setiap build GitHub Actions memakai debug key acak yang berbeda, sehingga Android menolak update dan memaksa uninstall. Mulai versi ini, semua APK bertanda tangan sama → **install langsung menimpa versi lama, data tetap aman**.
- **versionCode otomatis** naik mengikuti nomor run GitHub Actions — tidak perlu diubah manual lagi.
- **PENTING — hanya untuk update pertama kali ke v1.4:** APK lama masih bertanda tangan key acak, jadi khusus sekali ini Android tetap minta uninstall. Urutannya: (1) buka aplikasi lama → Lainnya → *jika sudah punya menu Backup gunakan itu*, kalau belum, catat/export PDF data penting dulu; (2) uninstall; (3) install APK v1.4; (4) input ulang / pulihkan data. Setelah itu, semua update berikutnya tinggal install di atasnya.
- Fitur baru **Backup & Restore** (Lainnya → Backup & Restore): simpan seluruh database ke satu file di Download/KeuanganKu, dan pulihkan kapan saja (mis. saat ganti HP). Biasakan backup sebelum update besar.

## Cara Build (GitHub Actions — tanpa Android Studio)

1. Buat repository baru di GitHub (private boleh).
2. Upload seluruh isi folder ini ke repository (branch `main`).
3. Buka tab **Actions** → workflow **Build APK** akan berjalan otomatis (atau klik **Run workflow**).
4. Setelah selesai (±5-8 menit), download artifact **KeuanganKu-debug-apk**.
5. Extract zip artifact → install `app-debug.apk` di HP (izinkan "install dari sumber tidak dikenal").

## Teknis

- minSdk 29 (Android 10+), targetSdk 34
- Jetpack Compose + Material 3, warna tema hijau #009540
- Room database (lokal, tidak butuh internet)
- PDF dibuat native dengan `android.graphics.pdf.PdfDocument` (tanpa library eksternal)
- WorkManager untuk pelaporan otomatis bulanan
