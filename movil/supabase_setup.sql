-- ============================================================
-- Script SQL: App de Registro de Estudiantes (Proyecto académico)
-- Ejecutar en el SQL Editor de Supabase
-- ============================================================

-- Extensión para generar UUIDs (normalmente ya viene habilitada en Supabase)
create extension if not exists "pgcrypto";

-- ------------------------------------------------------------
-- 1. Tabla principal: students
-- ------------------------------------------------------------
create table if not exists students (
    id                  uuid primary key default gen_random_uuid(),
    name                text not null,
    age                 int not null check (age > 0),
    career              text not null,
    email               text,
    photo_url           text,
    cv_url              text,
    background_text     text,  -- texto de aptitudes/experiencia: extraído del CV o escrito manualmente
    review              text,
    created_at          timestamptz not null default now()
);

-- ------------------------------------------------------------
-- 2. Catálogo de palabras clave: skills
-- ------------------------------------------------------------
create table if not exists skills (
    id          bigint generated always as identity primary key,
    name        text not null unique,
    category    text not null check (category in ('tech', 'soft', 'experience'))
);

-- ------------------------------------------------------------
-- 3. Tabla puente: student_skills (relación N:N)
-- ------------------------------------------------------------
create table if not exists student_skills (
    student_id  uuid not null references students(id) on delete cascade,
    skill_id    bigint not null references skills(id) on delete cascade,
    primary key (student_id, skill_id)
);

-- ------------------------------------------------------------
-- 4. Precarga del catálogo de skills (mismas palabras clave del análisis en Kotlin)
-- ------------------------------------------------------------
insert into skills (name, category) values
    ('Kotlin', 'tech'),
    ('Java', 'tech'),
    ('Python', 'tech'),
    ('JavaScript', 'tech'),
    ('React', 'tech'),
    ('SQL', 'tech'),
    ('Android', 'tech'),
    ('Figma', 'tech'),
    ('Liderazgo', 'soft'),
    ('Trabajo en equipo', 'soft'),
    ('Comunicación', 'soft'),
    ('Proactividad', 'soft'),
    ('Practicante', 'experience'),
    ('Pasantía', 'experience'),
    ('Freelance', 'experience'),
    ('Desarrollador', 'experience'),
    ('Becario', 'experience'),
    ('Voluntariado', 'experience')
on conflict (name) do nothing;

-- ------------------------------------------------------------
-- 5. Índices útiles
-- ------------------------------------------------------------
create index if not exists idx_student_skills_student_id on student_skills(student_id);
create index if not exists idx_student_skills_skill_id on student_skills(skill_id);
create index if not exists idx_students_name on students(name);

-- ------------------------------------------------------------
-- 6. Row Level Security (RLS)
-- Nota: no hay autenticación en la app, así que se habilita RLS
-- con una política abierta (permite todo). En un entorno real de
-- producción, esto se restringiría por usuario/rol autenticado.
-- ------------------------------------------------------------
alter table students enable row level security;
alter table skills enable row level security;
alter table student_skills enable row level security;

create policy "Allow all on students" on students
    for all using (true) with check (true);

create policy "Allow all on skills" on skills
    for all using (true) with check (true);

create policy "Allow all on student_skills" on student_skills
    for all using (true) with check (true);

-- ------------------------------------------------------------
-- 7. Buckets de Storage (fotos y CVs)
-- Nota: esto crea los buckets vía SQL. Si prefieres, también puedes
-- crearlos desde la sección "Storage" del dashboard de Supabase.
-- ------------------------------------------------------------
insert into storage.buckets (id, name, public)
values ('student-photos', 'student-photos', true)
on conflict (id) do nothing;

insert into storage.buckets (id, name, public)
values ('student-cvs', 'student-cvs', true)
on conflict (id) do nothing;

-- Políticas de Storage: acceso abierto de lectura/escritura (sin login)
create policy "Public access student-photos"
    on storage.objects for all
    using (bucket_id = 'student-photos')
    with check (bucket_id = 'student-photos');

create policy "Public access student-cvs"
    on storage.objects for all
    using (bucket_id = 'student-cvs')
    with check (bucket_id = 'student-cvs');

-- ============================================================
-- 8. Tabla: contacts (Agenda de contactos estilo WhatsApp)
-- ============================================================
create table if not exists contacts (
    id              uuid primary key default gen_random_uuid(),
    first_name      text not null check (char_length(trim(first_name)) >= 2),
    last_name       text not null check (char_length(trim(last_name)) >= 2),
    username        text unique check (username is null or char_length(trim(username)) >= 3),
    country_code    text not null default '+51',
    phone           text not null check (char_length(trim(phone)) = 9),
    email           text,
    sync_to_phone   boolean not null default false,
    created_at      timestamptz not null default now(),

    -- Restricción: No duplicar el número dentro del mismo código de país
    constraint unique_contact_phone unique (country_code, phone),

    -- Restricción: No duplicar la combinación exacta de nombre y apellido
    constraint unique_contact_fullname unique (first_name, last_name)
);

-- Índices para optimizar búsquedas por nombre, apellido, usuario y teléfono
create index if not exists idx_contacts_first_name on contacts(first_name);
create index if not exists idx_contacts_last_name on contacts(last_name);
create index if not exists idx_contacts_username on contacts(username);
create index if not exists idx_contacts_phone on contacts(phone);

-- Seguridad a nivel de fila (Row Level Security - RLS)
alter table contacts enable row level security;

create policy "Allow all operations on contacts" on contacts
    for all
    using (true)
    with check (true);

-- Inserción de registros iniciales de prueba
insert into contacts (first_name, last_name, username, country_code, phone, email, sync_to_phone) values
    ('Juan', 'Pérez', 'juanperez', '+51', '987654321', 'juan.perez@unp.edu.pe', false),
    ('María', 'López', null, '+51', '912345678', 'maria.lopez@gmail.com', false),
    ('Carlos', 'Mendoza', 'carlos_m', '+51', '955443322', 'carlos.mendoza@outlook.com', false)
on conflict do nothing;

