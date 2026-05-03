package service;

import exception.InvalidAmountException;
import model.Fund;
import model.User;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BankingCore {
    private final Map<String, User> users;

    public BankingCore() {
        this.users = new HashMap<>();
        initializeUsers();
    }

    private void initializeUsers() {
        String[] userNames = {"Alice", "Imran", "Valarie", "Denzel"};
        for (String name : userNames) {
            users.put(name.toLowerCase(), new User(name));
        }
    }

    public synchronized Optional<User> findUser(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(users.get(name.trim().toLowerCase()));
    }

    public synchronized List<User> getUsers() {
        Collection<User> values = users.values();
        return values.stream()
            .sorted((first, second) -> first.getName().compareToIgnoreCase(second.getName()))
            .toList();
    }

    public synchronized void depositSavings(User user, BigDecimal amount) throws InvalidAmountException {
        validatePositive(amount);
        ensureSufficientFunds(user.getCash(), amount, "Insufficient cash.");
        user.subtractCash(amount);
        user.getSavingsAccount().deposit(amount);
        user.addTransaction("Deposited $" + format(amount) + " from cash to savings");
    }

    public synchronized void withdrawSavings(User user, BigDecimal amount) throws InvalidAmountException {
        validatePositive(amount);
        ensureSufficientFunds(user.getSavingsAccount().getBalance(), amount, "Insufficient funds in savings account.");
        user.getSavingsAccount().withdraw(amount);
        user.addCash(amount);
        user.addTransaction("Withdrew $" + format(amount) + " from savings to cash");
    }

    public synchronized void sendMoney(User sender, User recipient, BigDecimal amount) throws InvalidAmountException {
        validatePositive(amount);
        if (sender.getName().equalsIgnoreCase(recipient.getName())) {
            throw new InvalidAmountException("You cannot send money to yourself.");
        }
        ensureSufficientFunds(sender.getSavingsAccount().getBalance(), amount, "Insufficient funds in savings account.");
        sender.getSavingsAccount().withdraw(amount);
        recipient.getSavingsAccount().deposit(amount);
        sender.addTransaction("Sent $" + format(amount) + " to " + recipient.getName());
        recipient.addTransaction("Received $" + format(amount) + " from " + sender.getName());
    }

    public synchronized void transferSavingsToInvestment(User user, BigDecimal amount) throws InvalidAmountException {
        validatePositive(amount);
        ensureSufficientFunds(user.getSavingsAccount().getBalance(), amount, "Insufficient funds in savings account.");
        user.getSavingsAccount().withdraw(amount);
        user.getInvestmentAccount().deposit(amount);
        user.addTransaction("Moved $" + format(amount) + " from savings to investment wallet");
    }

    public synchronized void transferInvestmentToSavings(User user, BigDecimal amount) throws InvalidAmountException {
        validatePositive(amount);
        ensureSufficientFunds(user.getInvestmentAccount().getBalance(), amount, "Insufficient funds in investment wallet.");
        user.getInvestmentAccount().withdraw(amount);
        user.getSavingsAccount().deposit(amount);
        user.addTransaction("Moved $" + format(amount) + " from investment wallet to savings");
    }

    public synchronized void investInFund(User user, Fund fund, BigDecimal amount) throws InvalidAmountException {
        validatePositive(amount);
        ensureSufficientFunds(user.getInvestmentAccount().getBalance(), amount, "Insufficient funds in investment wallet.");
        user.getInvestmentAccount().invest(fund, amount);
        user.addTransaction("Invested $" + format(amount) + " in " + fund.getDisplayName() + " fund");
    }

    public synchronized BigDecimal withdrawAllInvestments(User user) {
        BigDecimal withdrawn = user.getInvestmentAccount().withdrawInvestments();
        user.addTransaction("Withdrew all fund investments ($" + format(withdrawn) + ") to investment wallet");
        return withdrawn;
    }

    public synchronized BigDecimal applyMonthlyGrowth(User user) {
        BigDecimal interest = user.getSavingsAccount().applyInterest();
        user.getInvestmentAccount().applyFundGrowth();
        user.addTransaction("Applied monthly savings interest of $" + format(interest) + " and fund growth");
        return interest;
    }

    public synchronized List<String> transactionHistory(User user) {
        return new ArrayList<>(user.getTransactions());
    }

    public BigDecimal parseAmount(String value) throws InvalidAmountException {
        try {
            BigDecimal amount = new BigDecimal(value.trim());
            validatePositive(amount);
            return amount;
        } catch (NumberFormatException | NullPointerException e) {
            throw new InvalidAmountException("Invalid amount format. Please enter a valid number.");
        }
    }

    public String format(BigDecimal amount) {
        return String.format("%.2f", amount);
    }

    private void validatePositive(BigDecimal amount) throws InvalidAmountException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidAmountException("Amount must be positive.");
        }
    }

    private void ensureSufficientFunds(BigDecimal available, BigDecimal amount, String message) throws InvalidAmountException {
        if (available.compareTo(amount) < 0) {
            throw new InvalidAmountException(message);
        }
    }
}
