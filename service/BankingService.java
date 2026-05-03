package service;

import exception.InvalidAmountException;
import model.Fund;
import model.User;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class BankingService {
    private final Scanner scanner;
    private final BankingCore bankingCore;
    private User currentUser;
    private boolean running;

    public BankingService(Scanner scanner) {
        this(scanner, new BankingCore());
    }

    public BankingService(Scanner scanner, BankingCore bankingCore) {
        this.scanner = scanner;
        this.bankingCore = bankingCore;
        this.running = true;
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
        System.out.println("Web mode: java WebBankingApp");
    }

    private void login() {
        System.out.print("\nEnter your name to login, or type 'exit': ");
        String name = scanner.nextLine().trim();

        if (name.equalsIgnoreCase("exit")) {
            exit();
            return;
        }

        Optional<User> user = bankingCore.findUser(name);
        if (user.isPresent()) {
            currentUser = user.get();
            System.out.println("Welcome, " + currentUser.getName() + "!");
        } else {
            System.out.println("User not found. Try one of the demo names listed above.");
        }
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
            bankingCore.depositSavings(currentUser, amount);
            System.out.println("Deposit successful.");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    private void withdrawMoney() {
        try {
            BigDecimal amount = readPositiveAmount("Enter amount to withdraw from savings account: $");
            bankingCore.withdrawSavings(currentUser, amount);
            System.out.println("Withdrawal successful.");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    private void sendMoney() {
        try {
            System.out.println("Available recipients:");
            bankingCore.getUsers().stream()
                .map(User::getName)
                .filter(name -> !name.equalsIgnoreCase(currentUser.getName()))
                .forEach(name -> System.out.println("- " + name));

            System.out.print("Enter recipient's name: ");
            String recipientName = scanner.nextLine().trim();
            Optional<User> recipient = bankingCore.findUser(recipientName);

            if (recipient.isEmpty()) {
                System.out.println("Invalid recipient.");
                return;
            }

            BigDecimal amount = readPositiveAmount("Enter amount to send from savings: $");
            bankingCore.sendMoney(currentUser, recipient.get(), amount);
            System.out.printf("Successfully sent $%.2f to %s.%n", amount, recipient.get().getName());
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    private void transferSavingsToInvestment() {
        try {
            BigDecimal amount = readPositiveAmount("Enter amount to move from savings to investment wallet: $");
            bankingCore.transferSavingsToInvestment(currentUser, amount);
            System.out.println("Transfer successful.");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    private void transferInvestmentToSavings() {
        try {
            BigDecimal amount = readPositiveAmount("Enter amount to move from investment wallet to savings: $");
            bankingCore.transferInvestmentToSavings(currentUser, amount);
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
            bankingCore.investInFund(currentUser, fund, amount);
            System.out.printf("Successfully invested $%.2f in %s fund.%n", amount, fund.getDisplayName());
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid fund selection.");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    private void withdrawAllInvestments() {
        bankingCore.withdrawAllInvestments(currentUser);
        System.out.println("All fund investments have been withdrawn to your investment wallet.");
    }

    private void applyMonthlyGrowth() {
        bankingCore.applyMonthlyGrowth(currentUser);
        System.out.println("Monthly interest and investment growth applied.");
    }

    private void showTransactionHistory() {
        System.out.println("\n--- Transaction History ---");
        List<String> transactions = bankingCore.transactionHistory(currentUser);
        for (int index = transactions.size() - 1; index >= 0; index--) {
            System.out.println("- " + transactions.get(index));
        }
    }

    private BigDecimal readPositiveAmount(String prompt) throws InvalidAmountException {
        System.out.print(prompt);
        return bankingCore.parseAmount(scanner.nextLine());
    }

    private int readInt() {
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
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
