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
