package ke.greendaybank.service;

import ke.greendaybank.customer.CustomerProfile;
import ke.greendaybank.identity.SecureReferenceGenerator;
import ke.greendaybank.repository.AuditRepository;
import ke.greendaybank.repository.CustomerRepository;
import ke.greendaybank.repository.LedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
public class BankingApplicationService {
    private final CustomerRepository customerRepository;
    private final LedgerRepository ledgerRepository;
    private final AuditRepository auditRepository;
    private final SecureReferenceGenerator references;

    public BankingApplicationService(
            CustomerRepository customerRepository,
            LedgerRepository ledgerRepository,
            AuditRepository auditRepository,
            SecureReferenceGenerator references
    ) {
        this.customerRepository = customerRepository;
        this.ledgerRepository = ledgerRepository;
        this.auditRepository = auditRepository;
        this.references = references;
    }

    @Transactional
    public CustomerRepository.CustomerAccount openAccount(Map<String, String> privateProfile, String actor) {
        CustomerProfile profile = new CustomerProfile(
                require(privateProfile, "displayName"),
                require(privateProfile, "identityRef"),
                require(privateProfile, "contactRef"),
                privateProfile.getOrDefault("emailRef", ""),
                privateProfile.getOrDefault("taxRef", "")
        );
        CustomerRepository.CustomerAccount account = customerRepository.openCustomer(profile);
        auditRepository.record(actor, "ACCOUNT_OPENED", "ACCOUNT", account.accountNumber(), Map.of(
                "customerRef", account.customerRef()
        ));
        return account;
    }

    @Transactional
    public String credit(String accountNumber, BigDecimal amount, String idempotencyKey, String narration, String actor) {
        String key = cleanIdempotencyKey(idempotencyKey);
        String transactionRef = ledgerRepository.postCredit(accountNumber, amount, key, narration);
        auditRepository.record(actor, "ACCOUNT_CREDIT_POSTED", "ACCOUNT", accountNumber, Map.of(
                "transactionRef", transactionRef,
                "amount", amount.toPlainString()
        ));
        return transactionRef;
    }

    @Transactional
    public String move(String fromAccountNumber, String toAccountNumber, BigDecimal amount, String idempotencyKey, String narration, String actor) {
        String key = cleanIdempotencyKey(idempotencyKey);
        String transactionRef = ledgerRepository.postMovement(fromAccountNumber, toAccountNumber, amount, key, narration);
        auditRepository.record(actor, "ACCOUNT_MOVEMENT_POSTED", "ACCOUNT", fromAccountNumber, Map.of(
                "transactionRef", transactionRef,
                "to", toAccountNumber,
                "amount", amount.toPlainString()
        ));
        return transactionRef;
    }

    public BigDecimal balance(String accountNumber) {
        return ledgerRepository.balance(accountNumber);
    }

    private String cleanIdempotencyKey(String incoming) {
        if (incoming == null || incoming.isBlank()) {
            return references.idempotencyKey();
        }
        if (incoming.length() < 24) {
            throw new IllegalArgumentException("Idempotency key is too short");
        }
        return incoming;
    }

    private String require(Map<String, String> map, String key) {
        String value = map.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required private profile field: " + key);
        }
        return value.trim();
    }
}
