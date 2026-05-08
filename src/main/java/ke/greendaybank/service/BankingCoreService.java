package ke.greendaybank.service;

import ke.greendaybank.audit.AuditLog;
import ke.greendaybank.core.Ledger;
import ke.greendaybank.core.Money;
import ke.greendaybank.customer.CustomerProfile;
import ke.greendaybank.customer.CustomerRecord;
import ke.greendaybank.customer.CustomerStatus;
import ke.greendaybank.security.TwofishVault;

import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BankingCoreService {
    private static final String SUSPENSE_ACCOUNT = "GL-SUSPENSE-KES";

    private final Ledger ledger;
    private final AuditLog auditLog;
    private final Map<String, CustomerRecord> customersByAccount = new ConcurrentHashMap<>();
    private final Clock clock;

    public BankingCoreService(Clock clock) {
        this.clock = clock;
        this.ledger = new Ledger(clock);
        this.auditLog = new AuditLog(clock);
        ledger.registerAccount(SUSPENSE_ACCOUNT, Money.KES);
    }

    public CustomerRecord onboardCustomer(CustomerProfile profile, char[] vaultPassphrase, String actor) {
        String accountNumber = generateAccountNumber();
        String encryptedProfile = TwofishVault.encrypt(profile.serialize(), vaultPassphrase);
        CustomerRecord record = new CustomerRecord(UUID.randomUUID(), accountNumber, encryptedProfile, Instant.now(clock), CustomerStatus.PENDING_KYC);
        customersByAccount.put(accountNumber, record);
        ledger.registerAccount(accountNumber, Money.KES);
        auditLog.record(actor, "CUSTOMER_ONBOARDED", accountNumber, Map.of("status", record.status().name()));
        return record;
    }

    public CustomerProfile readCustomerProfile(String accountNumber, char[] vaultPassphrase, String actor) {
        CustomerRecord record = requireCustomer(accountNumber);
        auditLog.record(actor, "CUSTOMER_PROFILE_READ", accountNumber, Map.of());
        return CustomerProfile.deserialize(TwofishVault.decrypt(record.encryptedProfile(), vaultPassphrase));
    }

    public UUID depositKes(String accountNumber, String amount, String narration, String actor) {
        requireCustomer(accountNumber);
        UUID transactionId = ledger.postExternalCredit(accountNumber, Money.kes(amount), SUSPENSE_ACCOUNT, narration);
        auditLog.record(actor, "DEPOSIT_POSTED", accountNumber, Map.of("transactionId", transactionId.toString(), "amount", amount));
        return transactionId;
    }

    public UUID transferKes(String fromAccountNumber, String toAccountNumber, String amount, String narration, String actor) {
        requireCustomer(fromAccountNumber);
        requireCustomer(toAccountNumber);
        UUID transactionId = ledger.postTransfer(fromAccountNumber, toAccountNumber, Money.kes(amount), narration);
        auditLog.record(actor, "TRANSFER_POSTED", fromAccountNumber, Map.of(
                "transactionId", transactionId.toString(),
                "to", toAccountNumber,
                "amount", amount
        ));
        return transactionId;
    }

    public Money balanceKes(String accountNumber) {
        requireCustomer(accountNumber);
        return ledger.balanceOf(accountNumber, Currency.getInstance("KES"));
    }

    public Ledger ledger() {
        return ledger;
    }

    public AuditLog auditLog() {
        return auditLog;
    }

    private CustomerRecord requireCustomer(String accountNumber) {
        CustomerRecord record = customersByAccount.get(accountNumber);
        if (record == null) {
            throw new IllegalArgumentException("Unknown customer account");
        }
        if (record.status() == CustomerStatus.FROZEN || record.status() == CustomerStatus.CLOSED) {
            throw new IllegalStateException("Customer account is not operational");
        }
        return record;
    }

    private String generateAccountNumber() {
        long value = Math.abs(UUID.randomUUID().getMostSignificantBits());
        return "GD" + String.format("%012d", value % 1_000_000_000_000L);
    }
}
