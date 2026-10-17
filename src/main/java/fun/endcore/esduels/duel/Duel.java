package fun.endcore.esduels.duel;

import fun.endcore.esduels.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Encapsulates an active duel match between two players in an arena.
 */
public class Duel {
    private final Arena arena;
    private final UUID player1Uuid;
    private final String player1Name;
    private final UUID player2Uuid;
    private final String player2Name;
    private DuelState state = DuelState.COUNTDOWN;
    private UUID winnerUuid;
    private UUID loserUuid;
    private final long startTime;
    private org.bukkit.scheduler.BukkitTask countdownTask;

    public Duel(Arena arena, Player player1, Player player2) {
        this.arena = arena;
        this.player1Uuid = player1.getUniqueId();
        this.player1Name = player1.getName();
        this.player2Uuid = player2.getUniqueId();
        this.player2Name = player2.getName();
        this.startTime = System.currentTimeMillis();
    }

    public Arena getArena() {
        return arena;
    }

    public UUID getPlayer1Uuid() {
        return player1Uuid;
    }

    public String getPlayer1Name() {
        return player1Name;
    }

    public UUID getPlayer2Uuid() {
        return player2Uuid;
    }

    public String getPlayer2Name() {
        return player2Name;
    }

    public Player getPlayer1() {
        return Bukkit.getPlayer(player1Uuid);
    }

    public Player getPlayer2() {
        return Bukkit.getPlayer(player2Uuid);
    }

    public DuelState getState() {
        return state;
    }

    public void setState(DuelState state) {
        this.state = state;
    }

    public UUID getWinnerUuid() {
        return winnerUuid;
    }

    public void setWinnerUuid(UUID winnerUuid) {
        this.winnerUuid = winnerUuid;
    }

    public UUID getLoserUuid() {
        return loserUuid;
    }

    public void setLoserUuid(UUID loserUuid) {
        this.loserUuid = loserUuid;
    }

    public long getStartTime() {
        return startTime;
    }

    public boolean containsPlayer(UUID uuid) {
        return player1Uuid.equals(uuid) || player2Uuid.equals(uuid);
    }

    public UUID getOpponentUuid(UUID uuid) {
        if (player1Uuid.equals(uuid)) return player2Uuid;
        if (player2Uuid.equals(uuid)) return player1Uuid;
        return null;
    }

    public Player getOpponent(UUID uuid) {
        UUID opp = getOpponentUuid(uuid);
        return opp != null ? Bukkit.getPlayer(opp) : null;
    }

    public org.bukkit.scheduler.BukkitTask getCountdownTask() {
        return countdownTask;
    }

    public void setCountdownTask(org.bukkit.scheduler.BukkitTask countdownTask) {
        this.countdownTask = countdownTask;
    }
}
