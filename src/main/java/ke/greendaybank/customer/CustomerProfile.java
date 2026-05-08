package ke.greendaybank.customer;

public record CustomerProfile(
        String fullName,
        String nationalId,
        String phoneNumber,
        String email,
        String kraPin
) {
    public String serialize() {
        return escape(fullName) + "|" + escape(nationalId) + "|" + escape(phoneNumber) + "|" + escape(email) + "|" + escape(kraPin);
    }

    public static CustomerProfile deserialize(String raw) {
        String[] parts = raw.split("\\|", -1);
        if (parts.length != 5) {
            throw new IllegalArgumentException("Invalid customer profile payload");
        }
        return new CustomerProfile(unescape(parts[0]), unescape(parts[1]), unescape(parts[2]), unescape(parts[3]), unescape(parts[4]));
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("|", "\\p");
    }

    private static String unescape(String value) {
        return value.replace("\\p", "|").replace("\\\\", "\\");
    }
}
