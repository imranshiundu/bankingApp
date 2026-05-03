package model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public abstract class Account {
    protected BigDecimal balance;

    protected Account() {
        this.balance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void deposit(BigDecimal amount) {
        balance = balance.add(normalize(amount));
    }

    public void withdraw(BigDecimal amount) {
        balance = balance.subtract(normalize(amount));
    }

    protected BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
