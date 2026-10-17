package fun.endcore.esduels.duel;

import fun.endcore.esduels.ESDuels;
import fun.endcore.esduels.util.MessageUtil;
import fun.endcore.esduels.util.SoundUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Persistently tracks players who disconnected during an active duel or arena countdown.
 * When these players reconnect, they are confirmed dead, stripped of all loot, and teleported to spawn.
 */
public class CombatLogManager {
    private ESDuels plugin;
    private final File file;
    private final java.util.logging.Logger logger;
    private final Set<UUID> combatLogDeaths = ConcurrentHashMap.newKeySet();

    public CombatLogManager(ESDuels plugin) {
        this(plugin.getDataFolder(), plugin.getLogger());
        this.plugin = plugin;
    }

    public CombatLogManager(File dataFolder, java.util.logging.Logger logger) {
        this.logger = logger != null ? logger : java.util.logging.Logger.getLogger("CombatLogManager");
        this.file = new File(dataFolder, "combat_logs.yml");
        load();
    }

    public synchronized void load() {
        combatLogDeaths.clear();
        if (!file.exists()) {
            return;
        }
        try {
            FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
            List<String> list = yaml.getStringList("combat-loggers");
            for (String str : list) {
                try {
                    combatLogDeaths.add(UUID.fromString(str));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load combat_logs.yml", e);
        }
    }

    public synchronized void save() {
        try {
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            FileConfiguration yaml = new YamlConfiguration();
            List<String> list = combatLogDeaths.stream().map(UUID::toString).toList();
            yaml.set("combat-loggers", list);
            yaml.save(file);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to save combat_logs.yml", e);
        }
    }

    public void addCombatLogger(UUID uuid) {
        combatLogDeaths.add(uuid);
        save();
    }

    public boolean hasCombatLogger(UUID uuid) {
        return combatLogDeaths.contains(uuid);
    }

    public void removeCombatLogger(UUID uuid) {
        combatLogDeaths.remove(uuid);
        save();
    }

    public Set<UUID> getCombatLoggers() {
        return Collections.unmodifiableSet(combatLogDeaths);
    }

    /**
     * Handles returning a combat-logger to server: ensures they are dead, out of the arena,
     * at the main duels spawn, and possess no loot.
     */
    public void handleCombatLogRejoin(Player player) {
        UUID uuid = player.getUniqueId();
        removeCombatLogger(uuid);

        // 1. Wipe inventory completely
        wipeInventory(player);

        // 2. Register for spawn respawn override
        plugin.getDuelManager().addPendingRespawn(uuid);

        // 3. Teleport to duels spawn immediately
        plugin.getSpawnManager().teleportToSpawn(player);

        // 4. Follow-up next tick to ensure no client desync, ensure out of arena, and enforce death
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;

            plugin.getSpawnManager().teleportToSpawn(player);
            wipeInventory(player);

            if (!player.isDead()) {
                plugin.getDuelManager().addPendingRespawn(uuid);
                try {
                    player.setHealth(0.0);
                } catch (Exception ignored) {
                }
            }

            // Defeat title, subtitle, and chat notification
            String defTitle = plugin.getConfigManager().getMessage("duel.defeat-title");
            String defSub = "&cYou disconnected during a duel!";
            MessageUtil.sendTitle(player, defTitle, defSub, 5, 60, 15);

            String combatMsg = plugin.getConfigManager().getMessage("duel.combat-logged");
            player.sendMessage(MessageUtil.toComponent(combatMsg));
            SoundUtil.playSound(player, "ENTITY_PLAYER_DEATH", 1.0f, 1.0f);
        });
    }

    private void wipeInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        player.setTotalExperience(0);
        player.setExp(0);
        player.setLevel(0);
    }
}
