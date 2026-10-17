package fun.endcore.esduels.queue;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Represents a player waiting in the matchmaking queue.
 */
public class QueueEntry {
    private final UUID uuid;
    private final String playerName;
    private final long joinedTime;

    public QueueEntry(Player player) {
        this.uuid = player.getUniqueId();
        this.playerName = player.getName();
        this.joinedTime = System.currentTimeMillis();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getJoinedTime() {
        return joinedTime;
    }
}
