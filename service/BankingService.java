package service;

import exception.InvalidAmountException;
import model.Fund;
import model.User;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class BankingService {
    private final Scanner scanner;
    private final Map<String, User> users;
    private User currentUser;
    private boolean running;

    public BankingService(Scanner scanner) {
        this.scanner = scanner;
        this.users = new HashMap<>();
        this.running = true;
        initializeUsers();
    }

    private void initializeUsers() {
        String[] userNames = {"Alice", "Imran", "Valarie", "Denzel"};
        for (String name : userNames) {
            users.put(name.toLowerCase(), new User(name));
        }
    }

    public void start() {
        printWelcome();
        while (running) {
            if (currentUser == null) {
                login();
            } else {
                showMenu();
            }
        }
    }

    private void printWelcome() {
        System.out.println("============================");
        System.out.println(" BankingApp - Console Bank ");
        System.out.println("============================");
        System.out.println("Demo users: Alice, Imran, Valarie, Denzel");
    }

    private void login() {
        System.out.print("\nEnter your name to login, or type 'exit': ");
        String name = scanner.nextLine().trim();

        if (name.equalsIgnoreCase("exit")) {
            exit();
            return;
        }

        Optional<User> user = findUser(name);
        if (user.isPresent()) {
            currentUser = user.get();
            System.out.println("Welcome, " + currentUser.getName() + "!");
        } else {
            System.out.println("User not found. Try one of the demo names listed above.");
        }
    }

    private Optional<User> findUser(String name) {
        return Optional.ofNullable(users.get(name.toLowerCase()));
    }

    private void showMenu() {
        System.out.println("\n--- Banking App Menu ---");
        System.out.println("1. Show balances");
        System.out.println("2. Deposit cash to savings");
        System.out.println("3. Withdraw savings to cash");
        System.out.println("4. Send money to a person");
        System.out.println("5. Move savings to investment wallet");
        System.out.println("6. Invest wallet balance into a fund");
        System.out.println("7. Move investment wallet to savings");
        System.out.println("8. Withdraw all fund investments to wallet");
        System.out.println("9. Apply monthly interest/growth");
        System.out.println("10. View transaction history");
        System.out.println("11. Logout");
        System.out.println("12. Exit");
        System.out.print("Enter your choice: ");

        int choice = readInt();
        handleMenuChoice(choice);
    }

    private void handleMenuChoice(int choice) {
        switch (choice) {
            case 1 -> showBalance();
            case 2 -> depositMoney();
            case 3 -> withdrawMoney();
            case 4 -> sendMoney();
            case 5 -> transferSavingsToInvestment();
            case 6 -> investInFunds();
            case 7 -> transferInvestmentToSavings();
            case 8 -> withdrawAllInvestments();
            case 9 -> applyMonthlyGrowth();
            case 10 -> showTransactionHistory();
            case 11 -> logout();
            case 12 -> exit();
            default -> System.out.println("Invalid choice. Please enter a number between 1 and 12.");
        }
    }

    private void showBalance() {
        System.out.println("\n--- Balances for " + currentUser.getName() + " ---");
        System.out.printf("Cash: $%.2f%n", currentUser.getCash());
        System.out.printf("Savings: $%.2f%n", currentUser.getSavingsAccount().getBalance());
        System.out.printf("Investment wallet: $%.2f%n", currentUser.getInvestmentAccount().getBalance());
        System.out.printf("Total invested in funds: $%.2f%n", currentUser.getInvestmentAccount().getTotalInvested());

        for (Fund fund : Fund.values()) {
            System.out.printf("- %s fund: $%.2f%n",
                fund.getDisplayName(),
                currentUser.getInvestmentAccount().getInvestments().get(fund));
        }
    }

    private void depositMoney() {
        try {
            BigDecimal amount = readPositiveAmount("Enter amount to deposit to savings account: $");
            ensureSufficientFunds(currentUser.getCash(), amount, "Insufficient cash.");

            currentUser.subtractCash(amount);
            currentUser.getSavingsAccount().deposit(amount);
            currentUser.addTransaction("Deposited $" + format(amount) + " from cash to savings");
            System.out.println("Deposit successful.");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    private void withdrawMoney() {
        try {
            BigDecimal amount = readPositiveAmount("Enter amount to withdraw from savings account: $");
            ensureSufficientFunds(currentUser.getSavingsAccount().getBalance(), amount, "Insufficient funds in savings account.");

            currentUser.getSavingsAccount().withdraw(amount);
            currentUser.addCash(amount);
            currentUser.addTransaction("Withdrew $" + format(amount) + " from savings to cash");
            System.out.println("Withdrawal successful.");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    private void sendMoney() {
        try {
            System.out.println("Available recipients:");
            users.values().stream()
                .map(User::getName)
                .filter(name -> !name.equalsIgnoreCase(currentUser.getName()))
                .sorted()
                .forEach(name -> System.out.println("- " + name));

            System.out.print("Enter recipient's name: ");
            String recipientName = scanner.nextLine().trim();
            Optional<User> recipient = findUser(recipientName);

            if (recipient.isEmpty() || recipient.get().getName().equalsIgnoreCase(currentUser.getName())) {
                System.out.println("Invalid recipient.");
                return;
            }

            BigDecimal amount = readPositiveAmount("Enter amount to send from savings: $");
            ensureSufficientFunds(currentUser.getSavingsAccount().getBalance(), amount, "Insufficient funds in savings account.");

            currentUser.getSavingsAccount().withdraw(amount);
            recipient.get().getSavingsAccount().deposit(amount);
            currentUser.addTransaction("Sent $" + format(amount) + " to " + recipient.get().getName());
            recipient.get().addTransaction("Received $" + format(amount) + " from " + currentUser.getName());
            System.out.printf("Successfully sent $%.2f to %s.%n", amount, recipient.get().getName());
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    private void transferSavingsToInvestment() {
        try {
            BigDecimal amount = readPositiveAmount("Enter amount to move from savings to investment wallet: $");
            ensureSufficientFunds(currentUser.getSavingsAccount().getBalance(), amount, "Insufficient funds in savings account.");

            currentUser.getSavingsAccount().withdraw(amount);
            currentUser.getInvestmentAccount().deposit(amount);
            currentUser.addTransaction("Moved $" + format(amount) + " from savings to investment wallet");
            System.out.println("Transfer successful.");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    private void transferInvestmentToSavings() {
        try {
            BigDecimal amount = readPositiveAmount("Enter amount to move from investment wallet to savings: $");
            ensureSufficientFunds(currentUser.getInvestmentAccount().getBalance(), amount, "Insufficient funds in investment wallet.");

            currentUser.getInvestmentAccount().withdraw(amount);
            currentUser.getSavingsAccount().deposit(amount);
            currentUser.addTransaction("Moved $" + format(amount) + " from investment wallet to savings");
            System.out.println("Transfer successful.");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    private void investInFunds() {
        try {
            System.out.println("Available funds:");
            for (Fund fund : Fund.values()) {
                System.out.printf("- %s (%s%% monthly demo growth)%n",
                    fund.getDisplayName(),
                    fund.getAppreciationRate().multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString());
            }

            System.out.print("Enter fund to invest in: ");
            Fund fund = Fund.fromInput(scanner.nextLine());
            BigDecimal amount = readPositiveAmount("Enter amount to invest from investment wallet: $");
            ensureSufficientFunds(currentUser.getInvestmentAccount().getBalance(), amount, "Insufficient funds in investment wallet.");

            currentUser.getInvestmentAccount().invest(fund, amount);
            currentUser.addTransaction("Invested $" + format(amount) + " in " + fund.getDisplayName() + " fund");
            System.out.printf("Successfully invested $%.2f in %s fund.%n", amount, fund.getDisplayName());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid fund selection.");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    private void withdrawAllInvestments() {
        BigDecimal withdrawn = currentUser.getInvestmentAccount().withdrawInvestments();
        currentUser.addTransaction("Withdrew all fund investments ($" + format(withdrawn) + ") to investment wallet");
        System.out.println("All fund investments have been withdrawn to your investment wallet.");
    }

    private void applyMonthlyGrowth() {
        BigDecimal interest = currentUser.getSavingsAccount().applyInterest();
        currentUser.getInvestmentAccount().applyFundGrowth();
        currentUser.addTransaction("Applied monthly savings interest of $" + format(interest) + " and fund growth");
        System.out.println("Monthly interest and investment growth applied.");
    }

    private void showTransactionHistory() {
        System.out.println("\n--- Transaction History ---");
        List<String> transactions = currentUser.getTransactions();
        for (int index = transactions.size() - 1; index >= 0; index--) {
            System.out.println("- " + transactions.get(index));
        }
    }

    private BigDecimal readPositiveAmount(String prompt) throws InvalidAmountException {
        System.out.print(prompt);
        try {
            BigDecimal amount = new BigDecimal(scanner.nextLine().trim());
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidAmountException("Amount must be positive.");
            }
            return amount;
        } catch (NumberFormatException e) {
            throw new InvalidAmountException("Invalid amount format. Please enter a valid number.");
        }
    }

    private int readInt() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void ensureSufficientFunds(BigDecimal available, BigDecimal amount, String message) throws InvalidAmountException {
        if (available.compareTo(amount) < 0) {
            throw new InvalidAmountException(message);
        }
    }

    private String format(BigDecimal amount) {
        return String.format("%.2f", amount);
    }

    private void logout() {
        currentUser = null;
        System.out.println("You have been logged out.");
    }

    private void exit() {
        System.out.println("Thank you for using BankingApp. Goodbye!");
        running = false;
    }
}
