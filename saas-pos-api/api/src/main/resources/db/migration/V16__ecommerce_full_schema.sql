-- V16: Esquema Completo para E-commerce y Pedidos Web

-- 1. CONFIGURACIÓN DE TIENDA (Perfil Público por Tenant)
CREATE TABLE shop_configs (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    url_slug VARCHAR(50) NOT NULL, -- La parte amigable de la URL: saaspos.app/tienda/don-pepe
    shop_name VARCHAR(100),
    primary_color VARCHAR(10) DEFAULT '#000000',
    logo_url TEXT,
    banner_url TEXT,

    -- Configuración flexible (JSONB permite cambios futuros sin tocar la BD)
    payment_methods JSONB DEFAULT '{}',  -- Ej: { "transfer": true, "data": "CBU 123..." }
    shipping_methods JSONB DEFAULT '{}', -- Ej: { "pickup": true, "delivery_cost": 5000 }
    reservation_minutes INTEGER DEFAULT 60, -- Cuánto tiempo reservamos el stock

    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT uq_shop_slug UNIQUE (url_slug),
    CONSTRAINT fk_shop_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

-- 2. PEDIDOS WEB (Cabecera del Pedido)
CREATE TABLE web_orders (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    order_number VARCHAR(20) NOT NULL, -- ID corto legible: "WEB-1001"

    -- Datos del Cliente (Snapshot para historial y reclamos)
    customer_name VARCHAR(100) NOT NULL,
    customer_phone VARCHAR(50) NOT NULL,
    customer_email VARCHAR(100),
    shipping_address TEXT,
    shipping_notes TEXT,

    -- Estado y Tiempos
    status VARCHAR(20) NOT NULL, -- PENDING, EDITED, PAID, DELIVERED, CANCELLED, EXPIRED
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP, -- Si llega a esta hora y sigue PENDING, el sistema libera el stock

    -- Auditoría de Edición (Requerimiento Clave)
    was_edited BOOLEAN DEFAULT FALSE,
    edited_by_user_id UUID, -- Qué usuario del sistema lo modificó
    edit_reason TEXT,       -- Por qué se modificó (ej: "Cliente pidió agregar bebida")

    -- Totales Financieros
    total_items NUMERIC(15,4) DEFAULT 0,
    shipping_cost NUMERIC(15,4) DEFAULT 0,
    discount_amount NUMERIC(15,4) DEFAULT 0,
    final_total NUMERIC(15,4) DEFAULT 0,

    -- Métodos seleccionados
    payment_method VARCHAR(50),
    shipping_method VARCHAR(50),

    -- Enlace con Venta final (Cuando se completa y paga)
    sale_id UUID,

    CONSTRAINT fk_web_order_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_web_order_sale FOREIGN KEY (sale_id) REFERENCES sales(id)
);

-- 3. DETALLE DE PEDIDOS (Items)
CREATE TABLE web_order_items (
    id UUID PRIMARY KEY,
    web_order_id UUID NOT NULL,
    product_id UUID NOT NULL,

    -- Snapshot de datos (Para Historial Fidedigno si borran el producto original)
    product_name_snapshot VARCHAR(255),
    sku_snapshot VARCHAR(50),

    quantity NUMERIC(15,4) NOT NULL,

    -- Precios Snapshot (Para Analítica de Ganancias real de ese momento)
    unit_price_at_moment NUMERIC(15,4) NOT NULL,
    cost_price_at_moment NUMERIC(15,4) NOT NULL,

    subtotal NUMERIC(15,4) NOT NULL,

    CONSTRAINT fk_woi_order FOREIGN KEY (web_order_id) REFERENCES web_orders(id),
    CONSTRAINT fk_woi_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- 4. ACTUALIZAR PRODUCTOS (Para el Catálogo)
ALTER TABLE products
ADD COLUMN IF NOT EXISTS is_public BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS public_price NUMERIC(15,4), -- Si es null, usa el precio normal
ADD COLUMN IF NOT EXISTS image_url TEXT,
ADD COLUMN IF NOT EXISTS description_web TEXT;

-- Índices para que el Dashboard vuele 🚀
CREATE INDEX idx_web_orders_status ON web_orders(status);
CREATE INDEX idx_web_orders_created ON web_orders(created_at);
CREATE INDEX idx_shop_slug ON shop_configs(url_slug);