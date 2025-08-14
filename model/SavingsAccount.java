// model/SavingsAccount.java
package model;

import java.math.BigDecimal;

public class SavingsAccount extends Account {
    private static final BigDecimal INTEREST_RATE = new BigDecimal("0.01");

    public void applyInterest() {
        BigDecimal interest = balance.multiply(INTEREST_RATE);
        deposit(interest);
    }
}