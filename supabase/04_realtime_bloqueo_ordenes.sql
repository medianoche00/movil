-- =============================================================================
-- EMPRENDEPIURA - SCRIPT 04: REALTIME TOTAL (ORDENES/CATALOGO) Y BLOQUEO DE COMPRAS
-- =============================================================================
-- Ejecutar en Supabase -> SQL Editor sobre la base actual.
-- Garantiza:
--   1. REPLICA IDENTITY FULL en ordenes, productos y emprendimientos para eventos
--      Realtime UPDATE/DELETE completos entre dispositivos en cualquier red.
--   2. Publicacion supabase_realtime habilitada para mensajes, conversaciones,
--      ordenes, productos y emprendimientos.
--   3. Proteccion en RPC crear_orden_con_stock para impedir compras si existe
--      bloqueo activo entre el comprador y el emprendimiento.
-- =============================================================================

ALTER TABLE public.ordenes         REPLICA IDENTITY FULL;
ALTER TABLE public.productos       REPLICA IDENTITY FULL;
ALTER TABLE public.emprendimientos REPLICA IDENTITY FULL;

DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.mensajes; EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.conversaciones; EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.ordenes; EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.productos; EXCEPTION WHEN duplicate_object THEN NULL; END $$;
DO $$ BEGIN ALTER PUBLICATION supabase_realtime ADD TABLE public.emprendimientos; EXCEPTION WHEN duplicate_object THEN NULL; END $$;

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

    -- Impedir pedidos si existe un bloqueo activo en cualquier direccion
    IF EXISTS (
        SELECT 1
        FROM public.conversaciones
        WHERE comprador_id = p_comprador_id
          AND emprendimiento_id = p_emprendimiento_id
          AND bloqueado_por IS NOT NULL
    ) THEN
        RAISE EXCEPTION 'Este emprendimiento no esta disponible para realizar pedidos en este momento.';
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
