package ke.greendaybank.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "greenday.vault")
public record VaultProperties(String passphrase, String keyReference) {
    public char[] passphraseChars() {
        return passphrase == null ? new char[0] : passphrase.toCharArray();
    }
}
