CREATE SCHEMA IF NOT EXISTS api;

CREATE TYPE api.idempotency_status AS ENUM ('PROCESSING', 'COMPLETED', 'FAILED');

CREATE TABLE api.idempotency_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key TEXT NOT NULL UNIQUE,
    request_hash TEXT NOT NULL,
    operation TEXT NOT NULL,
    status api.idempotency_status NOT NULL DEFAULT 'PROCESSING',
    response_code INT,
    response_body JSONB,
    failure_code TEXT,
    locked_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_idempotency_records_operation ON api.idempotency_records(operation);
CREATE INDEX idx_idempotency_records_status ON api.idempotency_records(status);
CREATE INDEX idx_idempotency_records_created_at ON api.idempotency_records(created_at);

CREATE INDEX idx_ledger_entries_account_posted_sequence ON core.ledger_entries(account_id, posted_at DESC, sequence_no DESC);
CREATE INDEX idx_transactions_ref_status ON core.transactions(transaction_ref, status);

CREATE VIEW core.account_statement_lines AS
SELECT
    le.sequence_no,
    le.posted_at,
    t.transaction_ref,
    t.transaction_type,
    a.account_number,
    le.side,
    le.amount,
    le.currency,
    le.narration,
    SUM(CASE WHEN le.side = 'CREDIT' THEN le.amount ELSE -le.amount END)
        OVER (PARTITION BY le.account_id ORDER BY le.sequence_no ASC) AS running_balance
FROM core.ledger_entries le
JOIN core.transactions t ON t.id = le.transaction_id
JOIN core.accounts a ON a.id = le.account_id;
