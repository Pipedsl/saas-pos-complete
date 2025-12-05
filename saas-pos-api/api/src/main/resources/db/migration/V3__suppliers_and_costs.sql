-- 1. Crear tabla de proveedores (si no existe)
CREATE TABLE IF NOT EXISTS suppliers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    rut VARCHAR(20),
    contact_name VARCHAR(100),
    contact_phone VARCHAR(50),
    contact_email VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- 2. Agregar columna supplier_id a products (con chequeo preventivo)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='products' AND column_name='supplier_id') THEN
        ALTER TABLE products ADD COLUMN supplier_id UUID REFERENCES suppliers(id);
    END IF;
END $$;