package ke.greendaybank.repository;

import ke.greendaybank.config.VaultProperties;
import ke.greendaybank.customer.CustomerProfile;
import ke.greendaybank.identity.SecureReferenceGenerator;
import ke.greendaybank.security.TwofishVault;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Repository
public class CustomerRepository {
    private final JdbcTemplate jdbcTemplate;
    private final SecureReferenceGenerator references;
    private final VaultProperties vaultProperties;

    public CustomerRepository(JdbcTemplate jdbcTemplate, SecureReferenceGenerator references, VaultProperties vaultProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.references = references;
        this.vaultProperties = vaultProperties;
    }

    @Transactional
    public CustomerAccount openCustomer(CustomerProfile profile) {
        String customerRef = references.customerRef();
        UUID customerId = jdbcTemplate.queryForObject(
                "INSERT INTO core.customers(customer_ref, status) VALUES (?, 'PENDING_KYC') RETURNING id",
                UUID.class,
                customerRef
        );
        String encrypted = TwofishVault.encrypt(profile.serialize(), vaultProperties.passphraseChars());
        jdbcTemplate.update("""
                INSERT INTO core.customer_encrypted_profiles(customer_id, vault_version, encryption_algorithm, key_reference, encrypted_payload, payload_hmac)
                VALUES (?, 'GDV1', 'Twofish/CBC/PKCS7Padding', ?, ?, 'embedded-hmac')
                """, customerId, vaultProperties.keyReference(), encrypted);

        String accountNumber = references.accountNumber();
        jdbcTemplate.update("""
                INSERT INTO core.accounts(account_number, customer_id, kind, currency, status)
                VALUES (?, ?, 'CUSTOMER_DEPOSIT', 'KES', 'PENDING_KYC')
                """, accountNumber, customerId);
        return new CustomerAccount(customerId, customerRef, accountNumber);
    }

    public boolean accountExists(String accountNumber) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM core.accounts WHERE account_number = ?", Integer.class, accountNumber);
        return count != null && count > 0;
    }

    public record CustomerAccount(UUID customerId, String customerRef, String accountNumber) {}
}
