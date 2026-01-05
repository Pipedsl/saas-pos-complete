-- V25__Add_Logistics_Config.sql

-- Campo para marcar un courier como "Favorito" o "Recomendado"
ALTER TABLE shop_configs ADD COLUMN IF NOT EXISTS recommended_courier VARCHAR(50);

-- Campo JSON para guardar los días. Ej: {"starken": ["Lunes", "Viernes"], "blue": ["Martes"]}
-- Usamos JSONB para PostgreSQL (o TEXT si usas otra DB y dejas que Java lo maneje)
ALTER TABLE shop_configs ADD COLUMN IF NOT EXISTS dispatch_days JSONB;