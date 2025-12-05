-- Tabla para los links de Demo
CREATE TABLE IF NOT EXISTS demo_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(100) NOT NULL UNIQUE, --codigo secreto de la URL
    agent_id UUID NOT NULL REFERENCES users(id), --Quien creo el link (Vendedor)

    is_used BOOLEAN DEFAULT FALSE, -- solo se puede usar una vez
    used_at TIMESTAMP WITH TIME ZONE, --Cuando se uso
    created_tenant_id UUID, --Que empresa nacio desde este link (informacion para el vendedor)

    expires_at TIMESTAMP WITH TIME ZONE NOT NULL, -- El link caduca si no se usa
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- CONFIGURACION DEL VENDEDOR (LIMITES)
--Agregamos columnas a 'users' para controlar a los vendedores
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='link_limit_monthly') THEN
        ALTER TABLE users ADD COLUMN link_limit_monthly INTEGER DEFAULT 10;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='links_created_this_month') THEN
        ALTER TABLE users ADD COLUMN links_created_this_month INTEGER DEFAULT 0;
    END IF;
END $$;

--CONFIGURACION DEL TENANT (DEMO TIMER)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='tenants' AND column_name='is_demo_account') THEN
        ALTER TABLE tenants ADD COLUMN is_demo_account BOOLEAN DEFAULT FALSE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='tenants' AND column_name='demo_expires_at') THEN
        ALTER TABLE tenants ADD COLUMN demo_expires_at TIMESTAMP WITH TIME ZONE;
    END IF;
END $$;