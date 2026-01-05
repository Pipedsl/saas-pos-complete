-- V23__Add_Bundles_Support.sql

-- 1. Agregar tipo de producto para diferenciar
-- Valores: 'STANDARD', 'BUNDLE', 'SERVICE'
ALTER TABLE products ADD COLUMN IF NOT EXISTS product_type VARCHAR(50) DEFAULT 'STANDARD';

-- 2. Tabla para los componentes del pack
CREATE TABLE bundle_items (
    id UUID PRIMARY KEY,
    bundle_product_id UUID NOT NULL, -- El Padre (El Pack)
    component_product_id UUID NOT NULL, -- El Hijo (El Perfume individual)
    quantity DECIMAL(10,3) NOT NULL DEFAULT 1, -- Cuántos de este hijo van en el pack

    CONSTRAINT fk_bundle_parent FOREIGN KEY (bundle_product_id) REFERENCES products(id),
    CONSTRAINT fk_bundle_child FOREIGN KEY (component_product_id) REFERENCES products(id)
);

CREATE INDEX idx_bundle_parent ON bundle_items(bundle_product_id);