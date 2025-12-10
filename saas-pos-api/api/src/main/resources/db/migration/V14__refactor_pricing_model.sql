-- V14: Refactorización del Modelo de Precios y Limpieza de Historial

-- 1. CAMBIOS EN PRODUCTS
-- Agregamos las columnas para la nueva lógica de precios
ALTER TABLE products
ADD COLUMN IF NOT EXISTS price_final NUMERIC(12,0) DEFAULT 0, -- Precio "Visual" que ingresó el usuario
ADD COLUMN IF NOT EXISTS is_tax_included BOOLEAN DEFAULT TRUE; -- Checkbox: ¿El precio visual incluye IVA?

-- Aseguramos que cost_price exista y sea numérico (ya lo tenías, pero reforzamos)
-- Si price_neto no existe, lo creamos (es el valor real sin IVA)
-- (Nota: price_neto ya existía en tu modelo, lo mantenemos como la fuente de verdad para cálculos)


-- 2. CAMBIOS EN SALE_ITEMS (Optimización para Analytics)
-- Agregamos columnas "Snapshot" (Foto del momento de la venta)
ALTER TABLE sale_items
ADD COLUMN IF NOT EXISTS cost_price_at_sale NUMERIC(12,0) DEFAULT 0, -- Costo unitario al momento de vender
ADD COLUMN IF NOT EXISTS net_price_at_sale NUMERIC(12,0) DEFAULT 0,  -- Precio Neto unitario al momento de vender
ADD COLUMN IF NOT EXISTS tax_amount_at_sale NUMERIC(12,0) DEFAULT 0; -- Monto de impuesto total de la línea

-- LIMPIEZA DE SALE_ITEMS (Eliminar basura)
-- Estas columnas no deberían estar en el detalle de una venta histórica, solo ocupan espacio
ALTER TABLE sale_items
DROP COLUMN IF EXISTS stock_current,
DROP COLUMN IF EXISTS stock_min,
DROP COLUMN IF EXISTS is_active,
DROP COLUMN IF EXISTS supplier_id;

-- 3. LIMPIEZA DE SALES (Cabecera)
-- La cabecera de venta no lleva detalles de producto individual
ALTER TABLE sales
DROP COLUMN IF EXISTS stock_current,
DROP COLUMN IF EXISTS stock_min,
DROP COLUMN IF EXISTS description,
DROP COLUMN IF EXISTS category_id,
DROP COLUMN IF EXISTS cost_price;