-- V12: Reparación de emergencia para demo_links en producción

-- 1. Si la columna no existe, la creamos
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='demo_links' AND column_name='created_tenant_id') THEN
        ALTER TABLE demo_links ADD COLUMN created_tenant_id UUID;
    END IF;

    -- Verificación extra para agent_id (por si acaso)
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='demo_links' AND column_name='agent_id') THEN
        -- Si existe la columna vieja 'agent', la renombramos
        IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='demo_links' AND column_name='agent') THEN
            ALTER TABLE demo_links RENAME COLUMN agent TO agent_id;
        ELSE
             -- Si no, agregamos agent_id (esto podría fallar si hay datos sin FK, pero asumimos tabla vacía o nueva)
             ALTER TABLE demo_links ADD COLUMN agent_id UUID REFERENCES users(id);
        END IF;
    END IF;
END $$;