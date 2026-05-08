ALTER TABLE approvals.pending_operations
ADD COLUMN idempotency_key TEXT;

ALTER TABLE approvals.pending_operations
ADD CONSTRAINT uq_pending_operations_operation_idempotency
UNIQUE (operation_type, idempotency_key);

CREATE INDEX idx_pending_operations_idempotency_key
ON approvals.pending_operations(idempotency_key)
WHERE idempotency_key IS NOT NULL;
