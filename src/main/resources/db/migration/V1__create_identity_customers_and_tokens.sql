CREATE TABLE account (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    normalized_email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255),
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_account_normalized_email UNIQUE (normalized_email),
    CONSTRAINT ck_account_email_not_blank CHECK (btrim(email) <> ''),
    CONSTRAINT ck_account_normalized_email CHECK (
        btrim(normalized_email) <> '' AND normalized_email = lower(btrim(normalized_email))
    ),
    CONSTRAINT ck_account_role CHECK (role IN ('CLIENT', 'ADMIN')),
    CONSTRAINT ck_account_admin_without_password CHECK (role <> 'ADMIN' OR password_hash IS NULL)
);

CREATE UNIQUE INDEX uk_account_single_admin
    ON account ((1))
    WHERE role = 'ADMIN';

CREATE TABLE account_external_identity (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    provider VARCHAR(30) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    email_at_link VARCHAR(320) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    last_login_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_external_identity_account
        FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE RESTRICT,
    CONSTRAINT uk_external_identity_provider_subject UNIQUE (provider, subject),
    CONSTRAINT ck_external_identity_provider CHECK (provider = 'GOOGLE'),
    CONSTRAINT ck_external_identity_subject_not_blank CHECK (btrim(subject) <> ''),
    CONSTRAINT ck_external_identity_email_not_blank CHECK (btrim(email_at_link) <> '')
);

CREATE TABLE customer (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    whatsapp_phone VARCHAR(20) NOT NULL,
    proactive_contact_authorized BOOLEAN NOT NULL DEFAULT FALSE,
    contact_consent_changed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_customer_account
        FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE RESTRICT,
    CONSTRAINT uk_customer_account UNIQUE (account_id),
    CONSTRAINT ck_customer_first_name_not_blank CHECK (btrim(first_name) <> ''),
    CONSTRAINT ck_customer_last_name_not_blank CHECK (btrim(last_name) <> ''),
    CONSTRAINT ck_customer_whatsapp_phone CHECK (whatsapp_phone ~ '^\\+[1-9][0-9]{7,14}$')
);

CREATE TABLE refresh_token (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    family_id UUID NOT NULL,
    jti_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    replaced_by_id UUID,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_refresh_token_account
        FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE RESTRICT,
    CONSTRAINT fk_refresh_token_replacement
        FOREIGN KEY (replaced_by_id) REFERENCES refresh_token (id) ON DELETE RESTRICT,
    CONSTRAINT uk_refresh_token_jti_hash UNIQUE (jti_hash),
    CONSTRAINT uk_refresh_token_replacement UNIQUE (replaced_by_id),
    CONSTRAINT ck_refresh_token_jti_hash_not_blank CHECK (btrim(jti_hash) <> ''),
    CONSTRAINT ck_refresh_token_expiration CHECK (expires_at > created_at)
);

CREATE TABLE oauth_handoff (
    id UUID PRIMARY KEY,
    handle_hash VARCHAR(255) NOT NULL,
    purpose VARCHAR(40) NOT NULL,
    account_id UUID,
    provider VARCHAR(30) NOT NULL,
    provider_subject VARCHAR(255) NOT NULL,
    verified_email VARCHAR(320) NOT NULL,
    given_name VARCHAR(100),
    family_name VARCHAR(100),
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_oauth_handoff_account
        FOREIGN KEY (account_id) REFERENCES account (id) ON DELETE RESTRICT,
    CONSTRAINT uk_oauth_handoff_handle_hash UNIQUE (handle_hash),
    CONSTRAINT ck_oauth_handoff_purpose CHECK (
        purpose IN ('EXISTING_ACCOUNT_LOGIN', 'CLIENT_REGISTRATION')
    ),
    CONSTRAINT ck_oauth_handoff_account_by_purpose CHECK (
        (purpose = 'EXISTING_ACCOUNT_LOGIN' AND account_id IS NOT NULL)
        OR (purpose = 'CLIENT_REGISTRATION' AND account_id IS NULL)
    ),
    CONSTRAINT ck_oauth_handoff_provider CHECK (provider = 'GOOGLE'),
    CONSTRAINT ck_oauth_handoff_handle_not_blank CHECK (btrim(handle_hash) <> ''),
    CONSTRAINT ck_oauth_handoff_subject_not_blank CHECK (btrim(provider_subject) <> ''),
    CONSTRAINT ck_oauth_handoff_email_not_blank CHECK (btrim(verified_email) <> ''),
    CONSTRAINT ck_oauth_handoff_expiration CHECK (
        expires_at > created_at AND expires_at <= created_at + INTERVAL '10 minutes'
    )
);
