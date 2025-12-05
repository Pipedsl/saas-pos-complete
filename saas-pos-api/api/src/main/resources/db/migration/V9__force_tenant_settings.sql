-- V9: Forzar la creación de la columna settings si V7 falló silenciosamente

DO $$
BEGIN
    -- Verificar si la columna 'settings' existe en la tabla 'tenants'
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='tenants' AND column_name='settings') THEN
        -- Si no existe, crearla
        ALTER TABLE tenants ADD COLUMN settings JSONB DEFAULT '{}'::jsonb;
    END IF;
END $$;