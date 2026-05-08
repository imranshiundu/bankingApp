CREATE TYPE approvals.operation_status AS ENUM ('PENDING_APPROVAL', 'APPROVED', 'REJECTED', 'EXECUTED', 'CANCELLED');

CREATE TABLE approvals.pending_operations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    operation_type TEXT NOT NULL,
    status approvals.operation_status NOT NULL DEFAULT 'PENDING_APPROVAL',
    request_payload JSONB NOT NULL,
    requested_by_label TEXT NOT NULL,
    reviewed_by_label TEXT,
    review_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMPTZ,
    executed_at TIMESTAMPTZ,
    result_ref TEXT
);

CREATE INDEX idx_pending_operations_status ON approvals.pending_operations(status);
CREATE INDEX idx_pending_operations_operation_type ON approvals.pending_operations(operation_type);
CREATE INDEX idx_pending_operations_created_at ON approvals.pending_operations(created_at);
