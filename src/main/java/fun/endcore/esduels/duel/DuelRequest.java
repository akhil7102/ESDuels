package fun.endcore.esduels.duel;

import java.util.UUID;

/**
 * Encapsulates a direct duel challenge from one player to another.
 */
public class DuelRequest {
    private final UUID sender;
    private final UUID target;
    private final long createdAt;

    public DuelRequest(UUID sender, UUID target) {
        this.sender = sender;
        this.target = target;
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getTarget() {
        return target;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired(long expirationSeconds) {
        return System.currentTimeMillis() - createdAt > (expirationSeconds * 1000L);
    }
}
