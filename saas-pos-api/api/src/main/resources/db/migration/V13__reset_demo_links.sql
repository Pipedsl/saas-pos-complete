-- V13: Reseteo total de la tabla demo_links para asegurar consistencia

DROP TABLE IF EXISTS demo_links;

CREATE TABLE demo_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(100) NOT NULL UNIQUE,
    agent_id UUID NOT NULL REFERENCES users(id),

    is_used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMP WITH TIME ZONE, -- Esta es la que faltaba
    created_tenant_id UUID,

    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);