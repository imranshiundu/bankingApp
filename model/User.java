// model/User.java
package model;

import java.math.BigDecimal;

public class User {
    private final String name;
    private BigDecimal cash;
    private final SavingsAccount savingsAccount;
    private final InvestmentAccount investmentAccount;

    public User(String name) {
        this.name = name;
        this.cash = new BigDecimal("1000");
        this.savingsAccount = new SavingsAccount();
        this.investmentAccount = new InvestmentAccount();
    }

    // Getters and setters
    public String getName() { return name; }
    public BigDecimal getCash() { return cash; }
    public SavingsAccount getSavingsAccount() { return savingsAccount; }
    public InvestmentAccount getInvestmentAccount() { return investmentAccount; }

    public void addCash(BigDecimal amount) {
        cash = cash.add(amount);
    }

    public void subtractCash(BigDecimal amount) {
        cash = cash.subtract(amount);
    }
}