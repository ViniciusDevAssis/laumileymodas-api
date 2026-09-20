create table category (
    id uuid primary key,
    name varchar(120) not null,
    normalized_name varchar(120) not null unique,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint category_name_not_blank check (length(trim(name)) > 0),
    constraint category_normalized_name_not_blank check (length(trim(normalized_name)) > 0)
);

create table product (
    id uuid primary key,
    category_id uuid not null references category(id),
    name varchar(200) not null,
    description varchar(2000) not null,
    status varchar(20) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint product_name_not_blank check (length(trim(name)) > 0),
    constraint product_description_not_blank check (length(trim(description)) > 0),
    constraint product_status_check check (status in ('ACTIVE', 'INACTIVE'))
);

create index idx_product_status on product(status);
create index idx_product_category_status on product(category_id, status);

create table product_image (
    id uuid primary key,
    product_id uuid not null references product(id) on delete cascade,
    url varchar(2048) not null,
    external_id varchar(255) not null unique,
    is_primary boolean not null,
    display_order integer not null,
    created_at timestamptz not null,
    constraint product_image_url_not_blank check (length(trim(url)) > 0),
    constraint product_image_external_id_not_blank check (length(trim(external_id)) > 0),
    constraint product_image_display_order_non_negative check (display_order >= 0),
    constraint product_image_product_display_order_unique unique (product_id, display_order)
);

create unique index idx_product_image_single_primary
    on product_image(product_id)
    where is_primary = true;
