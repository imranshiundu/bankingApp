package ke.greendaybank.security;

import java.util.Set;
import java.util.UUID;

public record StaffPrincipal(
        UUID staffUserId,
        String staffNumber,
        String displayName,
        Set<String> permissions
) {
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}
