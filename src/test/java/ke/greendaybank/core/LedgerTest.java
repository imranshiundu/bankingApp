package ke.greendaybank.core;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;

class LedgerTest {
    @Test
    void transferMovesValueWithoutCreatingMoney() {
        Ledger ledger = new Ledger(Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
        ledger.registerAccount("GL-SUSPENSE-KES", Money.KES);
        ledger.registerAccount("A001", Money.KES);
        ledger.registerAccount("A002", Money.KES);

        ledger.postExternalCredit("A001", Money.kes("1000.00"), "GL-SUSPENSE-KES", "Initial deposit");
        ledger.postTransfer("A001", "A002", Money.kes("250.00"), "Customer transfer");

        assertEquals(Money.kes("750.00"), ledger.balanceOf("A001", Money.KES));
        assertEquals(Money.kes("250.00"), ledger.balanceOf("A002", Money.KES));
        assertEquals(4, ledger.entries().size());
    }

    @Test
    void rejectsInsufficientFunds() {
        Ledger ledger = new Ledger(Clock.systemUTC());
        ledger.registerAccount("A001", Money.KES);
        ledger.registerAccount("A002", Money.KES);

        assertThrows(IllegalStateException.class, () -> ledger.postTransfer("A001", "A002", Money.kes("1.00"), "Bad transfer"));
    }

    @Test
    void rejectsCurrencyMismatch() {
        Ledger ledger = new Ledger(Clock.systemUTC());
        ledger.registerAccount("A001", Money.KES);
        ledger.registerAccount("A002", Currency.getInstance("USD"));

        assertThrows(IllegalArgumentException.class, () -> ledger.postTransfer("A001", "A002", Money.kes("1.00"), "Bad currency"));
    }
}
