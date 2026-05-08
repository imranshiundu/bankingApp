package ke.greendaybank.api;

import ke.greendaybank.approval.ApprovalPolicy;
import ke.greendaybank.idempotency.IdempotentOperationRunner;
import ke.greendaybank.repository.CustomerRepository;
import ke.greendaybank.repository.StatementRepository;
import ke.greendaybank.service.BankingApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class BankingApiController {
    private final BankingApplicationService service;
    private final IdempotentOperationRunner idempotentRunner;
    private final ApprovalPolicy approvalPolicy;

    public BankingApiController(BankingApplicationService service, IdempotentOperationRunner idempotentRunner, ApprovalPolicy approvalPolicy) {
        this.service = service;
        this.idempotentRunner = idempotentRunner;
        this.approvalPolicy = approvalPolicy;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountOpenedResponse> openAccount(@RequestBody @Valid OpenAccountRequest request) {
        CustomerRepository.CustomerAccount account = service.openAccount(request.privateProfile(), actor(request.actor()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new AccountOpenedResponse(account.customerRef(), account.accountNumber(), "PENDING_KYC"));
    }

    @PostMapping(value = "/ledger/credits", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> credit(@RequestBody @Valid CreditRequest request) {
        String canonical = "credit|" + request.accountNumber() + "|" + request.amount() + "|" + request.narration();
        return idempotentRunner.run("LEDGER_CREDIT", request.idempotencyKey(), canonical, () -> {
            String transactionRef = service.credit(request.accountNumber(), request.amount(), request.idempotencyKey(), request.narration(), actor(request.actor()));
            return "{\"transactionRef\":\"" + escape(transactionRef) + "\",\"status\":\"POSTED\"}";
        });
    }

    @PostMapping(value = "/ledger/movements", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> move(@RequestBody @Valid MovementRequest request) {
        if (approvalPolicy.movementRequiresApproval(request.amount())) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).body("{\"status\":\"APPROVAL_REQUIRED\",\"thresholdKes\":\"" + approvalPolicy.highRiskKesThreshold() + "\"}");
        }
        String canonical = "movement|" + request.fromAccountNumber() + "|" + request.toAccountNumber() + "|" + request.amount() + "|" + request.narration();
        return idempotentRunner.run("LEDGER_MOVEMENT", request.idempotencyKey(), canonical, () -> {
            String transactionRef = service.move(request.fromAccountNumber(), request.toAccountNumber(), request.amount(), request.idempotencyKey(), request.narration(), actor(request.actor()));
            return "{\"transactionRef\":\"" + escape(transactionRef) + "\",\"status\":\"POSTED\"}";
        });
    }

    @GetMapping("/accounts/{accountNumber}/balance")
    public BalanceResponse balance(@PathVariable String accountNumber) {
        return new BalanceResponse(accountNumber, service.balance(accountNumber), "KES");
    }

    @GetMapping("/accounts/{accountNumber}/statement")
    public StatementResponse statement(@PathVariable String accountNumber, @RequestParam(defaultValue = "50") int limit) {
        return new StatementResponse(accountNumber, service.statement(accountNumber, limit));
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "greenday-banking-core");
    }

    private String actor(String incoming) {
        return incoming == null || incoming.isBlank() ? "system-api" : incoming.trim();
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record OpenAccountRequest(Map<String, String> privateProfile, String actor) {}
    public record CreditRequest(@NotBlank String accountNumber, @DecimalMin(value = "0.01") BigDecimal amount, @NotBlank String narration, @NotBlank String idempotencyKey, String actor) {}
    public record MovementRequest(@NotBlank String fromAccountNumber, @NotBlank String toAccountNumber, @DecimalMin(value = "0.01") BigDecimal amount, @NotBlank String narration, @NotBlank String idempotencyKey, String actor) {}
    public record AccountOpenedResponse(String customerRef, String accountNumber, String status) {}
    public record TransactionResponse(String transactionRef, String status) {}
    public record BalanceResponse(String accountNumber, BigDecimal balance, String currency) {}
    public record StatementResponse(String accountNumber, List<StatementRepository.StatementLine> lines) {}
}
