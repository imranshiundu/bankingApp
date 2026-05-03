package model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SavingsAccount extends Account {
    private static final BigDecimal INTEREST_RATE = new BigDecimal("0.01");

    public BigDecimal applyInterest() {
        BigDecimal interest = balance.multiply(INTEREST_RATE).setScale(2, RoundingMode.HALF_UP);
        deposit(interest);
        return interest;
    }
}
