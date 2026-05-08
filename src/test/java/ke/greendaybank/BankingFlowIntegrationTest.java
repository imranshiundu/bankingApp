package ke.greendaybank;

import ke.greendaybank.repository.CustomerRepository;
import ke.greendaybank.repository.StatementRepository;
import ke.greendaybank.service.BankingApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BankingFlowIntegrationTest extends PostgresIntegrationBase {
    @Autowired
    BankingApplicationService banking;

    @Test
    void accountCreditMovementBalanceAndStatementWork() {
        CustomerRepository.CustomerAccount source = banking.openAccount(profile("Source Customer", "SRC-1"), "maker-a");
        CustomerRepository.CustomerAccount destination = banking.openAccount(profile("Destination Customer", "DST-1"), "maker-a");

        String creditRef = banking.credit(source.accountNumber(), new BigDecimal("2500.00"), "IK_TEST_CREDIT_000000000001", "Initial account credit", "maker-a");
        String moveRef = banking.move(source.accountNumber(), destination.accountNumber(), new BigDecimal("750.00"), "IK_TEST_MOVE_0000000000001", "Customer movement", "maker-a");

        assertNotNull(creditRef);
        assertNotNull(moveRef);
        assertEquals(new BigDecimal("1750.00"), banking.balance(source.accountNumber()));
        assertEquals(new BigDecimal("750.00"), banking.balance(destination.accountNumber()));

        List<StatementRepository.StatementLine> sourceLines = banking.statement(source.accountNumber(), 10);
        assertEquals(2, sourceLines.size());
        assertTrue(sourceLines.stream().anyMatch(line -> line.transactionRef().equals(creditRef)));
        assertTrue(sourceLines.stream().anyMatch(line -> line.transactionRef().equals(moveRef)));
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
