package fun.endcore.esduels;

import fun.endcore.esduels.duel.DuelRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DuelRequestTest {

    @Test
    public void testDuelRequestCreationAndExpiration() throws InterruptedException {
        UUID sender = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        DuelRequest req = new DuelRequest(sender, target);
        assertEquals(sender, req.getSender());
        assertEquals(target, req.getTarget());
        assertTrue(req.getCreatedAt() <= System.currentTimeMillis());

        // Test not expired immediately
        assertFalse(req.isExpired(60));

        // Test expired with 0 seconds expiration
        Thread.sleep(10);
        assertTrue(req.isExpired(0));
    }
}
