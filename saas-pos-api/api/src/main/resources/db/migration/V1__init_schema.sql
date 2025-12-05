-- Habilitar extensión para UUIDs
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. PLANES DEL SAAS (Hard limits)
CREATE TABLE plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL UNIQUE, -- FREE, PRO, ENTERPRISE
    price_clp NUMERIC(10,0) NOT NULL,
    max_users INTEGER NOT NULL,
    max_products INTEGER NOT NULL,
    enable_sii BOOLEAN DEFAULT FALSE,
    enable_transbank BOOLEAN DEFAULT FALSE,
    enable_multi_local BOOLEAN DEFAULT FALSE,
    demos_per_agent_limit INTEGER DEFAULT 5, -- Limite para vendedores
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. TENANTS (Las empresas clientes)
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_name VARCHAR(150) NOT NULL,
    rut VARCHAR(20) NOT NULL, -- Identificador fiscal Chile
    plan_id UUID REFERENCES plans(id),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 3. USUARIOS (Globales, incluyen Super Admin, Vendedores y usuarios de Tenants)
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id), -- Nullable (Super admins no tienen tenant)
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL, -- SUPER_ADMIN, VENDOR, TENANT_ADMIN, TENANT_USER
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 4. PRODUCTOS (Con Metadata JSONB para flexibilidad)
CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    sku VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price_neto NUMERIC(12,0) NOT NULL, -- Precio sin IVA
    tax_percent NUMERIC(5,2) DEFAULT 19.00, -- IVA Chile
    stock_current INTEGER DEFAULT 0,
    stock_min INTEGER DEFAULT 5,
    
    -- Metadata dinámica: { "color": "rojo", "talla": "L", "marca": "Sony" }
    -- Índices GIN se crearán para búsquedas rápidas dentro del JSON
    attributes JSONB DEFAULT '{}'::jsonb,
    
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    
    UNIQUE(tenant_id, sku) -- SKU único por empresa, no global
);

-- 5. DEMO LINKS (Automation)
CREATE TABLE demo_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id UUID NOT NULL REFERENCES users(id), -- El vendedor que lo creó
    token VARCHAR(64) NOT NULL UNIQUE,
    lead_email VARCHAR(150), -- Opcional, a quién se le envió
    is_used BOOLEAN DEFAULT FALSE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 6. DEMO REQUESTS (Leads entrantes)
CREATE TABLE demo_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contact_name VARCHAR(150),
    contact_email VARCHAR(150),
    contact_phone VARCHAR(50),
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, CONTACTED, CONVERTED
    assigned_agent_id UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Índices básicos para rendimiento
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_products_tenant ON products(tenant_id);
CREATE INDEX idx_products_attributes ON products USING GIN (attributes); -- CLAVE para JSONB
CREATE INDEX idx_demo_links_token ON demo_links(token);