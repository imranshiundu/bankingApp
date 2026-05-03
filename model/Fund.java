package model;

import java.math.BigDecimal;

public enum Fund {
    LOW_RISK("Low Risk", "0.02"),
    MEDIUM_RISK("Medium Risk", "0.05"),
    HIGH_RISK("High Risk", "0.10");

    private final String displayName;
    private final BigDecimal appreciationRate;

    Fund(String displayName, String appreciationRate) {
        this.displayName = displayName;
        this.appreciationRate = new BigDecimal(appreciationRate);
    }

    public String getDisplayName() {
        return displayName;
    }

    public BigDecimal getAppreciationRate() {
        return appreciationRate;
    }

    public static Fund fromInput(String input) {
        String normalized = input.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        return Fund.valueOf(normalized);
    }
}
