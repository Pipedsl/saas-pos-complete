-- Agregamos configuración dinámica al tenant (para datos bancarios, logo, etc)
ALTER TABLE tenants
ADD COLUMN setting JSONB DEFAULT '{}'::jsonb;