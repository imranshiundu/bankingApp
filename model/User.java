package model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class User {
    private final String name;
    private BigDecimal cash;
    private final SavingsAccount savingsAccount;
    private final InvestmentAccount investmentAccount;
    private final List<String> transactions;

    public User(String name) {
        this.name = name;
        this.cash = new BigDecimal("1000.00");
        this.savingsAccount = new SavingsAccount();
        this.investmentAccount = new InvestmentAccount();
        this.transactions = new ArrayList<>();
        addTransaction("Account created with starting cash of $1,000.00");
    }

    public String getName() { return name; }
    public BigDecimal getCash() { return cash; }
    public SavingsAccount getSavingsAccount() { return savingsAccount; }
    public InvestmentAccount getInvestmentAccount() { return investmentAccount; }
    public List<String> getTransactions() { return Collections.unmodifiableList(transactions); }

    public void addCash(BigDecimal amount) {
        cash = cash.add(normalize(amount));
    }

    public void subtractCash(BigDecimal amount) {
        cash = cash.subtract(normalize(amount));
    }

    public void addTransaction(String transaction) {
        transactions.add(transaction);
    }

    private BigDecimal normalize(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
