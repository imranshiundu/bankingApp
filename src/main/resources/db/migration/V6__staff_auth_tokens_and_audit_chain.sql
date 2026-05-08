CREATE TABLE security.staff_api_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    staff_user_id UUID NOT NULL REFERENCES security.staff_users(id) ON DELETE CASCADE,
    token_prefix TEXT NOT NULL,
    token_hash TEXT NOT NULL UNIQUE,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ
);

CREATE INDEX idx_staff_api_tokens_prefix ON security.staff_api_tokens(token_prefix);
CREATE INDEX idx_staff_api_tokens_status ON security.staff_api_tokens(status);

ALTER TABLE audit.events
ADD COLUMN previous_hash TEXT,
ADD COLUMN event_hash TEXT;

CREATE UNIQUE INDEX idx_audit_events_event_hash
ON audit.events(event_hash)
WHERE event_hash IS NOT NULL;

CREATE INDEX idx_audit_events_previous_hash
ON audit.events(previous_hash)
WHERE previous_hash IS NOT NULL;
