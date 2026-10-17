package fun.endcore.esduels;

import fun.endcore.esduels.stats.PlayerStats;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerStatsTest {

    @Test
    public void testStatsWinAndStreakTracking() {
        UUID uuid = UUID.randomUUID();
        PlayerStats stats = new PlayerStats(uuid, 0, 0, 0, 0);

        assertEquals(0, stats.getWins());
        assertEquals(0, stats.getLosses());
        assertEquals(0, stats.getStreak());
        assertEquals(0, stats.getBestStreak());
        assertEquals(0.0, stats.getWinRate(), 0.001);

        stats.addWin();
        assertEquals(1, stats.getWins());
        assertEquals(1, stats.getStreak());
        assertEquals(1, stats.getBestStreak());
        assertEquals(100.0, stats.getWinRate(), 0.001);

        stats.addWin();
        stats.addWin();
        assertEquals(3, stats.getWins());
        assertEquals(3, stats.getStreak());
        assertEquals(3, stats.getBestStreak());

        stats.addLoss();
        assertEquals(3, stats.getWins());
        assertEquals(1, stats.getLosses());
        assertEquals(0, stats.getStreak(), "Streak must reset to 0 after loss");
        assertEquals(3, stats.getBestStreak(), "Best streak must be preserved");
        assertEquals(75.0, stats.getWinRate(), 0.001);
    }
}
