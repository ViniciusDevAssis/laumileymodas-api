CREATE INDEX idx_refresh_token_account ON refresh_token(account_id);
CREATE INDEX idx_refresh_token_family ON refresh_token(family_id);
CREATE INDEX idx_refresh_token_expires_at ON refresh_token(expires_at);
CREATE INDEX idx_oauth_handoff_expires_at ON oauth_handoff(expires_at);
CREATE INDEX idx_oauth_handoff_account ON oauth_handoff(account_id) WHERE account_id IS NOT NULL;
CREATE INDEX idx_interest_customer_created ON interest(customer_id, created_at DESC, id DESC);
CREATE INDEX idx_interest_product ON interest(product_id);
CREATE INDEX idx_reminder_pending_due ON reminder(status, due_at, id);
CREATE INDEX idx_contact_record_customer_status ON contact_record(customer_id, status, created_at DESC, id DESC);

ALTER TABLE customer DROP CONSTRAINT ck_customer_whatsapp_phone;
ALTER TABLE customer ADD CONSTRAINT ck_customer_whatsapp_phone
    CHECK (whatsapp_phone ~ '^\+[1-9][0-9]{7,14}$');
