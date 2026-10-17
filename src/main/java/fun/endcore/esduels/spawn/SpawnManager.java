package fun.endcore.esduels.spawn;

import fun.endcore.esduels.ESDuels;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Manages the main duels spawn location persisted in spawn.yml.
 */
public class SpawnManager {
    private final ESDuels plugin;
    private final File spawnFile;
    private FileConfiguration spawnConfig;
    private Location spawnLocation;

    public SpawnManager(ESDuels plugin) {
        this.plugin = plugin;
        this.spawnFile = new File(plugin.getDataFolder(), "spawn.yml");
        load();
    }

    public void load() {
        if (!spawnFile.exists()) {
            try {
                if (spawnFile.getParentFile() != null) {
                    spawnFile.getParentFile().mkdirs();
                }
                spawnFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create spawn.yml", e);
            }
        }
        spawnConfig = YamlConfiguration.loadConfiguration(spawnFile);

        if (spawnConfig.contains("world")) {
            String worldName = spawnConfig.getString("world");
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                double x = spawnConfig.getDouble("x");
                double y = spawnConfig.getDouble("y");
                double z = spawnConfig.getDouble("z");
                float yaw = (float) spawnConfig.getDouble("yaw");
                float pitch = (float) spawnConfig.getDouble("pitch");
                this.spawnLocation = new Location(world, x, y, z, yaw, pitch);
            }
        }
    }

    /**
     * Sets the main spawn location and persists to spawn.yml.
     */
    public void setSpawnLocation(Location location) {
        this.spawnLocation = location.clone();
        if (location.getWorld() != null) {
            spawnConfig.set("world", location.getWorld().getName());
            spawnConfig.set("x", location.getX());
            spawnConfig.set("y", location.getY());
            spawnConfig.set("z", location.getZ());
            spawnConfig.set("yaw", location.getYaw());
            spawnConfig.set("pitch", location.getPitch());

            try {
                spawnConfig.save(spawnFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save spawn.yml", e);
            }
        }
    }

    public boolean isSpawnSet() {
        return spawnLocation != null && spawnLocation.getWorld() != null;
    }

    public Location getSpawnLocation() {
        if (spawnLocation != null && spawnLocation.getWorld() != null) {
            return spawnLocation.clone();
        }
        // Fallback to default world spawn if configured spawn is missing
        if (!Bukkit.getWorlds().isEmpty()) {
            return Bukkit.getWorlds().get(0).getSpawnLocation();
        }
        return null;
    }

    /**
     * Safely teleports a player to the duels spawn location.
     */
    public boolean teleportToSpawn(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }
        Location target = getSpawnLocation();
        if (target != null) {
            player.teleport(target);
            return true;
        }
        return false;
    }
}
