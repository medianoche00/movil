-- Ejecutar en Supabase > SQL Editor (coordina con tus compañeros: si ya existen, se ignoran o dan error inofensivo)

-- 1) Permisos abiertos para el proyecto académico (sin login)
alter table public.students enable row level security;
alter table public.skills enable row level security;
alter table public.student_skills enable row level security;

create policy "acceso publico students" on public.students for all using (true) with check (true);
create policy "acceso publico skills" on public.skills for all using (true) with check (true);
create policy "acceso publico student_skills" on public.student_skills for all using (true) with check (true);

-- 2) Buckets de Storage (públicos) para foto y CV
insert into storage.buckets (id, name, public)
values ('photos', 'photos', true), ('cvs', 'cvs', true)
on conflict (id) do nothing;

create policy "storage lectura publica" on storage.objects for select using (bucket_id in ('photos','cvs'));
create policy "storage subida publica" on storage.objects for insert with check (bucket_id in ('photos','cvs'));
create policy "storage update publico" on storage.objects for update using (bucket_id in ('photos','cvs'));
