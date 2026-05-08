package ke.greendaybank.service;

import ke.greendaybank.repository.AuditRepository;
import ke.greendaybank.repository.PendingOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
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
        Map<String, String> payload = parseFlatJson(pending.requestPayload());
        String transactionRef = banking.move(
                payload.get("fromAccount"),
                payload.get("toAccount"),
                new BigDecimal(payload.get("amount")),
                payload.get("idempotencyKey") + "_APPROVED",
                payload.get("narration"),
                executor
        );
        pendingOperations.markExecuted(approvalId, transactionRef);
        audit.record(executor, "HIGH_RISK_MOVEMENT_EXECUTED", "APPROVAL", approvalId.toString(), Map.of("transactionRef", transactionRef));
        return transactionRef;
    }

    private Map<String, String> parseFlatJson(String json) {
        Map<String, String> values = new HashMap<>();
        String clean = json.trim();
        if (clean.startsWith("{")) clean = clean.substring(1);
        if (clean.endsWith("}")) clean = clean.substring(0, clean.length() - 1);
        if (clean.isBlank()) return values;
        for (String pair : clean.split(",")) {
            String[] parts = pair.split(":", 2);
            if (parts.length == 2) {
                values.put(unquote(parts[0]), unquote(parts[1]));
            }
        }
        return values;
    }

    private String unquote(String value) {
        String v = value.trim();
        if (v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length() - 1);
        }
        return v.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
