-- V21__Update_Inventory_Logs_And_Sales.sql

-- 1. ACTUALIZAR INVENTORY_LOGS
-- Usamos IF NOT EXISTS para evitar errores si ya se corrió parcialmente
ALTER TABLE inventory_logs ADD COLUMN IF NOT EXISTS sale_id UUID;
ALTER TABLE inventory_logs ADD COLUMN IF NOT EXISTS web_order_id UUID;

-- Índices (Postgres soporta IF NOT EXISTS en índices)
CREATE INDEX IF NOT EXISTS idx_logs_product ON inventory_logs(product_id);

-- 2. ACTUALIZAR TABLA SALES
ALTER TABLE sales ADD COLUMN IF NOT EXISTS was_edited BOOLEAN DEFAULT FALSE;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS edited_by_user_id UUID;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS edit_reason TEXT;

-- Esta es la línea que fallaba, ahora está protegida
ALTER TABLE sales ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'COMPLETED';