-- ============================================================================
-- Script 01: Esquema Completo - EmprendePiura (Marketplace Local Piura / UNP)
-- Incluye campos biometricos en usuarios (dni, huella, imagen_facial, vector_facial)
-- Nota: Si ya ejecutaste la version anterior de 01_schema, ejecuta 01b_alter_usuarios_biometria.sql
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1. USUARIOS (Acceso biometrico 1:1 + 3 roles: COMPRADOR, EMPRENDEDOR, ADMIN)
CREATE TABLE IF NOT EXISTS public.usuarios (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    dni             TEXT NOT NULL UNIQUE,
    nombre          TEXT NOT NULL,
    correo          TEXT NOT NULL UNIQUE,
    telefono        TEXT NOT NULL DEFAULT '',
    password_hash   TEXT NOT NULL,
    rol             TEXT NOT NULL DEFAULT 'COMPRADOR'
                        CHECK (rol IN ('COMPRADOR', 'EMPRENDEDOR', 'ADMIN')),
    zona_piura      TEXT NOT NULL DEFAULT 'Campus UNP',
    huella          TEXT NOT NULL DEFAULT '',
    imagen_facial   TEXT NOT NULL DEFAULT '',
    vector_facial   TEXT NOT NULL DEFAULT '',
    avatar_url      TEXT,
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 2. EMPRENDIMIENTOS (Activacion aprobada por el Administrador)
CREATE TABLE IF NOT EXISTS public.emprendimientos (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id                UUID NOT NULL UNIQUE REFERENCES public.usuarios(id) ON DELETE CASCADE,
    nombre_tienda             TEXT NOT NULL,
    descripcion               TEXT NOT NULL DEFAULT '',
    categoria_principal       TEXT NOT NULL DEFAULT 'Alimentos y Postres',
    zona_piura                TEXT NOT NULL DEFAULT 'Campus UNP',
    punto_encuentro_habitual  TEXT NOT NULL DEFAULT 'Puerta Principal UNP',
    telefono_contacto         TEXT NOT NULL,
    numero_yape_plin          TEXT DEFAULT '',
    qr_pago_url               TEXT,
    logo_url                  TEXT,
    estado                    TEXT NOT NULL DEFAULT 'PENDIENTE'
                                  CHECK (estado IN ('PENDIENTE', 'APROBADO', 'SUSPENDIDO')),
    motivo_revision           TEXT DEFAULT '',
    creado_en                 TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION public.sync_rol_al_aprobar_emprendimiento()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.actualizado_en := NOW();
    IF NEW.estado = 'APROBADO' THEN
        UPDATE public.usuarios SET rol = 'EMPRENDEDOR'
        WHERE id = NEW.usuario_id AND rol <> 'ADMIN';
    ELSIF NEW.estado = 'SUSPENDIDO' THEN
        UPDATE public.usuarios SET rol = 'COMPRADOR'
        WHERE id = NEW.usuario_id AND rol = 'EMPRENDEDOR';
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_rol_emprendimiento ON public.emprendimientos;
CREATE TRIGGER trg_sync_rol_emprendimiento
BEFORE INSERT OR UPDATE OF estado ON public.emprendimientos
FOR EACH ROW
EXECUTE FUNCTION public.sync_rol_al_aprobar_emprendimiento();

-- 3. CATEGORIAS
CREATE TABLE IF NOT EXISTS public.categorias (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre    TEXT NOT NULL UNIQUE,
    icono     TEXT NOT NULL DEFAULT 'category',
    orden     INT NOT NULL DEFAULT 0,
    activa    BOOLEAN NOT NULL DEFAULT TRUE
);

-- 4. PRODUCTOS
CREATE TABLE IF NOT EXISTS public.productos (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    emprendimiento_id UUID NOT NULL REFERENCES public.emprendimientos(id) ON DELETE CASCADE,
    categoria_id      UUID NOT NULL REFERENCES public.categorias(id) ON DELETE RESTRICT,
    titulo            TEXT NOT NULL,
    descripcion       TEXT NOT NULL DEFAULT '',
    precio            NUMERIC(10, 2) NOT NULL CHECK (precio > 0),
    stock             INT NOT NULL DEFAULT 1 CHECK (stock >= 0),
    zona_piura        TEXT NOT NULL DEFAULT 'Campus UNP',
    imagen_url        TEXT,
    estado            TEXT NOT NULL DEFAULT 'ACTIVO'
                          CHECK (estado IN ('ACTIVO', 'PAUSADO', 'OCULTO_ADMIN')),
    destacado         BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en         TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_productos_emprendimiento ON public.productos(emprendimiento_id);
CREATE INDEX IF NOT EXISTS idx_productos_categoria ON public.productos(categoria_id);
CREATE INDEX IF NOT EXISTS idx_productos_zona ON public.productos(zona_piura);

-- 5. ORDENES Y DETALLE
CREATE TABLE IF NOT EXISTS public.ordenes (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo_orden       TEXT NOT NULL UNIQUE,
    comprador_id       UUID NOT NULL REFERENCES public.usuarios(id) ON DELETE RESTRICT,
    emprendimiento_id  UUID NOT NULL REFERENCES public.emprendimientos(id) ON DELETE RESTRICT,
    tipo_entrega       TEXT NOT NULL CHECK (tipo_entrega IN ('PUNTO_ENCUENTRO', 'DELIVERY')),
    direccion_o_punto  TEXT NOT NULL,
    metodo_pago        TEXT NOT NULL CHECK (metodo_pago IN ('CONTRA_ENTREGA', 'YAPE_PLIN')),
    comprobante_url    TEXT,
    codigo_operacion   TEXT DEFAULT '',
    total              NUMERIC(10, 2) NOT NULL CHECK (total >= 0),
    estado             TEXT NOT NULL DEFAULT 'PENDIENTE'
                           CHECK (estado IN ('PENDIENTE', 'CONFIRMADO', 'EN_CAMINO', 'ENTREGADO', 'CANCELADO')),
    notas_comprador    TEXT NOT NULL DEFAULT '',
    creado_en          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.orden_items (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    orden_id         UUID NOT NULL REFERENCES public.ordenes(id) ON DELETE CASCADE,
    producto_id      UUID NOT NULL REFERENCES public.productos(id) ON DELETE RESTRICT,
    titulo_snapshot  TEXT NOT NULL,
    precio_unitario  NUMERIC(10, 2) NOT NULL CHECK (precio_unitario > 0),
    cantidad         INT NOT NULL CHECK (cantidad > 0),
    subtotal         NUMERIC(10, 2) NOT NULL CHECK (subtotal >= 0)
);

CREATE INDEX IF NOT EXISTS idx_ordenes_comprador ON public.ordenes(comprador_id, creado_en DESC);
CREATE INDEX IF NOT EXISTS idx_ordenes_emprendimiento ON public.ordenes(emprendimiento_id, creado_en DESC);

-- 6. CHAT REALTIME
CREATE TABLE IF NOT EXISTS public.conversaciones (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    comprador_id       UUID NOT NULL REFERENCES public.usuarios(id) ON DELETE CASCADE,
    emprendimiento_id  UUID NOT NULL REFERENCES public.emprendimientos(id) ON DELETE CASCADE,
    producto_id        UUID REFERENCES public.productos(id) ON DELETE SET NULL,
    orden_id           UUID REFERENCES public.ordenes(id) ON DELETE SET NULL,
    ultimo_mensaje     TEXT NOT NULL DEFAULT '',
    actualizado_en     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    bloqueado_por      UUID REFERENCES public.usuarios(id) ON DELETE SET NULL,
    bloqueado_en       TIMESTAMPTZ,
    UNIQUE (comprador_id, emprendimiento_id)
);

CREATE TABLE IF NOT EXISTS public.mensajes (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversacion_id  UUID NOT NULL REFERENCES public.conversaciones(id) ON DELETE CASCADE,
    remitente_id     UUID NOT NULL REFERENCES public.usuarios(id) ON DELETE CASCADE,
    contenido        TEXT NOT NULL,
    leido            BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_mensajes_conv_fecha ON public.mensajes(conversacion_id, creado_en ASC);

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

CREATE OR REPLACE FUNCTION public.actualizar_cabecera_conversacion()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE public.conversaciones
    SET ultimo_mensaje = NEW.contenido,
        actualizado_en = NEW.creado_en
    WHERE id = NEW.conversacion_id;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_actualizar_conversacion ON public.mensajes;
CREATE TRIGGER trg_actualizar_conversacion
AFTER INSERT ON public.mensajes
FOR EACH ROW
EXECUTE FUNCTION public.actualizar_cabecera_conversacion();

-- 7. RPC TRANSACCIONAL DE ORDENES Y STOCK
CREATE OR REPLACE FUNCTION public.crear_orden_con_stock(
    p_comprador_id      UUID,
    p_emprendimiento_id UUID,
    p_tipo_entrega      TEXT,
    p_direccion_punto   TEXT,
    p_metodo_pago       TEXT,
    p_comprobante_url   TEXT,
    p_codigo_operacion  TEXT,
    p_notas             TEXT,
    p_items             JSONB
)
RETURNS UUID
LANGUAGE plpgsql
AS $$
DECLARE
    v_orden_id      UUID;
    v_codigo        TEXT;
    v_total         NUMERIC(10, 2) := 0;
    v_item          JSONB;
    v_prod          RECORD;
    v_cant          INT;
    v_subtotal      NUMERIC(10, 2);
BEGIN
    IF p_items IS NULL OR jsonb_array_length(p_items) = 0 THEN
        RAISE EXCEPTION 'El pedido debe contener al menos un producto.';
    END IF;

    v_codigo := 'UNP-' || UPPER(SUBSTR(REPLACE(gen_random_uuid()::TEXT, '-', ''), 1, 6));

    INSERT INTO public.ordenes (
        codigo_orden, comprador_id, emprendimiento_id,
        tipo_entrega, direccion_o_punto, metodo_pago,
        comprobante_url, codigo_operacion, total, notas_comprador
    )
    VALUES (
        v_codigo, p_comprador_id, p_emprendimiento_id,
        p_tipo_entrega, p_direccion_punto, p_metodo_pago,
        p_comprobante_url, COALESCE(p_codigo_operacion, ''), 0, COALESCE(p_notas, '')
    )
    RETURNING id INTO v_orden_id;

    FOR v_item IN SELECT * FROM jsonb_array_elements(p_items)
    LOOP
        v_cant := (v_item->>'cantidad')::INT;
        IF v_cant <= 0 THEN
            RAISE EXCEPTION 'Cantidad invalida para producto.';
        END IF;

        SELECT id, titulo, precio, stock, estado, emprendimiento_id
        INTO v_prod
        FROM public.productos
        WHERE id = (v_item->>'producto_id')::UUID
        FOR UPDATE;

        IF NOT FOUND THEN RAISE EXCEPTION 'Producto no encontrado.'; END IF;
        IF v_prod.emprendimiento_id <> p_emprendimiento_id THEN
            RAISE EXCEPTION 'El producto % no pertenece al emprendimiento.', v_prod.titulo;
        END IF;
        IF v_prod.estado <> 'ACTIVO' THEN
            RAISE EXCEPTION 'El producto % ya no esta disponible.', v_prod.titulo;
        END IF;
        IF v_prod.stock < v_cant THEN
            RAISE EXCEPTION 'Stock insuficiente para % (disponible: %).', v_prod.titulo, v_prod.stock;
        END IF;

        UPDATE public.productos SET stock = stock - v_cant WHERE id = v_prod.id;

        v_subtotal := v_prod.precio * v_cant;
        v_total := v_total + v_subtotal;

        INSERT INTO public.orden_items (
            orden_id, producto_id, titulo_snapshot, precio_unitario, cantidad, subtotal
        ) VALUES (
            v_orden_id, v_prod.id, v_prod.titulo, v_prod.precio, v_cant, v_subtotal
        );
    END LOOP;

    UPDATE public.ordenes SET total = v_total, actualizado_en = NOW() WHERE id = v_orden_id;
    RETURN v_orden_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.actualizar_estado_orden(
    p_orden_id     UUID,
    p_nuevo_estado TEXT
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_estado_actual TEXT;
    v_it            RECORD;
BEGIN
    SELECT estado INTO v_estado_actual FROM public.ordenes WHERE id = p_orden_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Orden no encontrada.'; END IF;
    IF v_estado_actual = p_nuevo_estado THEN RETURN; END IF;
    IF v_estado_actual IN ('ENTREGADO', 'CANCELADO') THEN
        RAISE EXCEPTION 'No se puede modificar una orden en estado %.', v_estado_actual;
    END IF;

    IF p_nuevo_estado = 'CANCELADO' THEN
        FOR v_it IN SELECT producto_id, cantidad FROM public.orden_items WHERE orden_id = p_orden_id
        LOOP
            UPDATE public.productos SET stock = stock + v_it.cantidad WHERE id = v_it.producto_id;
        END LOOP;
    END IF;

    UPDATE public.ordenes SET estado = p_nuevo_estado, actualizado_en = NOW() WHERE id = p_orden_id;
END;
$$;

-- 8. REALTIME
DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.mensajes; EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.conversaciones; EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.ordenes; EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- 9. RLS
ALTER TABLE public.usuarios         ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.emprendimientos  ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.categorias       ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.productos        ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ordenes          ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.orden_items      ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversaciones   ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.mensajes         ENABLE ROW LEVEL SECURITY;

DO $$
DECLARE t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY[
        'usuarios', 'emprendimientos', 'categorias', 'productos',
        'ordenes', 'orden_items', 'conversaciones', 'mensajes'
    ] LOOP
        EXECUTE format('DROP POLICY IF EXISTS "app_access_%s" ON public.%I;', t, t);
        EXECUTE format('CREATE POLICY "app_access_%s" ON public.%I FOR ALL USING (true) WITH CHECK (true);', t, t);
    END LOOP;
END $$;

-- 10. STORAGE BUCKETS
INSERT INTO storage.buckets (id, name, public)
VALUES
    ('marketplace-productos', 'marketplace-productos', TRUE),
    ('marketplace-comprobantes', 'marketplace-comprobantes', TRUE),
    ('marketplace-rostros', 'marketplace-rostros', TRUE)
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
