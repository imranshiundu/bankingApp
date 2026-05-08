package ke.greendaybank.security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

/**
 * Twofish encrypted vault for sensitive banking records.
 *
 * Design:
 * - Twofish/CBC/PKCS7Padding for confidentiality.
 * - HmacSHA256 over version + salt + iv + ciphertext for tamper detection.
 * - PBKDF2WithHmacSHA256 derives independent encryption and MAC keys.
 *
 * Note: production banks should use an HSM/KMS for master-key custody.
 */
public final class TwofishVault {
    private static final String VERSION = "GDV1";
    private static final int SALT_BYTES = 16;
    private static final int IV_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final int DERIVED_BITS = 512;
    private static final int PBKDF2_ITERATIONS = 210_000;
    private static final SecureRandom RANDOM = new SecureRandom();

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private TwofishVault() {}

    public static String encrypt(String plaintext, char[] passphrase) {
        try {
            byte[] salt = randomBytes(SALT_BYTES);
            byte[] iv = randomBytes(IV_BYTES);
            KeyMaterial keys = deriveKeys(passphrase, salt);

            Cipher cipher = Cipher.getInstance("Twofish/CBC/PKCS7Padding", BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, keys.encryptionKey(), new IvParameterSpec(iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] header = VERSION.getBytes(StandardCharsets.UTF_8);
            byte[] mac = hmac(keys.macKey(), header, salt, iv, ciphertext);
            ByteBuffer payload = ByteBuffer.allocate(header.length + salt.length + iv.length + ciphertext.length + mac.length);
            payload.put(header).put(salt).put(iv).put(ciphertext).put(mac);
            return Base64.getEncoder().encodeToString(payload.array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public static String decrypt(String encodedPayload, char[] passphrase) {
        try {
            byte[] payload = Base64.getDecoder().decode(encodedPayload);
            int minSize = VERSION.length() + SALT_BYTES + IV_BYTES + 32 + 1;
            if (payload.length < minSize) {
                throw new SecurityException("Invalid encrypted payload");
            }

            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] header = new byte[VERSION.length()];
            byte[] salt = new byte[SALT_BYTES];
            byte[] iv = new byte[IV_BYTES];
            buffer.get(header).get(salt).get(iv);

            String version = new String(header, StandardCharsets.UTF_8);
            if (!VERSION.equals(version)) {
                throw new SecurityException("Unsupported vault payload version");
            }

            int ciphertextLength = payload.length - VERSION.length() - SALT_BYTES - IV_BYTES - 32;
            byte[] ciphertext = new byte[ciphertextLength];
            byte[] suppliedMac = new byte[32];
            buffer.get(ciphertext).get(suppliedMac);

            KeyMaterial keys = deriveKeys(passphrase, salt);
            byte[] expectedMac = hmac(keys.macKey(), header, salt, iv, ciphertext);
            if (!constantTimeEquals(expectedMac, suppliedMac)) {
                throw new SecurityException("Encrypted payload failed integrity check");
            }

            Cipher cipher = Cipher.getInstance("Twofish/CBC/PKCS7Padding", BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.DECRYPT_MODE, keys.encryptionKey(), new IvParameterSpec(iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new SecurityException("Decryption failed", e);
        }
    }

    private static KeyMaterial deriveKeys(char[] passphrase, byte[] salt) throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(passphrase, salt, PBKDF2_ITERATIONS, DERIVED_BITS);
        byte[] derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        byte[] enc = new byte[KEY_BITS / 8];
        byte[] mac = new byte[KEY_BITS / 8];
        System.arraycopy(derived, 0, enc, 0, enc.length);
        System.arraycopy(derived, enc.length, mac, 0, mac.length);
        return new KeyMaterial(new SecretKeySpec(enc, "Twofish"), new SecretKeySpec(mac, "HmacSHA256"));
    }

    private static byte[] hmac(SecretKeySpec key, byte[]... parts) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(key);
        for (byte[] part : parts) {
            mac.update(part);
        }
        return mac.doFinal();
    }

    private static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    private static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    private record KeyMaterial(SecretKeySpec encryptionKey, SecretKeySpec macKey) {}
}
