package fun.endcore.esduels.stats;

import java.util.UUID;

/**
 * Encapsulates a player's duel statistics.
 */
public class PlayerStats {
    private final UUID uuid;
    private int wins;
    private int losses;
    private int streak;
    private int bestStreak;

    public PlayerStats(UUID uuid, int wins, int losses, int streak, int bestStreak) {
        this.uuid = uuid;
        this.wins = wins;
        this.losses = losses;
        this.streak = streak;
        this.bestStreak = bestStreak;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getWins() {
        return wins;
    }

    public int getLosses() {
        return losses;
    }

    public int getStreak() {
        return streak;
    }

    public int getBestStreak() {
        return bestStreak;
    }

    public void addWin() {
        this.wins++;
        this.streak++;
        if (this.streak > this.bestStreak) {
            this.bestStreak = this.streak;
        }
    }

    public void addLoss() {
        this.losses++;
        this.streak = 0;
    }

    public double getWinRate() {
        int total = wins + losses;
        if (total == 0) return 0.0;
        return (double) wins / total * 100.0;
    }
}
