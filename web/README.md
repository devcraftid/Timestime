# FIRST LOGISTICS — PORTAL BUKTI PELANGGAN (VERCEL & SUPABASE)

Folder ini berisi website portal bukti pelanggan yang siap di-deploy ke **Vercel** dan terhubung ke **Supabase**.

---

### Cara Deploy ke Vercel (Gratis dalam 2 Menit):

#### Metode 1: Menggunakan Vercel CLI
1. Buka terminal di folder `web`:
   ```bash
   cd web
   npx vercel
   ```
2. Ikuti petunjuk di terminal (pilih default).
3. Vercel akan langsung memberikan link publik, contoh: `https://flproof-evidence.vercel.app`.

#### Metode 2: Menggunakan GitHub & Vercel Dashboard
1. Push project ini ke repository GitHub Anda.
2. Buka [https://vercel.com](https://vercel.com) -> Klik **Add New Project**.
3. Pilih repository GitHub Anda.
4. Pada bagian **Root Directory**, pilih folder `web`.
5. Klik **Deploy**!

---

### Cara Menghubungkan ke Supabase:
1. Jalankan skrip `supabase_schema.sql` di Supabase SQL Editor.
2. Buat Storage Bucket bernama `flproof-media` (set Public).
3. Di file `index.html` (atau di aplikasi Android menu Pengaturan):
   - Masukkan **Supabase URL** (`https://xxxx.supabase.co`)
   - Masukkan **Supabase Anon Key**
4. Masukkan URL Vercel Anda di menu Pengaturan FL PROOF Android:
   - Contoh: `https://flproof-evidence.vercel.app/evidence`

Setelah itu, setiap foto atau video yang diambil di FL PROOF akan otomatis tersinkron ke Supabase, dan QR code pada foto akan langsung membuka portal Vercel ini ketika discan oleh siapapun!

