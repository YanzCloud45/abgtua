# RucoyFloat

Eksperimen floating-window khusus Rucoy Online untuk Android root.

## Fungsi
- Membuat VirtualDisplay 1280x720.
- Menampilkan Surface VirtualDisplay di overlay draggable/resizable.
- Menjalankan `com.mmo.android` ke display virtual memakai root shell.
- Meneruskan tap/swipe sederhana memakai `input -d <DISPLAY_ID>`.

## Build di AIDE Pro
1. Extract ZIP.
2. Buka folder `RucoyFloat` sebagai Gradle Android project.
3. Pastikan Android SDK 35 tersedia. Jika AIDE tidak punya SDK 35, ubah:
   - `compileSdk 35` menjadi versi SDK yang tersedia, minimal 33 disarankan.
   - `targetSdk 35` bisa ikut diturunkan untuk tes lokal.
4. Build APK lalu install.
5. Jalankan app dan izinkan:
   - akses root,
   - "Display over other apps".
6. Tekan `START RUCOY FLOAT`.

## Catatan penting Android 15
Android modern membatasi activity dari UID lain di virtual display tertentu.
Project ini memakai root + `am start --display`, tetapi beberapa ROM tetap dapat
menolak launch ke display yang dianggap tidak trusted.

Kalau log/layar tetap hitam, cek dari root shell:

    dumpsys display | grep -A8 RucoyFloatDisplay

Lalu coba manual:

    am start --display DISPLAY_ID -n COMPONENT_RUCOY

Cari component Rucoy:

    cmd package resolve-activity --brief \
      -a android.intent.action.MAIN \
      -c android.intent.category.LAUNCHER \
      com.mmo.android

## Input
Versi 1 meneruskan:
- tap
- swipe / drag sederhana

Belum mendukung multitouch penuh. Untuk Rucoy, mayoritas tombol/tap dasar harusnya
bisa dites lebih dulu sebelum bikin injector input yang lebih ribet.

## Package
`biz.shikuro.rucoyfloat`
