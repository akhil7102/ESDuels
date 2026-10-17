package fun.endcore.esduels.arena;

import fun.endcore.esduels.ESDuels;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages arena lifecycle, disk persistence (arenas/<name>.yml), and allocation.
 */
public class ArenaManager {
    private final ESDuels plugin;
    private final File arenasDir;
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();

    public ArenaManager(ESDuels plugin) {
        this.plugin = plugin;
        this.arenasDir = new File(plugin.getDataFolder(), "arenas");
        if (!arenasDir.exists()) {
            arenasDir.mkdirs();
        }
        loadArenas();
    }

    public void loadArenas() {
        arenas.clear();
        File[] files = arenasDir.listFiles((dir, name) -> name.endsWith(".yml") && !name.equalsIgnoreCase("arenas.yml"));
        if (files == null) return;

        for (File file : files) {
            String name = file.getName().substring(0, file.getName().length() - 4);
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            Arena arena = new Arena(name);
            arena.setWorldName(config.getString("world"));
            arena.setPos1(deserializeLocation(config, "pos1"));
            arena.setPos2(deserializeLocation(config, "pos2"));
            arena.setCorner1(deserializeLocation(config, "corner1"));
            arena.setCorner2(deserializeLocation(config, "corner2"));

            boolean hasSnap = plugin.getSnapshotStorage().hasSnapshot(name);
            arena.setHasSnapshot(hasSnap);

            if (arena.isConfigured()) {
                arena.setState(ArenaState.IDLE);
            } else {
                arena.setState(ArenaState.INCOMPLETE);
            }

            arenas.put(name.toLowerCase(), arena);
        }

        plugin.getLogger().info("Loaded " + arenas.size() + " arenas.");
    }

    public static final java.util.regex.Pattern ARENA_NAME_PATTERN = java.util.regex.Pattern.compile("^[a-zA-Z0-9_-]{1,32}$");

    public static boolean isValidArenaName(String name) {
        return name != null && ARENA_NAME_PATTERN.matcher(name).matches();
    }

    public File getArenaFile(String name) {
        if (!isValidArenaName(name)) {
            throw new IllegalArgumentException("Invalid arena name: " + name);
        }
        File file = new File(arenasDir, name.toLowerCase() + ".yml");
        try {
            if (!file.getCanonicalPath().startsWith(arenasDir.getCanonicalPath())) {
                throw new SecurityException("Path traversal attempt detected: " + name);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not resolve canonical path for arena " + name, e);
        }
        return file;
    }

    public Arena createArena(String name) {
        if (!isValidArenaName(name)) {
            return null;
        }
        String lower = name.toLowerCase();
        if (arenas.containsKey(lower)) {
            return null;
        }
        Arena arena = new Arena(name);
        arena.setState(ArenaState.INCOMPLETE);
        arenas.put(lower, arena);
        saveArenaConfig(arena);
        return arena;
    }

    public boolean deleteArena(String name) {
        if (!isValidArenaName(name)) {
            return false;
        }
        Arena arena = arenas.remove(name.toLowerCase());
        if (arena != null) {
            File file = getArenaFile(arena.getName());
            if (file.exists()) {
                file.delete();
            }
            plugin.getSnapshotStorage().deleteSnapshot(arena.getName());
            return true;
        }
        return false;
    }

    public Arena getArena(String name) {
        if (name == null || !isValidArenaName(name)) return null;
        return arenas.get(name.toLowerCase());
    }

    public Collection<Arena> getArenas() {
        return Collections.unmodifiableCollection(arenas.values());
    }

    /**
     * Atomically finds and reserves an available arena.
     */
    public synchronized Arena allocateAvailableArena() {
        for (Arena arena : arenas.values()) {
            if (arena.isAvailable()) {
                arena.setState(ArenaState.COUNTDOWN);
                return arena;
            }
        }
        return null;
    }

    public void saveArenaConfig(Arena arena) {
        File file = getArenaFile(arena.getName());
        YamlConfiguration config = new YamlConfiguration();

        if (arena.getWorldName() != null) {
            config.set("world", arena.getWorldName());
        }
        serializeLocation(config, "pos1", arena.getPos1());
        serializeLocation(config, "pos2", arena.getPos2());
        serializeLocation(config, "corner1", arena.getCorner1());
        serializeLocation(config, "corner2", arena.getCorner2());
        config.set("hasSnapshot", arena.hasSnapshot());

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save arena configuration for " + arena.getName(), e);
        }
    }

    public String getIncompleteReason(Arena arena) {
        if (arena == null) return "Arena does not exist";
        List<String> missing = new ArrayList<>();
        if (arena.getPos1() == null) missing.add("pos1");
        if (arena.getPos2() == null) missing.add("pos2");
        if (arena.getCorner1() == null) missing.add("corner1");
        if (arena.getCorner2() == null) missing.add("corner2");
        if (arena.getWorld() == null) missing.add("valid world");
        if (!arena.hasSnapshot()) missing.add("saved snapshot");
        return missing.isEmpty() ? "None" : String.join(", ", missing);
    }

    private void serializeLocation(YamlConfiguration config, String path, Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", loc.getYaw());
        config.set(path + ".pitch", loc.getPitch());
    }

    private Location deserializeLocation(YamlConfiguration config, String path) {
        if (!config.contains(path + ".world")) return null;
        String worldName = config.getString(path + ".world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        double x = config.getDouble(path + ".x");
        double y = config.getDouble(path + ".y");
        double z = config.getDouble(path + ".z");
        float yaw = (float) config.getDouble(path + ".yaw", 0.0);
        float pitch = (float) config.getDouble(path + ".pitch", 0.0);

        return new Location(world, x, y, z, yaw, pitch);
    }
}
