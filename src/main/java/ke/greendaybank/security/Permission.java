package ke.greendaybank.security;

public final class Permission {
    private Permission() {}

    public static final String ACCOUNT_OPEN = "ACCOUNT_OPEN";
    public static final String LEDGER_CREDIT = "LEDGER_CREDIT";
    public static final String LEDGER_MOVE = "LEDGER_MOVE";
    public static final String STATEMENT_READ = "STATEMENT_READ";
    public static final String APPROVAL_REVIEW = "APPROVAL_REVIEW";
    public static final String APPROVAL_EXECUTE = "APPROVAL_EXECUTE";
    public static final String AUDIT_READ = "AUDIT_READ";
}
