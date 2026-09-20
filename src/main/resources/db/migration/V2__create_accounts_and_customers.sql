create table account (
    id uuid primary key,
    email varchar(320) not null unique,
    password_hash varchar(255),
    role varchar(20) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint account_email_not_blank check (length(trim(email)) > 0),
    constraint account_role_check check (role in ('CLIENT', 'ADMIN'))
);

create unique index idx_account_single_admin
    on account(role)
    where role = 'ADMIN';

create table account_external_identity (
    id uuid primary key,
    account_id uuid not null references account(id) on delete cascade,
    provider varchar(30) not null,
    subject varchar(255) not null,
    created_at timestamptz not null,
    constraint account_external_identity_provider_check check (provider in ('GOOGLE')),
    constraint account_external_identity_subject_not_blank check (length(trim(subject)) > 0),
    constraint account_external_identity_provider_subject_unique unique (provider, subject),
    constraint account_external_identity_account_provider_unique unique (account_id, provider)
);

create table customer (
    id uuid primary key,
    account_id uuid not null unique references account(id) on delete cascade,
    first_name varchar(120) not null,
    last_name varchar(120) not null,
    whatsapp_phone varchar(20) not null,
    proactive_contact_authorized boolean not null default false,
    consent_granted_at timestamptz,
    consent_revoked_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint customer_first_name_not_blank check (length(trim(first_name)) > 0),
    constraint customer_last_name_not_blank check (length(trim(last_name)) > 0),
    constraint customer_whatsapp_phone_not_blank check (length(trim(whatsapp_phone)) > 0)
);

create index idx_customer_whatsapp_phone on customer(whatsapp_phone);
create index idx_customer_name on customer(last_name, first_name);

create table refresh_token (
    id uuid primary key,
    account_id uuid not null references account(id) on delete cascade,
    token_hash varchar(64) not null unique,
    family_id uuid not null,
    expires_at timestamptz not null,
    consumed_at timestamptz,
    revoked_at timestamptz,
    created_at timestamptz not null,
    constraint refresh_token_hash_hex check (token_hash ~ '^[0-9a-f]{64}$')
);

create index idx_refresh_token_account on refresh_token(account_id);
create index idx_refresh_token_family on refresh_token(family_id);
create index idx_refresh_token_expires_at on refresh_token(expires_at);
