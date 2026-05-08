package ke.greendaybank.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TwofishVaultTest {
    @Test
    void encryptsAndDecryptsPayload() {
        char[] passphrase = "correct horse battery staple".toCharArray();
        String payload = TwofishVault.encrypt("name=Imran|nationalId=12345678", passphrase);

        assertNotEquals("name=Imran|nationalId=12345678", payload);
        assertEquals("name=Imran|nationalId=12345678", TwofishVault.decrypt(payload, passphrase));
    }

    @Test
    void rejectsWrongPassphrase() {
        String payload = TwofishVault.encrypt("sensitive", "right-pass".toCharArray());
        assertThrows(SecurityException.class, () -> TwofishVault.decrypt(payload, "wrong-pass".toCharArray()));
    }

    @Test
    void rejectsTamperedPayload() {
        String payload = TwofishVault.encrypt("sensitive", "right-pass".toCharArray());
        String tampered = payload.substring(0, payload.length() - 2) + "AA";
        assertThrows(SecurityException.class, () -> TwofishVault.decrypt(tampered, "right-pass".toCharArray()));
    }
}
