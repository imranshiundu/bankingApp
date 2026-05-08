CREATE UNIQUE INDEX uq_pending_operations_executed_once
ON approvals.pending_operations(id)
WHERE status = 'EXECUTED';

CREATE INDEX idx_pending_operations_result_ref
ON approvals.pending_operations(result_ref)
WHERE result_ref IS NOT NULL;
