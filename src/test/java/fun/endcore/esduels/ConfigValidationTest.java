package fun.endcore.esduels;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigValidationTest {

    @Test
    public void testConfigDefaults() {
        File configFile = new File("src/main/resources/config.yml");
        assertTrue(configFile.exists(), "config.yml must exist in src/main/resources");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Requirement: Ensure default loot timer is 60 seconds
        assertEquals(60, config.getInt("loot-timer-seconds"), "Default loot timer MUST be 60 seconds");
        assertEquals(60, config.getInt("request-expiration-seconds"));
        assertEquals(5, config.getInt("countdown-seconds"));

        // Matchmaking queue defaults
        assertEquals(20, config.getInt("queue.refresh-interval-ticks"));
        assertNotNull(config.getString("queue.region-name"));
        assertNotNull(config.getString("queue.default-estimated-wait"));

        // Music settings
        assertTrue(config.getBoolean("queue-music.enabled"));
        assertFalse(config.getStringList("queue-music.tracks").isEmpty());
        assertTrue(config.getBoolean("loot-music.enabled"));
        assertFalse(config.getStringList("loot-music.tracks").isEmpty());

        // Restoration settings
        assertEquals(250, config.getInt("restoration.batch-size"));
    }

    @Test
    public void testMessagesDefaults() {
        File messagesFile = new File("src/main/resources/messages.yml");
        assertTrue(messagesFile.exists(), "messages.yml must exist in src/main/resources");

        YamlConfiguration messages = YamlConfiguration.loadConfiguration(messagesFile);

        assertNotNull(messages.getString("prefix"));
        assertNotNull(messages.getString("general.no-permission"));
        assertNotNull(messages.getString("general.player-not-found"));
        assertNotNull(messages.getString("queue.joined"));
        assertNotNull(messages.getString("queue.left"));
        assertNotNull(messages.getString("duel.match-found"));
        assertNotNull(messages.getString("duel.victory-title"));
        assertNotNull(messages.getString("duel.defeat-title"));
        assertNotNull(messages.getString("duel.draw"), "duel.draw message must exist");
        assertNotNull(messages.getString("duel.leave-duel"));
        assertNotNull(messages.getString("duel.combat-logged"), "duel.combat-logged message must exist");
        assertNotNull(messages.getString("loot.ended"));
        assertNotNull(messages.getString("loot.loser-teleported"));
        assertNotNull(messages.getString("spawn.set"));
        assertNotNull(messages.getString("arena.saved"));
    }
}
