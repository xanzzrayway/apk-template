# Template Android WebWrap

Ini template project yang dipakai web compiler buat generate APK.

## Setup (sekali doang)

1. Bikin repo GitHub baru (public atau private), misal `apk-builder`.
2. Push semua isi folder ini ke repo tersebut (root repo = isi folder ini).
3. Bikin Personal Access Token (Settings → Developer settings → Fine-grained tokens):
   - Scope: **Actions (read/write)** dan **Contents (read)** untuk repo tersebut.
4. Buka `compiler.html`, isi kolom Owner, Repo, dan Token dengan punya kamu.

## Cara kerja

Web compiler kirim event `repository_dispatch` ke repo ini lewat GitHub API.
Workflow `.github/workflows/build-apk.yml` otomatis jalan, isi nama/package/URL/HTML
ke project (`configure.py`), lalu build APK debug pakai Gradle, dan upload hasilnya
sebagai artifact yang bisa didownload dari web compiler atau tab Actions repo.

## Catatan

- APK yang dihasilkan pakai **debug signing** — cukup buat sideload/install manual,
  bukan buat upload ke Play Store.
- Icon default masih placeholder ungu-putih. Ganti `ic_launcher.xml` kalau mau custom.
