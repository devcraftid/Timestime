-- ==============================================================================
-- FIRST LOGISTICS (FL PROOF) - SUPABASE COMPLETE DATABASE & STORAGE SCRIPT
-- ==============================================================================
-- Buka Supabase Dashboard -> SQL Editor -> New Query -> Paste script ini -> Klik Run
-- ==============================================================================

-- 1. BUAT STORAGE BUCKET: flproof-media (Public: Foto & Video dapat diakses Customer)
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'flproof-media',
    'flproof-media',
    true,
    104857600, -- 100 MB max per file
    ARRAY['image/jpeg', 'image/png', 'image/webp', 'video/mp4', 'video/quicktime', 'application/pdf']
)
ON CONFLICT (id) DO UPDATE SET 
    public = true,
    file_size_limit = 104857600,
    allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'image/webp', 'video/mp4', 'video/quicktime', 'application/pdf'];

-- 2. KEBIJAKAN AKSES STORAGE (storage.objects)
-- Reset kebijakan lama jika ada agar tidak bentrok
DROP POLICY IF EXISTS "Public Read flproof-media" ON storage.objects;
DROP POLICY IF EXISTS "Public Insert flproof-media" ON storage.objects;
DROP POLICY IF EXISTS "Public Update flproof-media" ON storage.objects;
DROP POLICY IF EXISTS "Public Delete flproof-media" ON storage.objects;

-- Siapapun (customer/publik saat scan QR) bisa melihat & mengunduh foto/video
CREATE POLICY "Public Read flproof-media"
ON storage.objects FOR SELECT
USING (bucket_id = 'flproof-media');

-- Aplikasi Android dapat mengunggah file foto & video (menggunakan anon key)
CREATE POLICY "Public Insert flproof-media"
ON storage.objects FOR INSERT
WITH CHECK (bucket_id = 'flproof-media');

-- Aplikasi Android dapat memperbarui file jika terjadi upload ulang (upsert)
CREATE POLICY "Public Update flproof-media"
ON storage.objects FOR UPDATE
USING (bucket_id = 'flproof-media');

-- 3. TABEL SESI DOKUMENTASI (Metadata Sesi, misal: FL-2026-000001)
CREATE TABLE IF NOT EXISTS public.evidence_sessions (
    session_id TEXT PRIMARY KEY,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    officer_name TEXT DEFAULT 'FADEL RIPAI',
    category TEXT DEFAULT 'BARANG MASUK',
    address TEXT,
    latitude DOUBLE PRECISION DEFAULT 0.0,
    longitude DOUBLE PRECISION DEFAULT 0.0,
    gps_accuracy REAL DEFAULT 0.0,
    status TEXT DEFAULT 'TERVERIFIKASI LENGKAP',
    barcode_value TEXT,
    notes TEXT
);

-- 4. TABEL MULTI-MEDIA (Bisa BANYAK Foto & BANYAK Video dalam 1 ID Sesi)
CREATE TABLE IF NOT EXISTS public.evidence_media (
    id BIGSERIAL PRIMARY KEY,
    session_id TEXT NOT NULL REFERENCES public.evidence_sessions(session_id) ON DELETE CASCADE,
    media_type TEXT NOT NULL, -- 'photo' atau 'video'
    file_url TEXT NOT NULL,
    session_index INT DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. INDEX UNTUK PERFORMA QUERY
CREATE INDEX IF NOT EXISTS idx_evidence_media_session_id ON public.evidence_media(session_id);
CREATE INDEX IF NOT EXISTS idx_evidence_sessions_created_at ON public.evidence_sessions(created_at DESC);

-- 6. ROW LEVEL SECURITY (RLS) PADA TABEL
ALTER TABLE public.evidence_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.evidence_media ENABLE ROW LEVEL SECURITY;

-- Reset policies tabel agar aman dijalankan berulang kali
DROP POLICY IF EXISTS "Public Read Sessions" ON public.evidence_sessions;
DROP POLICY IF EXISTS "Public Read Media" ON public.evidence_media;
DROP POLICY IF EXISTS "App Insert Sessions" ON public.evidence_sessions;
DROP POLICY IF EXISTS "App Insert Media" ON public.evidence_media;

-- Customer dapat membaca informasi bukti via web/QR
CREATE POLICY "Public Read Sessions" 
    ON public.evidence_sessions 
    FOR SELECT 
    USING (true);

CREATE POLICY "Public Read Media" 
    ON public.evidence_media 
    FOR SELECT 
    USING (true);

-- Aplikasi Android dapat menambah/update sesi & file media
CREATE POLICY "App Insert Sessions" 
    ON public.evidence_sessions 
    FOR ALL 
    USING (true) 
    WITH CHECK (true);

CREATE POLICY "App Insert Media" 
    ON public.evidence_media 
    FOR ALL 
    USING (true) 
    WITH CHECK (true);
