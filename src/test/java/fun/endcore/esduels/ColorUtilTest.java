package fun.endcore.esduels;

import fun.endcore.esduels.stats.PlayerStats;
import fun.endcore.esduels.util.ColorUtil;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ColorUtilTest {

    @Test
    public void testLegacyColorTranslation() {
        String input = "&aHello &cWorld";
        String output = ColorUtil.colorize(input);
        assertTrue(output.contains("§aHello"));
        assertTrue(output.contains("§cWorld"));
    }

    @Test
    public void testHexColorTranslation() {
        String input = "&#33A2FFEndCore";
        String output = ColorUtil.colorize(input);
        assertTrue(output.startsWith("§x§3§3§a§2§f§f") || output.startsWith("§x§3§3§A§2§F§F"));
    }

    @Test
    public void testPlayerStatsCalculation() {
        UUID uuid = UUID.randomUUID();
        PlayerStats stats = new PlayerStats(uuid, 0, 0, 0, 0);

        stats.addWin();
        stats.addWin();
        assertEquals(2, stats.getWins());
        assertEquals(2, stats.getStreak());
        assertEquals(2, stats.getBestStreak());

        stats.addLoss();
        assertEquals(1, stats.getLosses());
        assertEquals(0, stats.getStreak());
        assertEquals(2, stats.getBestStreak());

        double winRate = stats.getWinRate();
        assertEquals(66.66, winRate, 0.1);
    }
}
