CREATE TABLE category (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_category_normalized_name UNIQUE (normalized_name),
    CONSTRAINT ck_category_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT ck_category_normalized_name CHECK (
        btrim(normalized_name) <> '' AND normalized_name = lower(btrim(normalized_name))
    )
);

CREATE TABLE product (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT,
    CONSTRAINT ck_product_name_not_blank CHECK (btrim(name) <> ''),
    CONSTRAINT ck_product_description CHECK (
        btrim(description) <> '' AND char_length(description) <= 5000
    ),
    CONSTRAINT ck_product_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_product_status_created_at
    ON product (status, created_at DESC, id);

CREATE INDEX idx_product_category
    ON product (category_id);

CREATE TABLE product_image (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    external_id VARCHAR(255) NOT NULL,
    secure_url VARCHAR(2048) NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_product_image_product
        FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE RESTRICT,
    CONSTRAINT uk_product_image_external_id UNIQUE (external_id),
    CONSTRAINT uk_product_image_display_order UNIQUE (product_id, display_order),
    CONSTRAINT ck_product_image_external_id_not_blank CHECK (btrim(external_id) <> ''),
    CONSTRAINT ck_product_image_secure_url CHECK (secure_url LIKE 'https://%'),
    CONSTRAINT ck_product_image_display_order CHECK (display_order >= 0)
);

CREATE UNIQUE INDEX uk_product_image_primary
    ON product_image (product_id)
    WHERE is_primary = TRUE;

CREATE INDEX idx_product_image_product_order
    ON product_image (product_id, display_order);
