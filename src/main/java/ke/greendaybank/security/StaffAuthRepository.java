package ke.greendaybank.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public class StaffAuthRepository {
    private final JdbcTemplate jdbcTemplate;

    public StaffAuthRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<StaffPrincipal> findByBearerToken(String token) {
        if (token == null || token.length() < 32) {
            return Optional.empty();
        }
        String prefix = token.substring(0, Math.min(12, token.length()));
        String hash = sha256(token);
        return jdbcTemplate.query("""
                SELECT su.id, su.staff_number, su.full_name
                FROM security.staff_api_tokens sat
                JOIN security.staff_users su ON su.id = sat.staff_user_id
                WHERE sat.token_prefix = ?
                  AND sat.token_hash = ?
                  AND sat.status = 'ACTIVE'
                  AND su.status = 'ACTIVE'
                  AND (sat.expires_at IS NULL OR sat.expires_at > now())
                """, rs -> {
            if (!rs.next()) {
                return Optional.<StaffPrincipal>empty();
            }
            UUID staffId = rs.getObject("id", UUID.class);
            Set<String> permissions = loadPermissions(staffId);
            jdbcTemplate.update("UPDATE security.staff_api_tokens SET last_used_at = now() WHERE token_hash = ?", hash);
            return Optional.of(new StaffPrincipal(
                    staffId,
                    rs.getString("staff_number"),
                    rs.getString("full_name"),
                    permissions
            ));
        }, prefix, hash);
    }

    private Set<String> loadPermissions(UUID staffId) {
        return new HashSet<>(jdbcTemplate.query("""
                SELECT p.code
                FROM security.staff_user_roles sur
                JOIN security.role_permissions rp ON rp.role_id = sur.role_id
                JOIN security.permissions p ON p.id = rp.permission_id
                WHERE sur.staff_user_id = ?
                """, (rs, rowNum) -> rs.getString("code"), staffId));
    }

    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
