# BankingApp

BankingApp is a simple Java banking simulation that can be used from both the terminal and a local web interface. It is designed as a clean beginner-to-intermediate Java project for practicing object-oriented programming, input validation, account models, money movement, simple investment flows, and Java's built-in HTTP server.

The app does not use a database yet, so all balances reset when the program exits.

## Interfaces

BankingApp now has two ways to use the same banking engine:

1. **CLI mode** - terminal menu for direct console usage.
2. **Web mode** - browser interface powered by Java's built-in `HttpServer`.

Both interfaces use the same shared `BankingCore` service so the business logic stays consistent.

## What the app can do

- Login/switch between demo users
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
├── BankingApp.java              # CLI entry point
├── WebBankingApp.java           # Web entry point
├── exception/
│   └── InvalidAmountException.java
├── model/
│   ├── Account.java
│   ├── Fund.java
│   ├── InvestmentAccount.java
│   ├── SavingsAccount.java
│   └── User.java
├── service/
│   ├── BankingCore.java         # Shared banking engine used by CLI and web
│   └── BankingService.java      # CLI menu/controller
└── web/
    └── WebBankingServer.java    # Local browser interface
```

## Requirements

- Java 17 or newer is recommended
- A terminal or command prompt
- A browser for web mode

The project uses plain Java only. No Maven, Gradle, Node.js, or external dependencies are required.

## Compile

From the repository root:

```bash
javac BankingApp.java WebBankingApp.java service/*.java model/*.java exception/*.java web/*.java
```

## Run CLI mode

```bash
java BankingApp
```

## Run web mode

```bash
java WebBankingApp
```

Then open:

```text
http://localhost:8080
```

You can also choose a custom port:

```bash
java WebBankingApp 9090
```

Then open:

```text
http://localhost:9090
```

## CLI example session

```text
============================
 BankingApp - Console Bank
============================
Demo users: Alice, Imran, Valarie, Denzel
Web mode: java WebBankingApp

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

## Web interface features

The web interface includes:

- Active user selector
- Balance cards
- Fund balance overview
- Deposit form
- Withdraw form
- Send money form
- Savings-to-investment transfer
- Investment-to-savings transfer
- Fund investment form
- Withdraw-all-investments action
- Monthly interest/growth action
- Transaction history

The web app is intentionally local-first and dependency-free. It is not meant for public internet deployment yet.

## Important design notes

This project is a learning/demo banking app, not a real banking system.

Current behavior:

- Data is stored in memory only.
- CLI mode and web mode each start their own in-memory bank session.
- There is no password authentication yet.
- There is no database yet.
- There is no HTTPS/security layer yet.
- Interest and fund growth only apply when the user chooses the monthly growth option.
- Money is handled with `BigDecimal` for safer decimal arithmetic.
- Transactions are stored only for the current runtime session.

## Recent upgrades

The project was upgraded from a fragile demo into a cleaner dual-interface application:

- Added a proper README with setup and usage instructions
- Added `.gitignore`
- Added CLI and web access modes
- Added a shared `BankingCore` so CLI and web use the same banking logic
- Made login/user selection case-insensitive
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
- Add Maven or Gradle build scripts
- Add API endpoints that return JSON
- Add transaction timestamps
- Add admin user management
- Add a production-ready web framework later, such as Spring Boot, only after the core is stable

## License

No license has been added yet. Add one before using this as an open-source project.
