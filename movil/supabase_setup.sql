-- ============================================================
-- SCRIPT: MODELO DE BASE DE DATOS NORMALIZADO (3 TABLAS) - UNP
-- Ejecutar en el SQL Editor de Supabase
-- ============================================================

-- 1. Limpieza de tablas previas si existen
DROP TABLE IF EXISTS personas CASCADE;
DROP TABLE IF EXISTS profesiones CASCADE;
DROP TABLE IF EXISTS facultades CASCADE;

-- 2. Tabla 1: facultades
CREATE TABLE facultades (
    id_facultad SERIAL PRIMARY KEY,
    nombre      VARCHAR(120) NOT NULL UNIQUE
);

-- 3. Tabla 2: profesiones / carreras
CREATE TABLE profesiones (
    codigo_profesion VARCHAR(15) PRIMARY KEY,
    nombre           VARCHAR(120) NOT NULL UNIQUE,
    id_facultad      INT NOT NULL,

    -- Clave foránea hacia facultades
    CONSTRAINT fk_profesion_facultad
        FOREIGN KEY (id_facultad)
        REFERENCES facultades(id_facultad)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

-- 4. Tabla 3: personas
CREATE TABLE personas (
    dni                 CHAR(8) PRIMARY KEY,
    nombres             VARCHAR(100) NOT NULL,
    apellidos           VARCHAR(100) NOT NULL,
    direccion           VARCHAR(200) NOT NULL,
    codigo_profesion    VARCHAR(15) NOT NULL,
    celular             CHAR(9) NOT NULL,
    correo_electronico  VARCHAR(150) NOT NULL,
    foto                TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),

    -- Restricciones de formato
    CONSTRAINT chk_dni_format CHECK (dni ~ '^[0-9]{8}$'),
    CONSTRAINT chk_celular_format CHECK (celular ~ '^[0-9]{9}$'),

    -- Clave foránea hacia profesiones
    CONSTRAINT fk_persona_profesion 
        FOREIGN KEY (codigo_profesion) 
        REFERENCES profesiones(codigo_profesion) 
        ON UPDATE CASCADE 
        ON DELETE RESTRICT
);

-- 5. Índices de rendimiento para búsquedas y relaciones
CREATE INDEX idx_profesiones_facultad ON profesiones(id_facultad);
CREATE INDEX idx_personas_nombres_apellidos ON personas(nombres, apellidos);
CREATE INDEX idx_personas_profesion ON personas(codigo_profesion);
CREATE INDEX idx_personas_celular ON personas(celular);

-- 6. Seguridad a nivel de fila (Row Level Security - RLS)
ALTER TABLE facultades ENABLE ROW LEVEL SECURITY;
ALTER TABLE profesiones ENABLE ROW LEVEL SECURITY;
ALTER TABLE personas ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Allow all operations on facultades" ON facultades
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations on profesiones" ON profesiones
    FOR ALL USING (true) WITH CHECK (true);

CREATE POLICY "Allow all operations on personas" ON personas
    FOR ALL USING (true) WITH CHECK (true);

-- 7. Bucket de Storage en Supabase para almacenamiento de fotos
INSERT INTO storage.buckets (id, name, public)
VALUES ('personas-photos', 'personas-photos', true)
ON CONFLICT (id) DO NOTHING;

DROP POLICY IF EXISTS "Public access personas-photos" ON storage.objects;

CREATE POLICY "Public access personas-photos"
    ON storage.objects FOR ALL
    USING (bucket_id = 'personas-photos')
    WITH CHECK (bucket_id = 'personas-photos');

-- 8. Carga del catálogo de Facultades (14 Facultades de la UNP)
INSERT INTO facultades (id_facultad, nombre) VALUES
    (1, 'Facultad de Agronomía'),
    (2, 'Facultad de Arquitectura y Urbanismo'),
    (3, 'Facultad de Ciencias Administrativas'),
    (4, 'Facultad de Ciencias Contables y Financieras'),
    (5, 'Facultad de Ciencias'),
    (6, 'Facultad de Ciencias de la Salud'),
    (7, 'Facultad de Ciencias Sociales y Educación'),
    (8, 'Facultad de Derecho y Ciencias Políticas'),
    (9, 'Facultad de Economía'),
    (10, 'Facultad de Ingeniería Civil'),
    (11, 'Facultad de Ingeniería Industrial'),
    (12, 'Facultad de Ingeniería de Minas'),
    (13, 'Facultad de Ingeniería Pesquera'),
    (14, 'Facultad de Zootecnia');

-- Sincronizar el valor de la secuencia autoincrementable
SELECT setval(pg_get_serial_sequence('facultades', 'id_facultad'), 14);

-- 9. Carga de las Escuelas Profesionales vinculadas a su Facultad
INSERT INTO profesiones (codigo_profesion, nombre, id_facultad) VALUES
    -- Facultad de Agronomía (1)
    ('AGRO', 'Agronomía', 1),
    ('ING-AGRI', 'Ingeniería Agrícola', 1),

    -- Facultad de Arquitectura y Urbanismo (2)
    ('ARQ', 'Arquitectura y Urbanismo', 2),

    -- Facultad de Ciencias Administrativas (3)
    ('ADM', 'Ciencias Administrativas', 3),

    -- Facultad de Ciencias Contables y Financieras (4)
    ('CONT', 'Ciencias Contables y Financieras', 4),

    -- Facultad de Ciencias (5)
    ('MAT', 'Matemática', 5),
    ('FIS', 'Física', 5),
    ('BIO', 'Ciencias Biológicas', 5),
    ('EST', 'Estadística', 5),
    ('ING-TEL', 'Ingeniería Electrónica y Telecomunicaciones', 5),

    -- Facultad de Ciencias de la Salud (6)
    ('MED', 'Medicina Humana', 6),
    ('ENF', 'Enfermería', 6),
    ('ESTO', 'Estomatología', 6),
    ('OBS', 'Obstetricia', 6),
    ('PSI', 'Psicología', 6),

    -- Facultad de Ciencias Sociales y Educación (7)
    ('EDU-HG', 'Historia y Geografía', 7),
    ('EDU-LL', 'Lengua y Literatura', 7),
    ('EDU-INI', 'Educación Inicial', 7),
    ('EDU-PRI', 'Educación Primaria', 7),
    ('COM', 'Ciencias de la Comunicación', 7),

    -- Facultad de Derecho y Ciencias Políticas (8)
    ('DER', 'Derecho y Ciencias Políticas', 8),

    -- Facultad de Economía (9)
    ('ECO', 'Economía', 9),

    -- Facultad de Ingeniería Civil (10)
    ('ING-CIV', 'Ingeniería Civil', 10),

    -- Facultad de Ingeniería Industrial (11)
    ('ING-IND', 'Ingeniería Industrial', 11),
    ('ING-INFO', 'Ingeniería Informática', 11),
    ('ING-AGROIND', 'Ingeniería Agroindustrial e Industrias Alimentarias', 11),
    ('ING-MECA', 'Ingeniería Mecatrónica', 11),

    -- Facultad de Ingeniería de Minas (12)
    ('ING-MIN', 'Ingeniería de Minas', 12),
    ('ING-GEO', 'Ingeniería Geológica', 12),
    ('ING-PET', 'Ingeniería de Petróleo', 12),
    ('ING-QUIM', 'Ingeniería Química', 12),
    ('ING-AMB', 'Ingeniería Ambiental y Seguridad Industrial', 12),

    -- Facultad de Ingeniería Pesquera (13)
    ('ING-PESQ', 'Ingeniería Pesquera', 13),

    -- Facultad de Zootecnia (14)
    ('ING-ZOO', 'Ingeniería Zootecnia', 14),
    ('VET', 'Medicina Veterinaria', 14);
