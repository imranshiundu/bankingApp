package ke.greendaybank.service;

import ke.greendaybank.approval.HighRiskMovementPayload;
import ke.greendaybank.repository.AuditRepository;
import ke.greendaybank.repository.PendingOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class ApprovalExecutionService {
    private final PendingOperationRepository pendingOperations;
    private final BankingApplicationService banking;
    private final AuditRepository audit;

    public ApprovalExecutionService(PendingOperationRepository pendingOperations, BankingApplicationService banking, AuditRepository audit) {
        this.pendingOperations = pendingOperations;
        this.banking = banking;
        this.audit = audit;
    }

    public UUID requestHighRiskMovement(String fromAccount, String toAccount, BigDecimal amount, String narration, String idempotencyKey, String actor) {
        UUID approvalId = pendingOperations.createMovement(fromAccount, toAccount, amount, narration, idempotencyKey, actor);
        audit.record(actor, "HIGH_RISK_MOVEMENT_APPROVAL_REQUESTED", "APPROVAL", approvalId.toString(), Map.of(
                "from", fromAccount,
                "to", toAccount,
                "amount", amount.toPlainString()
        ));
        return approvalId;
    }

    public void approve(UUID approvalId, String reviewer, String reason) {
        pendingOperations.markApproved(approvalId, reviewer, reason);
        audit.record(reviewer, "HIGH_RISK_MOVEMENT_APPROVED", "APPROVAL", approvalId.toString(), Map.of());
    }

    public void reject(UUID approvalId, String reviewer, String reason) {
        pendingOperations.markRejected(approvalId, reviewer, reason);
        audit.record(reviewer, "HIGH_RISK_MOVEMENT_REJECTED", "APPROVAL", approvalId.toString(), Map.of("reason", reason == null ? "" : reason));
    }

    @Transactional
    public String executeApprovedMovement(UUID approvalId, String executor) {
        PendingOperationRepository.PendingMovement pending = pendingOperations.getPendingMovement(approvalId);
        if (!"APPROVED".equals(pending.status())) {
            throw new IllegalStateException("Operation is not approved");
        }
        HighRiskMovementPayload payload = pendingOperations.movementPayload(approvalId);
        String transactionRef = banking.move(
                payload.fromAccount(),
                payload.toAccount(),
                payload.amount(),
                payload.idempotencyKey() + "_APPROVED",
                payload.narration(),
                executor
        );
        pendingOperations.markExecuted(approvalId, transactionRef);
        audit.record(executor, "HIGH_RISK_MOVEMENT_EXECUTED", "APPROVAL", approvalId.toString(), Map.of("transactionRef", transactionRef));
        return transactionRef;
    }
}
