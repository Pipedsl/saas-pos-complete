-- V22__Add_Total_To_SaleItems.sql

-- 1. Agregar la columna 'total' a la tabla de items
ALTER TABLE sale_items ADD COLUMN IF NOT EXISTS total DECIMAL(19, 2);

-- 2. Calcular el total para los registros antiguos (Cantidad * Precio Unitario)
-- Esto es vital para que el historial no se rompa al ver ventas viejas
UPDATE sale_items
SET total = quantity * unit_price
WHERE total IS NULL;