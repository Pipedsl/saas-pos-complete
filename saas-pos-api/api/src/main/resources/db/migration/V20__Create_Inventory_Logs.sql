CREATE TABLE inventory_logs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,

    product_id UUID,
    product_name_snapshot VARCHAR(255), -- Guardamos el nombre por si borran el producto original

    user_id UUID,
    user_name_snapshot VARCHAR(255),    -- Guardamos el nombre del usuario por si se elimina

    action_type VARCHAR(50),            -- Ej: 'CREATE', 'UPDATE', 'SALE', 'WEB_ORDER', 'RETURN'

    quantity_change DECIMAL(10,3),      -- Ej: -2.000, +50.000
    old_stock DECIMAL(10,3),            -- Stock antes del movimiento
    new_stock DECIMAL(10,3),            -- Stock después del movimiento

    reason TEXT,                        -- Motivo: "Venta #100", "Ajuste manual", "Pedido Web #WEB-102"

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para que el reporte cargue rápido
CREATE INDEX idx_logs_tenant ON inventory_logs(tenant_id);
CREATE INDEX idx_logs_created_at ON inventory_logs(created_at);