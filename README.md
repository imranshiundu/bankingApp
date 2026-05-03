# BankingApp

BankingApp is a simple Java console banking simulation. It is designed as a clean beginner-to-intermediate Java project for practicing object-oriented programming, input validation, account models, money movement, and basic investment-account flows.

The app runs fully in the terminal. It does not use a database yet, so all balances reset when the program exits.

## What the app can do

- Login using demo users
- View cash, savings, investment wallet, and fund balances
- Deposit cash into savings
- Withdraw savings back to cash
- Send money from one user to another
- Move money from savings into an investment wallet
- Invest wallet funds into low, medium, or high-risk demo funds
- Withdraw all fund investments back into the investment wallet
- Apply monthly savings interest and fund growth manually
- View a simple transaction history for the current session

## Demo users

The app ships with four in-memory users:

- Alice
- Imran
- Valarie
- Denzel

Every user starts with `$1,000.00` in cash.

## Project structure

```text
bankingApp/
├── BankingApp.java
├── exception/
│   └── InvalidAmountException.java
├── model/
│   ├── Account.java
│   ├── Fund.java
│   ├── InvestmentAccount.java
│   ├── SavingsAccount.java
│   └── User.java
└── service/
    └── BankingService.java
```

## Requirements

- Java 17 or newer is recommended
- A terminal or command prompt

The project uses plain Java only. No Maven or Gradle setup is required.

## How to run

From the repository root, compile the project:

```bash
javac BankingApp.java service/*.java model/*.java exception/*.java
```

Then run it:

```bash
java BankingApp
```

## Example session

```text
============================
 BankingApp - Console Bank
============================
Demo users: Alice, Imran, Valarie, Denzel

Enter your name to login, or type 'exit': Imran
Welcome, Imran!

--- Banking App Menu ---
1. Show balances
2. Deposit cash to savings
3. Withdraw savings to cash
4. Send money to a person
5. Move savings to investment wallet
6. Invest wallet balance into a fund
7. Move investment wallet to savings
8. Withdraw all fund investments to wallet
9. Apply monthly interest/growth
10. View transaction history
11. Logout
12. Exit
```

## Important design notes

This project is a learning/demo banking app, not a real banking system.

Current behavior:

- Data is stored in memory only.
- There is no password authentication yet.
- Interest and fund growth only apply when the user chooses the monthly growth option.
- Money is handled with `BigDecimal` for safer decimal arithmetic.
- Transactions are stored only for the current runtime session.

## Recent upgrades

The project was upgraded from a fragile demo into a cleaner console application:

- Added a proper README with setup and usage instructions
- Made login case-insensitive
- Added safer money formatting and two-decimal handling
- Prevented balance viewing from secretly changing account balances
- Separated savings, investment wallet, and invested fund balances more clearly
- Added transaction history
- Replaced `System.exit(0)` with a controlled app shutdown flag
- Improved fund input handling and fund display names
- Added clearer validation and user-facing messages

## Possible next improvements

Good next steps for this project:

- Add password/PIN login
- Add account numbers
- Add persistent storage using files or SQLite
- Add unit tests
- Add Maven or Gradle
- Add a GUI or web interface
- Add transaction timestamps
- Add admin user management

## License

No license has been added yet. Add one before using this as an open-source project.
