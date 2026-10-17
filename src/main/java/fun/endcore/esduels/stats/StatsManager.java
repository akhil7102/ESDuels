package fun.endcore.esduels.stats;

import fun.endcore.esduels.ESDuels;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages player statistics with memory caching and asynchronous YAML disk persistence.
 */
public class StatsManager {
    private final ESDuels plugin;
    private final File statsDir;
    private final Map<UUID, PlayerStats> statsCache = new ConcurrentHashMap<>();

    public StatsManager(ESDuels plugin) {
        this.plugin = plugin;
        this.statsDir = new File(plugin.getDataFolder(), "data" + File.separator + "stats");
        if (!statsDir.exists()) {
            statsDir.mkdirs();
        }
    }

    /**
     * Gets or loads PlayerStats from cache/disk.
     */
    public PlayerStats getStats(UUID uuid) {
        return statsCache.computeIfAbsent(uuid, this::loadFromDiskSync);
    }

    private PlayerStats loadFromDiskSync(UUID uuid) {
        File file = new File(statsDir, uuid.toString() + ".yml");
        if (!file.exists()) {
            return new PlayerStats(uuid, 0, 0, 0, 0);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        int wins = config.getInt("wins", 0);
        int losses = config.getInt("losses", 0);
        int streak = config.getInt("streak", 0);
        int bestStreak = config.getInt("best-streak", 0);
        return new PlayerStats(uuid, wins, losses, streak, bestStreak);
    }

    /**
     * Records a win for the winner and a loss for the loser.
     */
    public void recordDuelResult(UUID winnerUuid, UUID loserUuid) {
        PlayerStats winnerStats = getStats(winnerUuid);
        winnerStats.addWin();
        saveStatsAsync(winnerStats);

        PlayerStats loserStats = getStats(loserUuid);
        loserStats.addLoss();
        saveStatsAsync(loserStats);
    }

    /**
     * Asynchronously saves stats to disk to ensure zero main-thread stall.
     */
    public void saveStatsAsync(PlayerStats stats) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveStatsSync(stats));
    }

    public void saveStatsSync(PlayerStats stats) {
        File file = new File(statsDir, stats.getUuid().toString() + ".yml");
        YamlConfiguration config = new YamlConfiguration();
        config.set("wins", stats.getWins());
        config.set("losses", stats.getLosses());
        config.set("streak", stats.getStreak());
        config.set("best-streak", stats.getBestStreak());
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save stats for " + stats.getUuid(), e);
        }
    }

    /**
     * Flushes all cached stats to disk synchronously (used on plugin disable).
     */
    public void saveAll() {
        for (PlayerStats stats : statsCache.values()) {
            saveStatsSync(stats);
        }
    }

    public void unloadPlayer(UUID uuid) {
        PlayerStats stats = statsCache.remove(uuid);
        if (stats != null) {
            saveStatsAsync(stats);
        }
    }
}
