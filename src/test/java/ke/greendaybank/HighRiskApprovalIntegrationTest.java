package ke.greendaybank;

import ke.greendaybank.repository.CustomerRepository;
import ke.greendaybank.service.ApprovalExecutionService;
import ke.greendaybank.service.BankingApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class HighRiskApprovalIntegrationTest extends PostgresIntegrationBase {
    @Autowired
    BankingApplicationService banking;

    @Autowired
    ApprovalExecutionService approvals;

    @Test
    void approvedHighRiskMovementExecutesOnce() {
        CustomerRepository.CustomerAccount source = banking.openAccount(profile("Large Source", "L-SRC-1"), "maker-large");
        CustomerRepository.CustomerAccount destination = banking.openAccount(profile("Large Destination", "L-DST-1"), "maker-large");
        banking.credit(source.accountNumber(), new BigDecimal("250000.00"), "IK_APPROVAL_CREDIT_000000001", "Large initial credit", "maker-large");

        UUID approvalId = approvals.requestHighRiskMovement(source.accountNumber(), destination.accountNumber(), new BigDecimal("150000.00"), "High risk movement", "IK_APPROVED_MOVE_000000000001", "maker-large");

        assertThrows(IllegalStateException.class, () -> approvals.approve(approvalId, "maker-large", "self approval should fail"));

        approvals.approve(approvalId, "checker-large", "approved after review");
        String transactionRef = approvals.executeApprovedMovement(approvalId, "executor-large");

        assertNotNull(transactionRef);
        assertMoney("100000.00", banking.balance(source.accountNumber()));
        assertMoney("150000.00", banking.balance(destination.accountNumber()));
        assertThrows(IllegalStateException.class, () -> approvals.executeApprovedMovement(approvalId, "executor-large"));
    }

    @Test
    void rejectedHighRiskMovementDoesNotExecute() {
        CustomerRepository.CustomerAccount source = banking.openAccount(profile("Reject Source", "R-SRC-1"), "maker-reject");
        CustomerRepository.CustomerAccount destination = banking.openAccount(profile("Reject Destination", "R-DST-1"), "maker-reject");
        banking.credit(source.accountNumber(), new BigDecimal("200000.00"), "IK_REJECT_CREDIT_0000000001", "Reject initial credit", "maker-reject");

        UUID approvalId = approvals.requestHighRiskMovement(source.accountNumber(), destination.accountNumber(), new BigDecimal("120000.00"), "Rejected high risk movement", "IK_REJECTED_MOVE_0000000001", "maker-reject");
        approvals.reject(approvalId, "checker-reject", "Rejected test");

        assertThrows(IllegalStateException.class, () -> approvals.executeApprovedMovement(approvalId, "executor-reject"));
        assertMoney("200000.00", banking.balance(source.accountNumber()));
        assertMoney("0.00", banking.balance(destination.accountNumber()));
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private Map<String, String> profile(String name, String ref) {
        return Map.of(
                "displayName", name,
                "identityRef", ref,
                "contactRef", "contact-" + ref,
                "emailRef", "email-" + ref,
                "taxRef", "tax-" + ref
        );
    }
}
