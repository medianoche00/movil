-- ============================================================================
-- Script 01b: Migracion incremental para unificar Login Biometrico (DNI + Huella + Rostro)
--             en la tabla public.usuarios ya creada por el Script 01.
-- ============================================================================

-- 1. Agregar columnas de autenticacion biometrica a public.usuarios
ALTER TABLE public.usuarios
    ADD COLUMN IF NOT EXISTS dni            TEXT,
    ADD COLUMN IF NOT EXISTS huella         TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS imagen_facial  TEXT NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS vector_facial  TEXT NOT NULL DEFAULT '';

-- 2. Si existieran filas previas sin DNI, asignarles uno temporal unico antes de volverlo NOT NULL
WITH pendientes AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY creado_en ASC, id ASC) AS rn
    FROM public.usuarios
    WHERE dni IS NULL OR TRIM(dni) = ''
)
UPDATE public.usuarios u
SET dni = CASE u.correo
    WHEN 'admin@unpmarket.pe'       THEN '00000001'
    WHEN 'emprendedor@unpmarket.pe' THEN '00000002'
    WHEN 'carlos.craft@unpmarket.pe' THEN '00000003'
    WHEN 'comprador@unpmarket.pe'   THEN '00000004'
    ELSE LPAD((90000000 + p.rn)::TEXT, 8, '0')
END
FROM pendientes p
WHERE u.id = p.id;

-- 3. Hacer el DNI obligatorio y unico para login rapido 1:1
ALTER TABLE public.usuarios
    ALTER COLUMN dni SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_usuarios_dni_unique ON public.usuarios(dni);

-- 4. Bucket publico dedicado para fotos faciales de usuarios
INSERT INTO storage.buckets (id, name, public)
VALUES ('marketplace-rostros', 'marketplace-rostros', TRUE)
ON CONFLICT (id) DO NOTHING;

DROP POLICY IF EXISTS "Public Read Marketplace" ON storage.objects;
CREATE POLICY "Public Read Marketplace"
ON storage.objects FOR SELECT
USING (bucket_id IN ('marketplace-productos', 'marketplace-comprobantes', 'marketplace-rostros'));

DROP POLICY IF EXISTS "App Upload Marketplace" ON storage.objects;
CREATE POLICY "App Upload Marketplace"
ON storage.objects FOR INSERT
WITH CHECK (bucket_id IN ('marketplace-productos', 'marketplace-comprobantes', 'marketplace-rostros'));

DROP POLICY IF EXISTS "App Update Marketplace" ON storage.objects;
CREATE POLICY "App Update Marketplace"
ON storage.objects FOR UPDATE
USING (bucket_id IN ('marketplace-productos', 'marketplace-comprobantes', 'marketplace-rostros'));
