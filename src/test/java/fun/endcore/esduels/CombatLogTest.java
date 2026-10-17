package fun.endcore.esduels;

import fun.endcore.esduels.duel.CombatLogManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class CombatLogTest {

    @Test
    public void testCombatLogPersistence(@TempDir File tempDir) {
        Logger logger = Logger.getLogger("CombatLogTest");
        CombatLogManager manager = new CombatLogManager(tempDir, logger);

        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();

        assertFalse(manager.hasCombatLogger(player1));
        assertFalse(manager.hasCombatLogger(player2));

        // Add combat logger
        manager.addCombatLogger(player1);
        assertTrue(manager.hasCombatLogger(player1));
        assertFalse(manager.hasCombatLogger(player2));

        manager.addCombatLogger(player2);
        assertTrue(manager.hasCombatLogger(player1));
        assertTrue(manager.hasCombatLogger(player2));

        // Simulate plugin reload or server reboot: new manager instance loads from same file
        CombatLogManager reloadedManager = new CombatLogManager(tempDir, logger);
        assertTrue(reloadedManager.hasCombatLogger(player1));
        assertTrue(reloadedManager.hasCombatLogger(player2));

        // Remove player 1
        reloadedManager.removeCombatLogger(player1);
        assertFalse(reloadedManager.hasCombatLogger(player1));
        assertTrue(reloadedManager.hasCombatLogger(player2));

        // Check file persistence again
        CombatLogManager thirdManager = new CombatLogManager(tempDir, logger);
        assertFalse(thirdManager.hasCombatLogger(player1));
        assertTrue(thirdManager.hasCombatLogger(player2));
    }
}
