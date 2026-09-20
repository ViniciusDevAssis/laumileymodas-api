create table interest (
    id uuid primary key,
    customer_id uuid not null references customer(id) on delete cascade,
    product_id uuid not null references product(id),
    idempotency_key varchar(100) not null,
    created_at timestamptz not null,
    constraint interest_idempotency_key_not_blank check (length(trim(idempotency_key)) > 0),
    constraint interest_customer_idempotency_unique unique (customer_id, idempotency_key)
);

create index idx_interest_customer_created_at on interest(customer_id, created_at desc);
create index idx_interest_product on interest(product_id);

create table reminder (
    id uuid primary key,
    interest_id uuid not null unique references interest(id) on delete cascade,
    description varchar(1000) not null,
    due_at timestamptz not null,
    status varchar(20) not null,
    completed_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint reminder_description_not_blank check (length(trim(description)) > 0),
    constraint reminder_status_check check (status in ('PENDING', 'COMPLETED')),
    constraint reminder_completed_at_check check (
        (status = 'PENDING' and completed_at is null)
        or (status = 'COMPLETED' and completed_at is not null)
    )
);

create index idx_reminder_status_due_at on reminder(status, due_at);

create table contact_record (
    id uuid primary key,
    customer_id uuid not null references customer(id) on delete cascade,
    interest_id uuid unique references interest(id) on delete cascade,
    status varchar(20) not null,
    occurred_at timestamptz,
    channel varchar(40) not null,
    description varchar(2000),
    completed_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint contact_record_status_check check (status in ('PENDING', 'COMPLETED')),
    constraint contact_record_channel_not_blank check (length(trim(channel)) > 0),
    constraint contact_record_completed_fields_check check (
        (status = 'PENDING' and occurred_at is null and completed_at is null)
        or (
            status = 'COMPLETED'
            and occurred_at is not null
            and completed_at is not null
            and description is not null
            and length(trim(description)) > 0
        )
    )
);

create index idx_contact_record_customer_status_occurred_at
    on contact_record(customer_id, status, occurred_at desc);
