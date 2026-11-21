-- ============================================================================
-- Script 02: Datos Iniciales - EmprendePiura (Piura + Universidad Nacional de Piura)
-- Contrasena de todas las cuentas demo: 123456
-- Hash: SHA-256('UNP_MARKET_2026:' || password)
-- ============================================================================

-- 1. CATEGORIAS ORIENTADAS A PIURA Y COMUNIDAD UNIVERSITARIA
INSERT INTO public.categorias (id, nombre, icono, orden, activa)
VALUES
    ('11111111-1111-1111-1111-111111111101', 'Alimentos y Postres', 'fastfood', 1, TRUE),
    ('11111111-1111-1111-1111-111111111102', 'Apuntes y Papelería', 'menu_book', 2, TRUE),
    ('11111111-1111-1111-1111-111111111103', 'Moda y Accesorios', 'checkroom', 3, TRUE),
    ('11111111-1111-1111-1111-111111111104', 'Tecnología', 'devices', 4, TRUE),
    ('11111111-1111-1111-1111-111111111105', 'Artesanía Piurana', 'palette', 5, TRUE),
    ('11111111-1111-1111-1111-111111111106', 'Servicios UNP', 'build', 6, TRUE)
ON CONFLICT (nombre) DO NOTHING;

-- 2. CUENTAS DEMO CON DNI (1 ADMIN, 2 EMPRENDEDORES APROBADOS, 1 COMPRADOR SOLICITANTE)
--    DNI 00000001 -> Administrador
--    DNI 00000002 -> Emprendedor 1 (Dulces & Snacks UNP)
--    DNI 00000003 -> Emprendedor 2 (Norte Artesanal Piura)
--    DNI 00000004 -> Comprador con tienda PENDIENTE para probar aprobacion del Admin
INSERT INTO public.usuarios (id, dni, nombre, correo, telefono, password_hash, rol, zona_piura)
VALUES
    (
        '22222222-2222-2222-2222-222222222201',
        '00000001',
        'Administrador EmprendePiura',
        'admin@unpmarket.pe',
        '969000001',
        encode(digest('UNP_MARKET_2026:123456', 'sha256'), 'hex'),
        'ADMIN',
        'Campus UNP'
    ),
    (
        '22222222-2222-2222-2222-222222222202',
        '00000002',
        'Valeria Mendoza (Dulces UNP)',
        'emprendedor@unpmarket.pe',
        '969111222',
        encode(digest('UNP_MARKET_2026:123456', 'sha256'), 'hex'),
        'EMPRENDEDOR',
        'Campus UNP'
    ),
    (
        '22222222-2222-2222-2222-222222222203',
        '00000003',
        'Carlos Chunga (Catacaos Craft)',
        'carlos.craft@unpmarket.pe',
        '969333444',
        encode(digest('UNP_MARKET_2026:123456', 'sha256'), 'hex'),
        'EMPRENDEDOR',
        'Piura Centro'
    ),
    (
        '22222222-2222-2222-2222-222222222204',
        '00000004',
        'Marco Garcia',
        'comprador@unpmarket.pe',
        '969555666',
        encode(digest('UNP_MARKET_2026:123456', 'sha256'), 'hex'),
        'COMPRADOR',
        'Castilla'
    )
ON CONFLICT (correo) DO UPDATE
SET dni = EXCLUDED.dni,
    rol = EXCLUDED.rol;

-- 3. EMPRENDIMIENTOS (2 APROBADOS + 1 PENDIENTE PARA PROBAR APROBACION DEL ADMIN)
INSERT INTO public.emprendimientos (
    id, usuario_id, nombre_tienda, descripcion, categoria_principal,
    zona_piura, punto_encuentro_habitual, telefono_contacto, numero_yape_plin, estado
)
VALUES
    (
        '33333333-3333-3333-3333-333333333301',
        '22222222-2222-2222-2222-222222222202',
        'Dulces & Snacks UNP',
        'Postres artesanales, chocotejas piuranas y chifles recién hechos para estudiantes de la UNP.',
        'Alimentos y Postres',
        'Campus UNP',
        'Explanada / Comedor UNP',
        '969111222',
        '969111222',
        'APROBADO'
    ),
    (
        '33333333-3333-3333-3333-333333333302',
        '22222222-2222-2222-2222-222222222203',
        'Norte Artesanal Piura',
        'Sombreros de Catacaos, polos algodón pima y recuerdos piuranos con entrega en Castilla y Centro.',
        'Artesanía Piurana',
        'Piura Centro',
        'Plaza de Armas de Piura',
        '969333444',
        '969333444',
        'APROBADO'
    ),
    (
        '33333333-3333-3333-3333-333333333303',
        '22222222-2222-2222-2222-222222222204',
        'Soporte Tech Castilla',
        'Mantenimiento preventivo, pasta térmica e instalación de software para laptops universitarias.',
        'Servicios UNP',
        'Castilla',
        'Puerta Principal UNP',
        '969555666',
        '969555666',
        'PENDIENTE'
    )
ON CONFLICT (usuario_id) DO NOTHING;

-- 4. CATALOGO INICIAL DE PRODUCTOS DE PIURA Y CAMPUS UNP
INSERT INTO public.productos (
    id, emprendimiento_id, categoria_id, titulo, descripcion, precio, stock, zona_piura, estado, destacado
)
VALUES
    (
        '44444444-4444-4444-4444-444444444401',
        '33333333-3333-3333-3333-333333333301',
        '11111111-1111-1111-1111-111111111101',
        'Caja x6 Chocotejas de Algarrobina y Pecana',
        'Elaboración casera piurana. Entrega entre clases en la Explanada o Facultad de Ingeniería.',
        15.00, 12, 'Campus UNP', 'ACTIVO', TRUE
    ),
    (
        '44444444-4444-4444-4444-444444444402',
        '33333333-3333-3333-3333-333333333301',
        '11111111-1111-1111-1111-111111111101',
        'Chifles Piuranos Crocantes + Cancha (250g)',
        'Plátano bellaco frito al día con sal de Sechura. Bolsa sellada ideal para media tarde.',
        8.50, 25, 'Campus UNP', 'ACTIVO', FALSE
    ),
    (
        '44444444-4444-4444-4444-444444444403',
        '33333333-3333-3333-3333-333333333301',
        '11111111-1111-1111-1111-111111111101',
        'Natilla Piurana Tradicional 500g',
        'Envase hermético de vidrio. Pedidos para recoger en Puerta Principal UNP o Open Plaza Castilla.',
        18.00, 8, 'Castilla', 'ACTIVO', TRUE
    ),
    (
        '44444444-4444-4444-4444-444444444404',
        '33333333-3333-3333-3333-333333333302',
        '11111111-1111-1111-1111-111111111105',
        'Sombrero de Paja Toquilla Catacaos',
        'Tejido fino artesanal ideal para el sol piurano y salidas de campo universitarias.',
        45.00, 6, 'Piura Centro', 'ACTIVO', TRUE
    ),
    (
        '44444444-4444-4444-4444-444444444405',
        '33333333-3333-3333-3333-333333333302',
        '11111111-1111-1111-1111-111111111103',
        'Polo Algodón Pima Piura (Unisex S-M-L)',
        'Algodón pima 100% fresco para clima cálido, con bordado discreto norteño.',
        36.00, 14, 'Piura Centro', 'ACTIVO', FALSE
    ),
    (
        '44444444-4444-4444-4444-444444444406',
        '33333333-3333-3333-3333-333333333302',
        '11111111-1111-1111-1111-111111111102',
        'Kit Apuntes + Cuaderno Anillado Ingeniería',
        'Pack universitario con cuaderno cuadriculado A4 pasta dura y resaltadores pastel.',
        22.00, 10, 'Campus UNP', 'ACTIVO', FALSE
    )
ON CONFLICT (id) DO NOTHING;
