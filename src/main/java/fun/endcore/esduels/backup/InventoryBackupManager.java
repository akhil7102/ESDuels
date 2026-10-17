package fun.endcore.esduels.backup;

import fun.endcore.esduels.ESDuels;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Creates pre-duel backups of player inventories and player states to prevent
 * accidental item loss during server crashes or unexpected interruptions.
 */
public class InventoryBackupManager {
    private final ESDuels plugin;
    private final File backupsDir;
    private final Map<UUID, PlayerBackupData> backupCache = new ConcurrentHashMap<>();

    public InventoryBackupManager(ESDuels plugin) {
        this.plugin = plugin;
        this.backupsDir = new File(plugin.getDataFolder(), "data" + File.separator + "backups");
        if (!backupsDir.exists()) {
            backupsDir.mkdirs();
        }
    }

    public static class PlayerBackupData {
        private final UUID uuid;
        private final ItemStack[] contents;
        private final ItemStack[] armor;
        private final ItemStack offHand;
        private final float exp;
        private final int level;
        private final double health;
        private final int foodLevel;
        private final long timestamp;

        public PlayerBackupData(Player player) {
            this.uuid = player.getUniqueId();
            this.contents = cloneItemArray(player.getInventory().getContents());
            this.armor = cloneItemArray(player.getInventory().getArmorContents());
            this.offHand = player.getInventory().getItemInOffHand().clone();
            this.exp = player.getExp();
            this.level = player.getLevel();
            this.health = player.getHealth();
            this.foodLevel = player.getFoodLevel();
            this.timestamp = System.currentTimeMillis();
        }

        public PlayerBackupData(UUID uuid, ItemStack[] contents, ItemStack[] armor, ItemStack offHand,
                                float exp, int level, double health, int foodLevel, long timestamp) {
            this.uuid = uuid;
            this.contents = contents;
            this.armor = armor;
            this.offHand = offHand;
            this.exp = exp;
            this.level = level;
            this.health = health;
            this.foodLevel = foodLevel;
            this.timestamp = timestamp;
        }

        private static ItemStack[] cloneItemArray(ItemStack[] source) {
            ItemStack[] copy = new ItemStack[source.length];
            for (int i = 0; i < source.length; i++) {
                if (source[i] != null) {
                    copy[i] = source[i].clone();
                }
            }
            return copy;
        }

        public UUID getUuid() { return uuid; }
        public ItemStack[] getContents() { return contents; }
        public ItemStack[] getArmor() { return armor; }
        public ItemStack getOffHand() { return offHand; }
        public float getExp() { return exp; }
        public int getLevel() { return level; }
        public double getHealth() { return health; }
        public int getFoodLevel() { return foodLevel; }
        public long getTimestamp() { return timestamp; }
    }

    /**
     * Captures a backup before duel start and writes asynchronously to disk.
     */
    public void createBackup(Player player) {
        if (player == null || !player.isOnline()) return;

        PlayerBackupData backup = new PlayerBackupData(player);
        backupCache.put(player.getUniqueId(), backup);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(backupsDir, player.getUniqueId() + ".yml");
            YamlConfiguration config = new YamlConfiguration();
            config.set("uuid", player.getUniqueId().toString());
            config.set("timestamp", backup.getTimestamp());
            config.set("level", backup.getLevel());
            config.set("exp", backup.getExp());
            config.set("health", backup.getHealth());
            config.set("foodLevel", backup.getFoodLevel());

            List<Map<String, Object>> contentsList = new ArrayList<>();
            for (int i = 0; i < backup.getContents().length; i++) {
                ItemStack item = backup.getContents()[i];
                if (item != null && !item.getType().isAir()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("slot", i);
                    map.put("item", item.serialize());
                    contentsList.add(map);
                }
            }
            config.set("contents", contentsList);

            List<Map<String, Object>> armorList = new ArrayList<>();
            for (int i = 0; i < backup.getArmor().length; i++) {
                ItemStack item = backup.getArmor()[i];
                if (item != null && !item.getType().isAir()) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("slot", i);
                    map.put("item", item.serialize());
                    armorList.add(map);
                }
            }
            config.set("armor", armorList);

            if (backup.getOffHand() != null && !backup.getOffHand().getType().isAir()) {
                config.set("offhand", backup.getOffHand().serialize());
            }

            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to asynchronously save inventory backup for " + player.getName(), e);
            }
        });
    }

    /**
     * Clears and deletes the backup after a match concludes normally.
     */
    public void clearBackup(UUID uuid) {
        backupCache.remove(uuid);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = new File(backupsDir, uuid + ".yml");
            if (file.exists()) {
                file.delete();
            }
        });
    }

    /**
     * Loads a backup from disk if not present in memory cache.
     */
    public PlayerBackupData loadBackup(UUID uuid) {
        if (uuid == null) return null;
        PlayerBackupData cached = backupCache.get(uuid);
        if (cached != null) return cached;

        File file = new File(backupsDir, uuid + ".yml");
        if (!file.exists()) return null;

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            long timestamp = config.getLong("timestamp", System.currentTimeMillis());
            int level = config.getInt("level", 0);
            float exp = (float) config.getDouble("exp", 0.0);
            double health = config.getDouble("health", 20.0);
            int foodLevel = config.getInt("foodLevel", 20);

            ItemStack[] contents = new ItemStack[41];
            List<Map<?, ?>> contentsList = config.getMapList("contents");
            for (Map<?, ?> map : contentsList) {
                int slot = (Integer) map.get("slot");
                @SuppressWarnings("unchecked")
                Map<String, Object> itemData = (Map<String, Object>) map.get("item");
                if (slot >= 0 && slot < contents.length) {
                    contents[slot] = ItemStack.deserialize(itemData);
                }
            }

            ItemStack[] armor = new ItemStack[4];
            List<Map<?, ?>> armorList = config.getMapList("armor");
            for (Map<?, ?> map : armorList) {
                int slot = (Integer) map.get("slot");
                @SuppressWarnings("unchecked")
                Map<String, Object> itemData = (Map<String, Object>) map.get("item");
                if (slot >= 0 && slot < armor.length) {
                    armor[slot] = ItemStack.deserialize(itemData);
                }
            }

            ItemStack offhand = null;
            if (config.isConfigurationSection("offhand")) {
                offhand = ItemStack.deserialize(Objects.requireNonNull(config.getConfigurationSection("offhand")).getValues(false));
            }

            PlayerBackupData data = new PlayerBackupData(uuid, contents, armor, offhand, exp, level, health, foodLevel, timestamp);
            backupCache.put(uuid, data);
            return data;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load backup from disk for " + uuid, e);
            return null;
        }
    }

    /**
     * Restores a player's inventory in the event of an exceptional cancellation.
     */
    public boolean restoreBackup(Player player) {
        if (player == null || !player.isOnline()) return false;

        PlayerBackupData backup = backupCache.remove(player.getUniqueId());
        if (backup != null) {
            player.getInventory().setContents(backup.getContents());
            player.getInventory().setArmorContents(backup.getArmor());
            player.getInventory().setItemInOffHand(backup.getOffHand());
            player.setExp(backup.getExp());
            player.setLevel(backup.getLevel());
            player.setHealth(Math.min(player.getMaxHealth(), backup.getHealth()));
            player.setFoodLevel(backup.getFoodLevel());
            clearBackup(player.getUniqueId());
            return true;
        }
        return false;
    }
}
