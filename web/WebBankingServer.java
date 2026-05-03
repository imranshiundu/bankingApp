package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import exception.InvalidAmountException;
import model.Fund;
import model.User;
import service.BankingCore;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WebBankingServer {
    private final BankingCore bankingCore;
    private final HttpServer server;

    public WebBankingServer(BankingCore bankingCore, int port) throws IOException {
        this.bankingCore = bankingCore;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        configureRoutes();
    }

    public void start() {
        server.start();
        System.out.println("BankingApp web interface running at http://localhost:" + server.getAddress().getPort());
    }

    private void configureRoutes() {
        server.createContext("/", this::handleHome);
        server.createContext("/action", this::handleAction);
    }

    private void handleHome(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            send(exchange, 405, "Method not allowed", "text/plain");
            return;
        }

        Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
        String selectedUser = query.getOrDefault("user", "Imran");
        Optional<User> user = bankingCore.findUser(selectedUser);

        if (user.isEmpty()) {
            send(exchange, 404, page("User not found", "<p>User not found.</p>"), "text/html");
            return;
        }

        String notice = query.getOrDefault("notice", "");
        send(exchange, 200, dashboard(user.get(), notice), "text/html");
    }

    private void handleAction(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            send(exchange, 405, "Method not allowed", "text/plain");
            return;
        }

        Map<String, String> form = parseForm(exchange);
        String userName = form.getOrDefault("user", "Imran");
        String action = form.getOrDefault("action", "");
        Optional<User> user = bankingCore.findUser(userName);

        if (user.isEmpty()) {
            redirect(exchange, "/?notice=User+not+found");
            return;
        }

        String notice;
        try {
            notice = runAction(action, user.get(), form);
        } catch (InvalidAmountException | IllegalArgumentException e) {
            notice = e.getMessage();
        }

        redirect(exchange, "/?user=" + encode(user.get().getName()) + "&notice=" + encode(notice));
    }

    private String runAction(String action, User user, Map<String, String> form) throws InvalidAmountException {
        switch (action) {
            case "deposit" -> {
                BigDecimal amount = bankingCore.parseAmount(form.get("amount"));
                bankingCore.depositSavings(user, amount);
                return "Deposit successful.";
            }
            case "withdraw" -> {
                BigDecimal amount = bankingCore.parseAmount(form.get("amount"));
                bankingCore.withdrawSavings(user, amount);
                return "Withdrawal successful.";
            }
            case "send" -> {
                Optional<User> recipient = bankingCore.findUser(form.get("recipient"));
                if (recipient.isEmpty()) {
                    throw new InvalidAmountException("Recipient not found.");
                }
                BigDecimal amount = bankingCore.parseAmount(form.get("amount"));
                bankingCore.sendMoney(user, recipient.get(), amount);
                return "Money sent successfully.";
            }
            case "savingsToInvestment" -> {
                BigDecimal amount = bankingCore.parseAmount(form.get("amount"));
                bankingCore.transferSavingsToInvestment(user, amount);
                return "Savings moved to investment wallet.";
            }
            case "investmentToSavings" -> {
                BigDecimal amount = bankingCore.parseAmount(form.get("amount"));
                bankingCore.transferInvestmentToSavings(user, amount);
                return "Investment wallet moved to savings.";
            }
            case "invest" -> {
                Fund fund = Fund.fromInput(form.getOrDefault("fund", ""));
                BigDecimal amount = bankingCore.parseAmount(form.get("amount"));
                bankingCore.investInFund(user, fund, amount);
                return "Investment placed successfully.";
            }
            case "withdrawInvestments" -> {
                bankingCore.withdrawAllInvestments(user);
                return "All fund investments moved to investment wallet.";
            }
            case "growth" -> {
                bankingCore.applyMonthlyGrowth(user);
                return "Monthly interest and fund growth applied.";
            }
            default -> throw new InvalidAmountException("Unknown action.");
        }
    }

    private String dashboard(User user, String notice) {
        StringBuilder body = new StringBuilder();
        body.append("<section class='hero'>")
            .append("<div><p class='eyebrow'>Console + Web Banking</p><h1>BankingApp</h1><p>Use the same Java banking engine from the terminal or this local web interface.</p></div>")
            .append(userSelector(user))
            .append("</section>");

        if (!notice.isBlank()) {
            body.append("<div class='notice'>").append(escape(notice)).append("</div>");
        }

        body.append(balanceCards(user));
        body.append(actions(user));
        body.append(transactions(user));
        return page("BankingApp", body.toString());
    }

    private String userSelector(User selected) {
        StringBuilder html = new StringBuilder("<form class='user-switcher' method='get' action='/'>");
        html.append("<label>Active user</label><select name='user' onchange='this.form.submit()'>");
        for (User user : bankingCore.getUsers()) {
            html.append("<option value='").append(escape(user.getName())).append("'");
            if (user.getName().equalsIgnoreCase(selected.getName())) {
                html.append(" selected");
            }
            html.append(">").append(escape(user.getName())).append("</option>");
        }
        html.append("</select></form>");
        return html.toString();
    }

    private String balanceCards(User user) {
        StringBuilder html = new StringBuilder("<section class='grid cards'>");
        html.append(card("Cash", user.getCash()));
        html.append(card("Savings", user.getSavingsAccount().getBalance()));
        html.append(card("Investment Wallet", user.getInvestmentAccount().getBalance()));
        html.append(card("Invested Funds", user.getInvestmentAccount().getTotalInvested()));
        html.append("</section><section class='panel'><h2>Funds</h2><div class='fund-list'>");
        for (Fund fund : Fund.values()) {
            html.append("<div><strong>").append(fund.getDisplayName()).append("</strong><span>$")
                .append(bankingCore.format(user.getInvestmentAccount().getInvestments().get(fund))).append("</span></div>");
        }
        html.append("</div></section>");
        return html.toString();
    }

    private String card(String title, BigDecimal amount) {
        return "<article class='card'><span>" + escape(title) + "</span><strong>$" + bankingCore.format(amount) + "</strong></article>";
    }

    private String actions(User user) {
        return """
            <section class='grid actions'>
                %s
                %s
                %s
                %s
                %s
                %s
                %s
                %s
            </section>
            """.formatted(
                moneyForm(user, "deposit", "Deposit to savings", "Move available cash into savings"),
                moneyForm(user, "withdraw", "Withdraw to cash", "Move savings back to available cash"),
                sendForm(user),
                moneyForm(user, "savingsToInvestment", "Savings → Investment", "Move savings into investment wallet"),
                moneyForm(user, "investmentToSavings", "Investment → Savings", "Move wallet balance back to savings"),
                investForm(user),
                simpleForm(user, "withdrawInvestments", "Withdraw all fund investments", "Move all fund positions back to investment wallet"),
                simpleForm(user, "growth", "Apply monthly growth", "Apply savings interest and demo fund growth")
            );
    }

    private String moneyForm(User user, String action, String title, String description) {
        return """
            <form class='panel' method='post' action='/action'>
                <h3>%s</h3><p>%s</p>
                <input type='hidden' name='user' value='%s'>
                <input type='hidden' name='action' value='%s'>
                <input name='amount' inputmode='decimal' placeholder='Amount' required>
                <button type='submit'>Submit</button>
            </form>
            """.formatted(escape(title), escape(description), escape(user.getName()), escape(action));
    }

    private String sendForm(User user) {
        StringBuilder recipients = new StringBuilder();
        for (User candidate : bankingCore.getUsers()) {
            if (!candidate.getName().equalsIgnoreCase(user.getName())) {
                recipients.append("<option value='").append(escape(candidate.getName())).append("'>")
                    .append(escape(candidate.getName())).append("</option>");
            }
        }
        return """
            <form class='panel' method='post' action='/action'>
                <h3>Send money</h3><p>Send from savings to another demo user.</p>
                <input type='hidden' name='user' value='%s'>
                <input type='hidden' name='action' value='send'>
                <select name='recipient'>%s</select>
                <input name='amount' inputmode='decimal' placeholder='Amount' required>
                <button type='submit'>Send</button>
            </form>
            """.formatted(escape(user.getName()), recipients);
    }

    private String investForm(User user) {
        StringBuilder funds = new StringBuilder();
        for (Fund fund : Fund.values()) {
            funds.append("<option value='").append(fund.name()).append("'>")
                .append(fund.getDisplayName()).append("</option>");
        }
        return """
            <form class='panel' method='post' action='/action'>
                <h3>Invest in fund</h3><p>Invest available wallet balance into a demo fund.</p>
                <input type='hidden' name='user' value='%s'>
                <input type='hidden' name='action' value='invest'>
                <select name='fund'>%s</select>
                <input name='amount' inputmode='decimal' placeholder='Amount' required>
                <button type='submit'>Invest</button>
            </form>
            """.formatted(escape(user.getName()), funds);
    }

    private String simpleForm(User user, String action, String title, String description) {
        return """
            <form class='panel' method='post' action='/action'>
                <h3>%s</h3><p>%s</p>
                <input type='hidden' name='user' value='%s'>
                <input type='hidden' name='action' value='%s'>
                <button type='submit'>Run action</button>
            </form>
            """.formatted(escape(title), escape(description), escape(user.getName()), escape(action));
    }

    private String transactions(User user) {
        StringBuilder html = new StringBuilder("<section class='panel'><h2>Transaction history</h2><ul class='transactions'>");
        List<String> transactions = bankingCore.transactionHistory(user);
        for (int index = transactions.size() - 1; index >= 0; index--) {
            html.append("<li>").append(escape(transactions.get(index))).append("</li>");
        }
        html.append("</ul></section>");
        return html.toString();
    }

    private String page(String title, String body) {
        return """
            <!doctype html>
            <html lang='en'>
            <head>
                <meta charset='utf-8'>
                <meta name='viewport' content='width=device-width, initial-scale=1'>
                <title>%s</title>
                <style>
                    :root { --bg: #0f172a; --panel: #111827; --muted: #94a3b8; --text: #f8fafc; --line: #243244; --accent: #38bdf8; }
                    * { box-sizing: border-box; }
                    body { margin: 0; font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: var(--bg); color: var(--text); }
                    main { width: min(1180px, calc(100%% - 32px)); margin: 0 auto; padding: 32px 0 48px; }
                    .hero { display: flex; justify-content: space-between; gap: 24px; align-items: end; margin-bottom: 20px; }
                    .eyebrow { color: var(--accent); text-transform: uppercase; letter-spacing: .12em; font-size: 12px; font-weight: 700; }
                    h1 { font-size: clamp(36px, 7vw, 72px); margin: 0; line-height: .95; }
                    h2, h3, p { margin-top: 0; }
                    p { color: var(--muted); }
                    .grid { display: grid; gap: 16px; }
                    .cards { grid-template-columns: repeat(4, minmax(0, 1fr)); margin-bottom: 16px; }
                    .actions { grid-template-columns: repeat(4, minmax(0, 1fr)); }
                    .card, .panel, .notice, .user-switcher { border: 1px solid var(--line); background: rgba(17, 24, 39, .9); border-radius: 18px; padding: 18px; box-shadow: 0 20px 60px rgba(0,0,0,.18); }
                    .card span { display: block; color: var(--muted); font-size: 13px; margin-bottom: 12px; }
                    .card strong { display: block; font-size: 28px; }
                    .notice { margin: 0 0 16px; border-color: rgba(56,189,248,.5); }
                    input, select, button { width: 100%%; border-radius: 12px; border: 1px solid var(--line); padding: 12px 14px; background: #0b1220; color: var(--text); margin-top: 10px; }
                    button { cursor: pointer; background: var(--accent); color: #06111a; border: none; font-weight: 800; }
                    .fund-list { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; }
                    .fund-list div { display: flex; justify-content: space-between; border: 1px solid var(--line); border-radius: 14px; padding: 14px; }
                    .transactions { padding-left: 18px; color: var(--muted); }
                    .transactions li { margin: 8px 0; }
                    @media (max-width: 900px) { .cards, .actions, .fund-list { grid-template-columns: 1fr 1fr; } .hero { align-items: stretch; flex-direction: column; } }
                    @media (max-width: 620px) { .cards, .actions, .fund-list { grid-template-columns: 1fr; } main { width: min(100%% - 20px, 1180px); padding-top: 20px; } }
                </style>
            </head>
            <body><main>%s</main></body>
            </html>
            """.formatted(escape(title), body);
    }

    private Map<String, String> parseForm(HttpExchange exchange) throws IOException {
        String rawBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return parseQuery(rawBody);
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> values = new HashMap<>();
        if (query == null || query.isBlank()) {
            return values;
        }

        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            String key = decode(parts[0]);
            String value = parts.length > 1 ? decode(parts[1]) : "";
            values.put(key, value);
        }
        return values;
    }

    private void redirect(HttpExchange exchange, String location) throws IOException {
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(303, -1);
        exchange.close();
    }

    private void send(HttpExchange exchange, int status, String body, String contentType) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
