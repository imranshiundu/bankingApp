package ke.greendaybank.api;

import ke.greendaybank.repository.PendingOperationRepository;
import ke.greendaybank.service.ApprovalExecutionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/high-risk-movements")
public class HighRiskApprovalApiController {
    private final PendingOperationRepository pendingOperations;
    private final ApprovalExecutionService approvalExecution;

    public HighRiskApprovalApiController(PendingOperationRepository pendingOperations, ApprovalExecutionService approvalExecution) {
        this.pendingOperations = pendingOperations;
        this.approvalExecution = approvalExecution;
    }

    @GetMapping("/pending")
    public List<PendingOperationRepository.PendingMovement> pending(@RequestParam(defaultValue = "50") int limit) {
        return pendingOperations.pending(limit);
    }

    @PostMapping("/{approvalId}/approve")
    public DecisionResponse approve(@PathVariable UUID approvalId, @RequestBody @Valid DecisionRequest request) {
        approvalExecution.approve(approvalId, request.reviewer(), request.reason());
        return new DecisionResponse(approvalId, "APPROVED", null);
    }

    @PostMapping("/{approvalId}/reject")
    public DecisionResponse reject(@PathVariable UUID approvalId, @RequestBody @Valid DecisionRequest request) {
        approvalExecution.reject(approvalId, request.reviewer(), request.reason());
        return new DecisionResponse(approvalId, "REJECTED", null);
    }

    @PostMapping("/{approvalId}/execute")
    public DecisionResponse execute(@PathVariable UUID approvalId, @RequestBody @Valid ExecuteRequest request) {
        String transactionRef = approvalExecution.executeApprovedMovement(approvalId, request.executor());
        return new DecisionResponse(approvalId, "EXECUTED", transactionRef);
    }

    public record DecisionRequest(@NotBlank String reviewer, String reason) {}
    public record ExecuteRequest(@NotBlank String executor) {}
    public record DecisionResponse(UUID approvalId, String status, String transactionRef) {}
}
