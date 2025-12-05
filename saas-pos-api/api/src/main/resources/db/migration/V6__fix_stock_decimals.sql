-- Cambiar el stock de ENTERO a DECIMAL (con 3 decimales de precisión)
ALTER TABLE products
ALTER COLUMN stock_current TYPE NUMERIC(12,3);

ALTER TABLE products
ALTER COLUMN stock_min TYPE NUMERIC(12,3);