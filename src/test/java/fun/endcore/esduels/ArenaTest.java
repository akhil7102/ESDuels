package fun.endcore.esduels;

import fun.endcore.esduels.arena.Arena;
import fun.endcore.esduels.arena.ArenaManager;
import fun.endcore.esduels.arena.ArenaState;
import fun.endcore.esduels.arena.Cuboid;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ArenaTest {

    @Test
    public void testArenaNameValidation() {
        // Valid arena names
        assertTrue(ArenaManager.isValidArenaName("arena1"));
        assertTrue(ArenaManager.isValidArenaName("forest_duel"));
        assertTrue(ArenaManager.isValidArenaName("map-123"));
        assertTrue(ArenaManager.isValidArenaName("PVP_ARENA"));
        assertTrue(ArenaManager.isValidArenaName("a"));

        // Invalid arena names / Path traversal attempts
        assertFalse(ArenaManager.isValidArenaName(null));
        assertFalse(ArenaManager.isValidArenaName(""));
        assertFalse(ArenaManager.isValidArenaName("../traversal"));
        assertFalse(ArenaManager.isValidArenaName("..\\traversal"));
        assertFalse(ArenaManager.isValidArenaName("arena/test"));
        assertFalse(ArenaManager.isValidArenaName("arena\\test"));
        assertFalse(ArenaManager.isValidArenaName("arena name"));
        assertFalse(ArenaManager.isValidArenaName("arena!@#$"));
        assertFalse(ArenaManager.isValidArenaName("this_arena_name_is_way_too_long_for_the_system_limit"));
    }

    @Test
    public void testArenaInitialStateIsIncomplete() {
        Arena arena = new Arena("test_arena");
        assertEquals(ArenaState.INCOMPLETE, arena.getState());
        assertFalse(arena.isConfigured());
        assertFalse(arena.isAvailable());
    }

    @Test
    public void testCuboidMath() {
        Cuboid cuboid = new Cuboid("world", 0, 10, 0, 10, 20, 10);
        assertEquals(11 * 11 * 11, cuboid.getVolume());
        assertEquals(0, cuboid.getMinX());
        assertEquals(10, cuboid.getMaxX());
        assertEquals(10, cuboid.getMinY());
        assertEquals(20, cuboid.getMaxY());
        assertEquals(0, cuboid.getMinZ());
        assertEquals(10, cuboid.getMaxZ());
    }
}
