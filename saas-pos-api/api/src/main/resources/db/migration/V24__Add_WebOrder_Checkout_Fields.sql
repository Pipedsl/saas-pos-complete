-- V24__Add_WebOrder_Checkout_Fields.sql

-- 1. Agregar RUT del cliente
ALTER TABLE web_orders ADD COLUMN IF NOT EXISTS customer_rut VARCHAR(20);

-- 2. Agregar campos estructurados de dirección (para integraciones futuras)
ALTER TABLE web_orders ADD COLUMN IF NOT EXISTS shipping_region VARCHAR(100);
ALTER TABLE web_orders ADD COLUMN IF NOT EXISTS shipping_commune VARCHAR(100);
ALTER TABLE web_orders ADD COLUMN IF NOT EXISTS shipping_street VARCHAR(255);

-- 3. (Opcional) Índices para búsquedas rápidas si planeas filtrar por RUT
CREATE INDEX IF NOT EXISTS idx_web_orders_rut ON web_orders(customer_rut);