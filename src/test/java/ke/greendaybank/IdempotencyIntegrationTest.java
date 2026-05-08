package ke.greendaybank;

import ke.greendaybank.idempotency.IdempotentOperationRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyIntegrationTest extends PostgresIntegrationBase {
    @Autowired
    IdempotentOperationRunner runner;

    @Test
    void sameRequestReplaysOriginalResponseWithoutRunningTwice() {
        AtomicInteger executions = new AtomicInteger(0);
        String key = "IK_REPLAY_TEST_000000000000001";
        String canonical = "operation|same-body";

        String first = runner.run("TEST_OPERATION", key, canonical, () -> {
            executions.incrementAndGet();
            return "{\"ok\":true,\"ref\":\"FIRST\"}";
        }).getBody();

        String second = runner.run("TEST_OPERATION", key, canonical, () -> {
            executions.incrementAndGet();
            return "{\"ok\":true,\"ref\":\"SECOND\"}";
        }).getBody();

        assertEquals("{\"ok\":true,\"ref\":\"FIRST\"}", first);
        assertEquals(first, second);
        assertEquals(1, executions.get());
    }

    @Test
    void sameKeyDifferentBodyIsRejected() {
        String key = "IK_REPLAY_TEST_000000000000002";
        runner.run("TEST_OPERATION_2", key, "operation|body-a", () -> "{\"ok\":true}");

        assertThrows(IllegalArgumentException.class, () -> runner.run("TEST_OPERATION_2", key, "operation|body-b", () -> "{\"ok\":false}"));
    }
}
