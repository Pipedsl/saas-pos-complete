-- Permitir decimales en la cantidad de ítems vendidos (para soporte Granel)
ALTER TABLE sale_items
ALTER COLUMN quantity TYPE NUMERIC(12,3); -- 3 decimales para precisión de peso (ej: 0.125 kg)