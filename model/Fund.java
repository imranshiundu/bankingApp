// model/Fund.java
package model;

public enum Fund {
    LOW_RISK(0.02),
    MEDIUM_RISK(0.05),
    HIGH_RISK(0.10);

    private final double appreciationRate;

    Fund(double appreciationRate) {
        this.appreciationRate = appreciationRate;
    }

    public double getAppreciationRate() {
        return appreciationRate;
    }
}