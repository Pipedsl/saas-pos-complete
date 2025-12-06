-- Gestión de Suscripciones y Límites Extra

ALTER TABLE tenants
ADD COLUMN subscription_end_date TIMESTAMP WITH TIME ZONE, -- Cuándo te cortamos el servicio
ADD COLUMN plan_status VARCHAR(20) DEFAULT 'ACTIVE',       -- ACTIVE, SUSPENDED, EXPIRED
ADD COLUMN max_cashiers_extra INTEGER DEFAULT 0;           -- Cajeros comprados aparte del plan base