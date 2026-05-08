package ke.greendaybank.api;

import ke.greendaybank.repository.CustomerRepository;
import ke.greendaybank.service.BankingApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class BankingApiController {
    private final BankingApplicationService service;

    public BankingApiController(BankingApplicationService service) {
        this.service = service;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountOpenedResponse> openAccount(@RequestBody @Valid OpenAccountRequest request) {
        CustomerRepository.CustomerAccount account = service.openAccount(request.privateProfile(), actor(request.actor()));
        return ResponseEntity.status(HttpStatus.CREATED).body(new AccountOpenedResponse(
                account.customerRef(),
                account.accountNumber(),
                "PENDING_KYC"
        ));
    }

    @PostMapping("/ledger/credits")
    public ResponseEntity<TransactionResponse> credit(@RequestBody @Valid CreditRequest request) {
        String transactionRef = service.credit(
                request.accountNumber(),
                request.amount(),
                request.idempotencyKey(),
                request.narration(),
                actor(request.actor())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new TransactionResponse(transactionRef, "POSTED"));
    }

    @PostMapping("/ledger/movements")
    public ResponseEntity<TransactionResponse> move(@RequestBody @Valid MovementRequest request) {
        String transactionRef = service.move(
                request.fromAccountNumber(),
                request.toAccountNumber(),
                request.amount(),
                request.idempotencyKey(),
                request.narration(),
                actor(request.actor())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(new TransactionResponse(transactionRef, "POSTED"));
    }

    @GetMapping("/accounts/{accountNumber}/balance")
    public BalanceResponse balance(@PathVariable String accountNumber) {
        return new BalanceResponse(accountNumber, service.balance(accountNumber), "KES");
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "greenday-banking-core");
    }

    private String actor(String incoming) {
        return incoming == null || incoming.isBlank() ? "system-api" : incoming.trim();
    }

    public record OpenAccountRequest(Map<String, String> privateProfile, String actor) {}

    public record CreditRequest(
            @NotBlank String accountNumber,
            @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank String narration,
            String idempotencyKey,
            String actor
    ) {}

    public record MovementRequest(
            @NotBlank String fromAccountNumber,
            @NotBlank String toAccountNumber,
            @DecimalMin(value = "0.01") BigDecimal amount,
            @NotBlank String narration,
            String idempotencyKey,
            String actor
    ) {}

    public record AccountOpenedResponse(String customerRef, String accountNumber, String status) {}

    public record TransactionResponse(String transactionRef, String status) {}

    public record BalanceResponse(String accountNumber, BigDecimal balance, String currency) {}
}
