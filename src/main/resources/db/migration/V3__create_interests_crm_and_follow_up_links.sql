CREATE TABLE interest (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
    product_id UUID NOT NULL REFERENCES product(id) ON DELETE RESTRICT,
    idempotency_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_interest_customer_idempotency UNIQUE (customer_id, idempotency_key),
    CONSTRAINT ck_interest_idempotency_key CHECK (btrim(idempotency_key) <> '')
);

CREATE TABLE reminder (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
    interest_id UUID NOT NULL REFERENCES interest(id) ON DELETE RESTRICT,
    description VARCHAR(1000) NOT NULL,
    due_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_reminder_interest UNIQUE (interest_id),
    CONSTRAINT ck_reminder_description CHECK (btrim(description) <> ''),
    CONSTRAINT ck_reminder_status CHECK (status IN ('PENDING', 'COMPLETED')),
    CONSTRAINT ck_reminder_completion CHECK ((status = 'PENDING' AND completed_at IS NULL) OR (status = 'COMPLETED' AND completed_at IS NOT NULL))
);

CREATE TABLE contact_record (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL REFERENCES customer(id) ON DELETE RESTRICT,
    interest_id UUID NOT NULL REFERENCES interest(id) ON DELETE RESTRICT,
    reminder_id UUID NOT NULL REFERENCES reminder(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMPTZ,
    channel VARCHAR(80) NOT NULL,
    description VARCHAR(2000),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_contact_record_interest UNIQUE (interest_id),
    CONSTRAINT uk_contact_record_reminder UNIQUE (reminder_id),
    CONSTRAINT ck_contact_record_channel CHECK (btrim(channel) <> ''),
    CONSTRAINT ck_contact_record_status CHECK (status IN ('PENDING', 'COMPLETED')),
    CONSTRAINT ck_contact_record_completion CHECK (
      (status = 'PENDING' AND occurred_at IS NULL AND description IS NULL AND completed_at IS NULL)
      OR (status = 'COMPLETED' AND occurred_at IS NOT NULL AND btrim(description) <> '' AND completed_at IS NOT NULL)
    )
);
