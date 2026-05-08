package ke.greendaybank.approval;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ApprovalPolicy {
    private static final BigDecimal HIGH_RISK_KES_THRESHOLD = new BigDecimal("100000.00");

    public boolean movementRequiresApproval(BigDecimal amount) {
        return amount != null && amount.compareTo(HIGH_RISK_KES_THRESHOLD) >= 0;
    }

    public BigDecimal highRiskKesThreshold() {
        return HIGH_RISK_KES_THRESHOLD;
    }
}
