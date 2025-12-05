-- Agregamos la unidad de medida. Por defecto todo es UNIDAD.
ALTER TABLE products
ADD COLUMN measurement_unit VARCHAR(20) DEFAULT 'UNIT' NOT NULL;

-- UNIT = Por unidad
-- KG   = Por Kilo (Granel)