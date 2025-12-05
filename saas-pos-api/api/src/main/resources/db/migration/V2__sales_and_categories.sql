-- V2__sales_and_categories.sql

-- 1) CATEGORÍAS
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(tenant_id, name) -- No repetir nombres de categoría en el mismo local
);

CREATE INDEX IF NOT EXISTS idx_categories_tenant ON categories(tenant_id);

-- 2) PROVEEDORES
CREATE TABLE IF NOT EXISTS suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    name VARCHAR(150) NOT NULL,
    contact_name VARCHAR(150),
    contact_email VARCHAR(150),
    contact_phone VARCHAR(50),
    tax_id VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_suppliers_tenant ON suppliers(tenant_id);

-- 3) Relación producto-proveedor
CREATE TABLE IF NOT EXISTS product_suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    product_id UUID NOT NULL REFERENCES products(id),
    supplier_id UUID NOT NULL REFERENCES suppliers(id),
    supplier_sku VARCHAR(100),
    lead_time_days INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(tenant_id, product_id, supplier_id)
);

-- 4) Ampliar tabla products con CATEGORÍA y COSTOS
ALTER TABLE products
  ADD COLUMN IF NOT EXISTS category_id UUID REFERENCES categories(id), -- Nueva relación
  ADD COLUMN IF NOT EXISTS cost_price NUMERIC(12,0) DEFAULT 0,
  ADD COLUMN IF NOT EXISTS margin_percent NUMERIC(5,2) DEFAULT NULL,
  ADD COLUMN IF NOT EXISTS discountable BOOLEAN DEFAULT TRUE;

CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);

-- 5) Historial de precios
CREATE TABLE IF NOT EXISTS product_price_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL REFERENCES products(id),
    price_neto NUMERIC(12,0) NOT NULL,
    cost_price NUMERIC(12,0) NOT NULL,
    valid_from TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    valid_until TIMESTAMP WITH TIME ZONE,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 6) VENTAS (Cabecera)
CREATE TABLE IF NOT EXISTS sales (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    sale_number VARCHAR(100),
    cashier_id UUID,
    agent_id UUID,
    total_amount NUMERIC(14,0) NOT NULL,
    subtotal_amount NUMERIC(14,0) NOT NULL,
    total_tax NUMERIC(14,0) NOT NULL,
    total_discount NUMERIC(14,0) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    status VARCHAR(20) DEFAULT 'COMPLETED'
);

CREATE INDEX IF NOT EXISTS idx_sales_tenant ON sales(tenant_id);

-- 7) DETALLE DE VENTA
CREATE TABLE IF NOT EXISTS sale_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_id UUID NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL DEFAULT 1,
    unit_price NUMERIC(12,0) NOT NULL,
    unit_tax NUMERIC(12,0) DEFAULT 0,
    discount_amount NUMERIC(12,0) DEFAULT 0,
    cost_price_at_sale NUMERIC(12,0) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 8) PAGOS
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_id UUID NOT NULL REFERENCES sales(id),
    method VARCHAR(50) NOT NULL,
    amount NUMERIC(14,0) NOT NULL,
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);