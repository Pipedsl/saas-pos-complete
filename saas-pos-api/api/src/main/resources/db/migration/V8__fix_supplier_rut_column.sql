-- V8: Corregir nombre de columna en suppliers para que coincida con Java

DO $$
BEGIN
    -- Si existe 'tax_id', la renombramos a 'rut'
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='suppliers' AND column_name='tax_id') THEN
        ALTER TABLE suppliers RENAME COLUMN tax_id TO rut;
    END IF;

    -- Si por alguna razón no existe ni tax_id ni rut, creamos rut
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='suppliers' AND column_name='rut') THEN
        ALTER TABLE suppliers ADD COLUMN rut VARCHAR(50);
    END IF;
END $$;