-- =============================================================================
-- EMPRENDEPIURA - SCRIPT 05: BUCKET STORAGE PARA ADJUNTOS DE CHAT EN TIEMPO REAL
-- =============================================================================
-- Ejecutar en Supabase -> SQL Editor.
-- Crea el bucket publico marketplace-chat para fotos, notas de voz (.m4a) y
-- documentos (PDF, Word, Excel, etc.) enviados en el chat en vivo.
-- =============================================================================

INSERT INTO storage.buckets (id, name, public)
VALUES ('marketplace-chat', 'marketplace-chat', TRUE)
ON CONFLICT (id) DO NOTHING;

DROP POLICY IF EXISTS "Public Read Chat Media" ON storage.objects;
CREATE POLICY "Public Read Chat Media"
ON storage.objects FOR SELECT
USING (bucket_id = 'marketplace-chat');

DROP POLICY IF EXISTS "App Upload Chat Media" ON storage.objects;
CREATE POLICY "App Upload Chat Media"
ON storage.objects FOR INSERT
WITH CHECK (bucket_id = 'marketplace-chat');

DROP POLICY IF EXISTS "App Update Chat Media" ON storage.objects;
CREATE POLICY "App Update Chat Media"
ON storage.objects FOR UPDATE
USING (bucket_id = 'marketplace-chat');
