-- 1. Eliminar cualquier versión incorrecta de la tabla (Singular o Plural)
DROP TABLE IF EXISTS public.product_price_history;
DROP TABLE IF EXISTS public.product_price_histories;

-- 2. Crear la tabla correcta (PLURAL) coincidiendo con la Entidad Java
CREATE TABLE public.product_price_histories (
    id bigserial NOT NULL,
    product_id uuid NOT NULL,
    old_price numeric(19, 2) NULL,
    new_price numeric(19, 2) NOT NULL,
    change_date timestamp(6) DEFAULT now(),
    change_reason varchar(255) NULL,
    changed_by_user_id uuid NULL,
    tenant_id uuid NOT NULL, -- Ahora consistente como UUID

    CONSTRAINT product_price_histories_pkey PRIMARY KEY (id),
    CONSTRAINT fk_pph_product FOREIGN KEY (product_id) REFERENCES public.products(id)
);

-- 3. Crear índice para optimizar búsquedas
CREATE INDEX idx_pph_product_id ON public.product_price_histories (product_id);