#!/bin/bash

# ================= KONFIGURASI =================
# Ganti ini dengan nama file skrip K6 Anda
K6_SCRIPT="product-get-test.js"
# ===============================================

# Periksa apakah file skrip K6 ada
if [ ! -f "$K6_SCRIPT" ]; then
    echo "Error: Skrip K6 '$K6_SCRIPT' tidak ditemukan."
    exit 1
fi

# Loop 5 kali untuk menjalankan pengujian
for i in {1..5}
do
  echo "================================================="
  echo "Memulai pengujian K6 putaran ke-$i dari 5..."
  echo "================================================="

  # Jalankan K6 dan simpan output detail ke file JSON yang diberi nama unik
  k6 run "$K6_SCRIPT" --summary-export "result-run-$i.json"

  echo "Putaran ke-$i selesai. Hasil disimpan ke result-run-$i.json"

  # Opsional: Tambahkan jeda singkat antar pengujian jika diperlukan
  # echo "Berhenti sejenak selama 10 detik sebelum putaran berikutnya..."
  # sleep 10
done

echo "================================================="
echo "Semua 5 putaran pengujian telah selesai."
echo "================================================="