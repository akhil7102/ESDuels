package fun.endcore.esduels;

import fun.endcore.esduels.arena.ArenaManager;
import fun.endcore.esduels.arena.restore.ArenaRestorer;
import fun.endcore.esduels.arena.restore.SnapshotStorage;
import fun.endcore.esduels.backup.InventoryBackupManager;
import fun.endcore.esduels.command.DuelCommand;
import fun.endcore.esduels.command.TabCompleteHelper;
import fun.endcore.esduels.config.ConfigManager;
import fun.endcore.esduels.duel.DuelManager;
import fun.endcore.esduels.duel.DuelRequestManager;
import fun.endcore.esduels.gui.GUIListener;
import fun.endcore.esduels.listener.ArenaProtectionListener;
import fun.endcore.esduels.listener.DuelCombatListener;
import fun.endcore.esduels.listener.DuelConnectionListener;
import fun.endcore.esduels.listener.RespawnListener;
import fun.endcore.esduels.loot.LootTimerManager;
import fun.endcore.esduels.music.MusicManager;
import fun.endcore.esduels.placeholder.ESDuelsExpansion;
import fun.endcore.esduels.queue.QueueManager;
import fun.endcore.esduels.spawn.SpawnManager;
import fun.endcore.esduels.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ESDuels v1.0 - Core Plugin Class
 * High-performance, lightweight DonutSMP-style duels plugin.
 */
public class ESDuels extends JavaPlugin {
    private static ESDuels instance;

    private ConfigManager configManager;
    private SpawnManager spawnManager;
    private StatsManager statsManager;
    private SnapshotStorage snapshotStorage;
    private ArenaRestorer arenaRestorer;
    private ArenaManager arenaManager;
    private MusicManager musicManager;
    private InventoryBackupManager backupManager;
    private DuelRequestManager duelRequestManager;
    private LootTimerManager lootTimerManager;
    private DuelManager duelManager;
    private QueueManager queueManager;
    private GUIListener guiListener;
    private fun.endcore.esduels.duel.CombatLogManager combatLogManager;

    @Override
    public void onEnable() {
        instance = this;

        long startTime = System.currentTimeMillis();
        getLogger().info("=========================================");
        getLogger().info("       ESDuels v1.0 - Starting Up         ");
        getLogger().info("       EndCore Studios - Production       ");
        getLogger().info("=========================================");

        // 1. Core configurations and spawn
        this.configManager = new ConfigManager(this);
        this.spawnManager = new SpawnManager(this);

        // 2. Data persistence and restoration engines
        this.statsManager = new StatsManager(this);
        this.snapshotStorage = new SnapshotStorage(this);
        this.arenaRestorer = new ArenaRestorer(this);
        this.arenaManager = new ArenaManager(this);

        // 3. Audio and backups
        this.musicManager = new MusicManager(this);
        this.backupManager = new InventoryBackupManager(this);

        // 4. Duel lifecycle managers
        this.duelRequestManager = new DuelRequestManager(this);
        this.lootTimerManager = new LootTimerManager(this);
        this.combatLogManager = new fun.endcore.esduels.duel.CombatLogManager(this);
        this.duelManager = new DuelManager(this);
        this.queueManager = new QueueManager(this);

        // 5. Register event listeners
        this.guiListener = new GUIListener(this);
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(new DuelCombatListener(this), this);
        getServer().getPluginManager().registerEvents(new DuelConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new ArenaProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new RespawnListener(this), this);

        // 6. Register commands and tab completion
        DuelCommand duelCommand = new DuelCommand(this);
        TabCompleteHelper tabHelper = new TabCompleteHelper(this);

        PluginCommand duelCmd = getCommand("duel");
        if (duelCmd != null) {
            duelCmd.setExecutor(duelCommand);
            duelCmd.setTabCompleter(tabHelper);
        }

        // 7. Optional PlaceholderAPI hook
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new ESDuelsExpansion(this).register();
            getLogger().info("Hooked into PlaceholderAPI successfully!");
        }

        long loadDuration = System.currentTimeMillis() - startTime;
        getLogger().info("ESDuels v1.0 enabled successfully in " + loadDuration + "ms!");
        getLogger().info("=========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling ESDuels v1.0...");

        if (guiListener != null) {
            guiListener.shutdown();
        }
        if (queueManager != null) {
            queueManager.shutdown();
        }
        if (duelRequestManager != null) {
            duelRequestManager.shutdown();
        }
        if (musicManager != null) {
            musicManager.clearAll();
        }
        if (lootTimerManager != null) {
            lootTimerManager.cancelAll();
        }
        if (duelManager != null) {
            duelManager.cancelAll();
        }
        if (statsManager != null) {
            statsManager.saveAll();
        }

        getLogger().info("ESDuels v1.0 disabled cleanly.");
    }

    public static ESDuels getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public SnapshotStorage getSnapshotStorage() {
        return snapshotStorage;
    }

    public ArenaRestorer getArenaRestorer() {
        return arenaRestorer;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public MusicManager getMusicManager() {
        return musicManager;
    }

    public InventoryBackupManager getBackupManager() {
        return backupManager;
    }

    public DuelRequestManager getDuelRequestManager() {
        return duelRequestManager;
    }

    public LootTimerManager getLootTimerManager() {
        return lootTimerManager;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public fun.endcore.esduels.duel.CombatLogManager getCombatLogManager() {
        return combatLogManager;
    }
}
