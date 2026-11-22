-- =============================================================================
-- EMPRENDEPIURA - SCRIPT 03: CHAT REALTIME PROFESIONAL Y BLOQUEOS PERSISTENTES
-- =============================================================================
-- Ejecutar en Supabase -> SQL Editor sobre la base ya creada con 01_schema.
-- Habilita:
--   1. Bloqueo persistente y bidireccional (comprador o emprendedor) en conversaciones.
--   2. Proteccion en base de datos para impedir mensajes en chats bloqueados.
--   3. REPLICA IDENTITY FULL para recibir UPDATE/DELETE completos en Realtime
--      (doble visto azul en vivo, bloqueos instantaneos y eliminacion sincronizada).
-- =============================================================================

ALTER TABLE public.conversaciones
    ADD COLUMN IF NOT EXISTS bloqueado_por UUID REFERENCES public.usuarios(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS bloqueado_en  TIMESTAMPTZ;

ALTER TABLE public.conversaciones REPLICA IDENTITY FULL;
ALTER TABLE public.mensajes       REPLICA IDENTITY FULL;

CREATE OR REPLACE FUNCTION public.validar_bloqueo_antes_de_mensaje()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_bloqueado_por UUID;
BEGIN
    SELECT bloqueado_por
    INTO v_bloqueado_por
    FROM public.conversaciones
    WHERE id = NEW.conversacion_id;

    IF v_bloqueado_por IS NOT NULL THEN
        RAISE EXCEPTION 'Esta conversacion se encuentra bloqueada.';
    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_validar_bloqueo_mensaje ON public.mensajes;
CREATE TRIGGER trg_validar_bloqueo_mensaje
BEFORE INSERT ON public.mensajes
FOR EACH ROW
EXECUTE FUNCTION public.validar_bloqueo_antes_de_mensaje();

DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.mensajes; EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.conversaciones; EXCEPTION WHEN duplicate_object THEN NULL; END $$;
