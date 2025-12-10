-- V15: Aumentar precisión de precios netos para evitar pérdida de centavos

-- Cambiamos de NUMERIC(12,0) a NUMERIC(15,4) (4 decimales)
-- Esto permite guardar 84.0336 en lugar de 84
ALTER TABLE products
ALTER COLUMN price_neto TYPE NUMERIC(15,4);

ALTER TABLE products
ALTER COLUMN cost_price TYPE NUMERIC(15,4);

-- También en el historial de ventas para reportes precisos
ALTER TABLE sale_items
ALTER COLUMN net_price_at_sale TYPE NUMERIC(15,4);

ALTER TABLE sale_items
ALTER COLUMN cost_price_at_sale TYPE NUMERIC(15,4);