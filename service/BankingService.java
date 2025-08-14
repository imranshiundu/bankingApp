// service/BankingService.java
package service;

import model.*;
import exception.InvalidAmountException;

import java.math.BigDecimal;
import java.util.*;

public class BankingService {
    private final Scanner scanner;
    private final Map<String, User> users;
    private User currentUser;

    public BankingService(Scanner scanner) {
        this.scanner = scanner;
        this.users = new HashMap<>();
        initializeUsers();
    }

    private void initializeUsers() {
        String[] userNames = {"Alice", "Imran", "Valarie", "Denzel"};
        for (String name : userNames) {
            users.put(name, new User(name));
        }
    }

    public void start() {
        while (true) {
            if (currentUser == null) {
                login();
            } else {
                showMenu();
            }
        }
    }

    private void login() {
        System.out.print("Enter your name to login: ");
        String name = scanner.nextLine();
        
        if (users.containsKey(name)) {
            currentUser = users.get(name);
            System.out.println("Welcome, " + name + "!");
        } else {
            System.out.println("User not found. Please try again.");
        }
    }

    private void showMenu() {
        System.out.println("--- Banking App Menu ---");
        System.out.println("1. Show balance");
        System.out.println("2. Deposit money");
        System.out.println("3. Withdraw money");
        System.out.println("4. Send money to a person");
        System.out.println("5. Invest in funds");
        System.out.println("6. Transfer between accounts");
        System.out.println("7. Withdraw all investments");
        System.out.println("8. Logout");
        System.out.println("9. Exit");
        System.out.print("Enter your choice: ");

        try {
            int choice = Integer.parseInt(scanner.nextLine());
            handleMenuChoice(choice);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a number between 1 and 9.");
        }
    }

    private void handleMenuChoice(int choice) {
        switch (choice) {
            case 1 -> showBalance();
            case 2 -> depositMoney();
            case 3 -> withdrawMoney();
            case 4 -> sendMoney();
            case 5 -> investInFunds();
            case 6 -> transferBetweenAccounts();
            case 7 -> withdrawAllInvestments();
            case 8 -> logout();
            case 9 -> exit();
            default -> System.out.println("Invalid choice. Please try again.");
        }
    }

    private void showBalance() {
        // Apply interest and growth before showing balance
        currentUser.getSavingsAccount().applyInterest();
        currentUser.getInvestmentAccount().applyFundGrowth();

        System.out.printf("Savings account balance: $%.2f%n", 
            currentUser.getSavingsAccount().getBalance());
        
        System.out.println("Investment account balance:");
        System.out.printf("Not Invested: $%.2f%n", 
            currentUser.getInvestmentAccount().getBalance());
        
        for (Fund fund : Fund.values()) {
            System.out.printf("%s: $%.2f%n", 
                fund.name().replace("_", " "), 
                currentUser.getInvestmentAccount().getInvestments().get(fund));
        }
    }

    private void depositMoney() {
        try {
            System.out.print("Enter amount to deposit to savings account: $");
            BigDecimal amount = new BigDecimal(scanner.nextLine());
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidAmountException("Amount must be positive.");
            }
            
            if (currentUser.getCash().compareTo(amount) < 0) {
                throw new InvalidAmountException("Insufficient cash.");
            }
            
            currentUser.subtractCash(amount);
            currentUser.getSavingsAccount().deposit(amount);
            System.out.println("Deposit successful.");
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount format. Please enter a valid number.");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    private void withdrawMoney() {
        try {
            System.out.print("Enter amount to withdraw from savings account: $");
            BigDecimal amount = new BigDecimal(scanner.nextLine());
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidAmountException("Amount must be positive.");
            }
            
            if (currentUser.getSavingsAccount().getBalance().compareTo(amount) < 0) {
                throw new InvalidAmountException("Insufficient funds in savings account.");
            }
            
            currentUser.getSavingsAccount().withdraw(amount);
            currentUser.addCash(amount);
            System.out.println("Withdrawal successful.");
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount format. Please enter a valid number.");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    private void sendMoney() {
        try {
            System.out.println("Available recipients:");
            users.keySet().stream()
                .filter(name -> !name.equals(currentUser.getName()))
                .forEach(System.out::println);
            
            System.out.print("Enter recipient's name: ");
            String recipientName = scanner.nextLine();
            
            if (!users.containsKey(recipientName) || recipientName.equals(currentUser.getName())) {
                System.out.println("Invalid recipient.");
                return;
            }
            
            System.out.print("Enter amount to send: $");
            BigDecimal amount = new BigDecimal(scanner.nextLine());
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidAmountException("Amount must be positive.");
            }
            
            if (currentUser.getSavingsAccount().getBalance().compareTo(amount) < 0) {
                throw new InvalidAmountException("Insufficient funds in savings account.");
            }
            
            // Perform transfer
            currentUser.getSavingsAccount().withdraw(amount);
            users.get(recipientName).getSavingsAccount().deposit(amount);
            System.out.printf("Successfully sent $%.2f to %s.%n", amount, recipientName);
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount format. Please enter a valid number.");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    private void investInFunds() {
        try {
            System.out.println("Available funds:");
            for (Fund fund : Fund.values()) {
                System.out.println(fund.name().replace("_", " "));
            }
            
            System.out.print("Enter fund to invest in: ");
            String fundInput = scanner.nextLine().toUpperCase().replace(" ", "_");
            
            Fund fund;
            try {
                fund = Fund.valueOf(fundInput);
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid fund selection.");
                return;
            }
            
            System.out.print("Enter amount to invest: $");
            BigDecimal amount = new BigDecimal(scanner.nextLine());
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidAmountException("Amount must be positive.");
            }
            
            if (currentUser.getInvestmentAccount().getBalance().compareTo(amount) < 0) {
                throw new InvalidAmountException("Insufficient funds in investment account.");
            }
            
            currentUser.getInvestmentAccount().invest(fund, amount);
            System.out.printf("Successfully invested $%.2f in %s fund.%n", 
                amount, fund.name().replace("_", " "));
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount format. Please enter a valid number.");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    private void transferBetweenAccounts() {
        try {
            System.out.println("1. Transfer from savings to investment");
            System.out.println("2. Transfer from investment to savings");
            System.out.print("Enter your choice: ");
            int choice = Integer.parseInt(scanner.nextLine());
            
            System.out.print("Enter amount to transfer: $");
            BigDecimal amount = new BigDecimal(scanner.nextLine());
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidAmountException("Amount must be positive.");
            }
            
            switch (choice) {
                case 1 -> {
                    if (currentUser.getSavingsAccount().getBalance().compareTo(amount) < 0) {
                        throw new InvalidAmountException("Insufficient funds in savings account.");
                    }
                    currentUser.getSavingsAccount().withdraw(amount);
                    currentUser.getInvestmentAccount().deposit(amount);
                    System.out.printf("Successfully transferred $%.2f to investment account.%n", amount);
                }
                case 2 -> {
                    if (currentUser.getInvestmentAccount().getBalance().compareTo(amount) < 0) {
                        throw new InvalidAmountException("Insufficient funds in investment account.");
                    }
                    currentUser.getInvestmentAccount().withdraw(amount);
                    currentUser.getSavingsAccount().deposit(amount);
                    System.out.printf("Successfully transferred $%.2f to savings account.%n", amount);
                }
                default -> System.out.println("Invalid choice.");
            }
            
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a valid number.");
        } catch (InvalidAmountException e) {
            System.out.println(e.getMessage());
        }
    }

    private void withdrawAllInvestments() {
        currentUser.getInvestmentAccount().withdrawInvestments();
        System.out.println("All investments have been withdrawn and added to your investment account balance.");
    }

    private void logout() {
        currentUser = null;
        System.out.println("You have been logged out.");
    }

    private void exit() {
        System.out.println("Thank you for using our banking app. Goodbye!");
        System.exit(0);
    }
}