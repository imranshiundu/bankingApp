package ke.greendaybank.api;

import ke.greendaybank.repository.ApprovalRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/approvals")
public class ApprovalApiController {
    private final ApprovalRepository approvals;

    public ApprovalApiController(ApprovalRepository approvals) {
        this.approvals = approvals;
    }

    @GetMapping("/pending")
    public List<ApprovalRepository.ApprovalItem> pending(@RequestParam(defaultValue = "50") int limit) {
        return approvals.pending(limit);
    }

    @PostMapping
    public ApprovalCreatedResponse request(@RequestBody @Valid ApprovalRequest request) {
        UUID id = approvals.request(request.resourceType(), request.resourceId(), request.action(), request.requestedBy());
        return new ApprovalCreatedResponse(id, "PENDING");
    }

    @PostMapping("/{approvalId}/approve")
    public ApprovalDecisionResponse approve(@PathVariable UUID approvalId, @RequestBody @Valid ApprovalDecisionRequest request) {
        approvals.approve(approvalId, request.reviewedBy());
        return new ApprovalDecisionResponse(approvalId, "APPROVED");
    }

    @PostMapping("/{approvalId}/reject")
    public ApprovalDecisionResponse reject(@PathVariable UUID approvalId, @RequestBody @Valid ApprovalDecisionRequest request) {
        approvals.reject(approvalId, request.reviewedBy(), request.reason());
        return new ApprovalDecisionResponse(approvalId, "REJECTED");
    }

    public record ApprovalRequest(@NotBlank String resourceType, UUID resourceId, @NotBlank String action, UUID requestedBy) {}
    public record ApprovalDecisionRequest(UUID reviewedBy, String reason) {}
    public record ApprovalCreatedResponse(UUID approvalId, String status) {}
    public record ApprovalDecisionResponse(UUID approvalId, String status) {}
}
