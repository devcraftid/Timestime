-- ==============================================================================
-- FIRST LOGISTICS (FL PROOF) - SUPABASE DATABASE & STORAGE SCHEMA
-- ==============================================================================
-- Jalankan skrip SQL ini di Supabase Dashboard -> SQL Editor -> New Query -> Run
-- ==============================================================================

-- 1. TABEL SESI DOKUMENTASI (Satu ID Sesi, misal: FL-2026-000001)
CREATE TABLE IF NOT EXISTS public.evidence_sessions (
    session_id TEXT PRIMARY KEY,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    officer_name TEXT DEFAULT 'FADEL RIPAI',
    category TEXT DEFAULT 'BARANG MASUK',
    address TEXT,
    latitude DOUBLE PRECISION DEFAULT 0.0,
    longitude DOUBLE PRECISION DEFAULT 0.0,
    gps_accuracy REAL DEFAULT 0.0,
    status TEXT DEFAULT 'TERVERIFIKASI',
    barcode_value TEXT,
    notes TEXT
);

-- 2. TABEL MULTI-MEDIA (Dapat berisi BANYAK Foto & BANYAK Video dalam 1 ID Sesi)
CREATE TABLE IF NOT EXISTS public.evidence_media (
    id BIGSERIAL PRIMARY KEY,
    session_id TEXT NOT NULL REFERENCES public.evidence_sessions(session_id) ON DELETE CASCADE,
    media_type TEXT NOT NULL, -- 'photo' atau 'video'
    file_url TEXT NOT NULL,
    session_index INT DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. INDEX UNTUK QUERY CEPAT
CREATE INDEX IF NOT EXISTS idx_evidence_media_session_id ON public.evidence_media(session_id);
CREATE INDEX IF NOT EXISTS idx_evidence_sessions_created_at ON public.evidence_sessions(created_at DESC);

-- 4. AKTIFKAN ROW LEVEL SECURITY (RLS)
ALTER TABLE public.evidence_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.evidence_media ENABLE ROW LEVEL SECURITY;

-- 5. KEBIJAKAN AKSES (POLICIES)
-- Customer/Publik dapat membaca data bukti saat scan QR tanpa perlu login
CREATE POLICY "Public Read Sessions" 
    ON public.evidence_sessions 
    FOR SELECT 
    USING (true);

CREATE POLICY "Public Read Media" 
    ON public.evidence_media 
    FOR SELECT 
    USING (true);

-- Aplikasi FL PROOF Android dapat mengunggah bukti (menggunakan anon key)
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

-- ==============================================================================
-- PANDUAN PENYIMPANAN FILE (SUPABASE STORAGE):
-- 1. Buka Supabase Dashboard -> Storage -> Create a new bucket.
-- 2. Beri nama bucket: flproof-media
-- 3. Centang opsi: "Public bucket" (agar foto & video dapat dibuka oleh customer).
-- 4. Simpan bucket.
-- ==============================================================================

