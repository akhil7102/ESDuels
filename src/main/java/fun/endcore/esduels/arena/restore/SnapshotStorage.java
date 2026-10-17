package fun.endcore.esduels.arena.restore;

import fun.endcore.esduels.ESDuels;
import fun.endcore.esduels.arena.Cuboid;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Handles capturing block state on the server thread, and serializing / deserializing
 * YAML snapshot files asynchronously to keep the main server thread lag-free.
 */
public class SnapshotStorage {
    private final ESDuels plugin;
    private final File snapshotsDir;

    private final Set<String> activeSnapshotOperations = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public SnapshotStorage(ESDuels plugin) {
        this.plugin = plugin;
        this.snapshotsDir = new File(plugin.getDataFolder(), "arenas" + File.separator + "snapshots");
        if (!snapshotsDir.exists()) {
            snapshotsDir.mkdirs();
        }
    }

    public File getSnapshotFile(String arenaName) {
        if (!fun.endcore.esduels.arena.ArenaManager.isValidArenaName(arenaName)) {
            throw new IllegalArgumentException("Invalid arena name: " + arenaName);
        }
        File file = new File(snapshotsDir, arenaName.toLowerCase() + ".yml");
        try {
            if (!file.getCanonicalPath().startsWith(snapshotsDir.getCanonicalPath())) {
                throw new SecurityException("Path traversal attempt detected in snapshot: " + arenaName);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not resolve canonical path for snapshot " + arenaName, e);
        }
        return file;
    }

    public boolean hasSnapshot(String arenaName) {
        if (!fun.endcore.esduels.arena.ArenaManager.isValidArenaName(arenaName)) {
            return false;
        }
        try {
            File file = getSnapshotFile(arenaName);
            return file.exists() && file.isFile() && file.length() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Captures current block state on the main thread, then writes asynchronously to disk.
     */
    public void captureAndSaveAsync(String arenaName, Cuboid cuboid, Consumer<Boolean> callback) {
        if (!fun.endcore.esduels.arena.ArenaManager.isValidArenaName(arenaName)) {
            if (callback != null) callback.accept(false);
            return;
        }

        String lower = arenaName.toLowerCase();
        if (!activeSnapshotOperations.add(lower)) {
            plugin.getLogger().warning("Snapshot operation already in progress for arena: " + arenaName);
            if (callback != null) callback.accept(false);
            return;
        }

        // Main thread step: safely read BlockData and tile entities
        List<BlockSnapshotData> blocks = new ArrayList<>(cuboid.getVolume());

        for (int x = cuboid.getMinX(); x <= cuboid.getMaxX(); x++) {
            for (int y = cuboid.getMinY(); y <= cuboid.getMaxY(); y++) {
                for (int z = cuboid.getMinZ(); z <= cuboid.getMaxZ(); z++) {
                    Block block = cuboid.getWorld().getBlockAt(x, y, z);
                    String dataString = block.getBlockData().getAsString();

                    Map<Integer, Map<String, Object>> serializedInv = null;
                    String customName = null;
                    List<String> signLines = null;

                    BlockState state = block.getState();
                    if (state instanceof Container container) {
                        serializedInv = new HashMap<>();
                        ItemStack[] contents = container.getInventory().getContents();
                        for (int slot = 0; slot < contents.length; slot++) {
                            ItemStack item = contents[slot];
                            if (item != null && !item.getType().isAir()) {
                                serializedInv.put(slot, item.serialize());
                            }
                        }
                        if (container.customName() != null) {
                            customName = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(container.customName());
                        }
                    } else if (state instanceof Sign sign) {
                        signLines = new ArrayList<>();
                        for (int line = 0; line < 4; line++) {
                            net.kyori.adventure.text.Component lineComp = sign.getSide(Side.FRONT).line(line);
                            String lineStr = lineComp != null ? net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().serialize(lineComp) : "";
                            signLines.add(lineStr);
                        }
                    }

                    blocks.add(new BlockSnapshotData(x, y, z, dataString, serializedInv, customName, signLines));
                }
            }
        }

        ArenaSnapshot snapshot = new ArenaSnapshot(
                arenaName,
                cuboid.getWorldName(),
                cuboid.getMinX(), cuboid.getMinY(), cuboid.getMinZ(),
                cuboid.getMaxX(), cuboid.getMaxY(), cuboid.getMaxZ(),
                blocks
        );

        // Async step: serialize and write to disk
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            boolean success = false;
            try {
                success = writeSnapshotToDisk(snapshot);
            } finally {
                activeSnapshotOperations.remove(lower);
            }
            final boolean finalSuccess = success;
            if (callback != null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(finalSuccess));
            }
        });
    }

    private boolean writeSnapshotToDisk(ArenaSnapshot snapshot) {
        File finalFile = getSnapshotFile(snapshot.getArenaName());
        File tmpFile = new File(snapshotsDir, snapshot.getArenaName().toLowerCase() + ".yml.tmp");
        YamlConfiguration config = new YamlConfiguration();

        config.set("arena", snapshot.getArenaName());
        config.set("world", snapshot.getWorldName());
        config.set("minX", snapshot.getMinX());
        config.set("minY", snapshot.getMinY());
        config.set("minZ", snapshot.getMinZ());
        config.set("maxX", snapshot.getMaxX());
        config.set("maxY", snapshot.getMaxY());
        config.set("maxZ", snapshot.getMaxZ());
        config.set("totalBlocks", snapshot.getTotalBlocks());

        List<Map<String, Object>> blockList = new ArrayList<>(snapshot.getBlocks().size());
        for (BlockSnapshotData b : snapshot.getBlocks()) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("x", b.getX());
            map.put("y", b.getY());
            map.put("z", b.getZ());
            map.put("data", b.getBlockDataString());

            if (b.getSerializedInventory() != null && !b.getSerializedInventory().isEmpty()) {
                map.put("items", b.getSerializedInventory());
            }
            if (b.getCustomName() != null) {
                map.put("name", b.getCustomName());
            }
            if (b.getSignLines() != null && !b.getSignLines().isEmpty()) {
                map.put("sign", b.getSignLines());
            }

            blockList.add(map);
        }

        config.set("blocks", blockList);

        try {
            config.save(tmpFile);
            try {
                java.nio.file.Files.move(
                        tmpFile.toPath(),
                        finalFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE
                );
            } catch (java.io.IOException atomicEx) {
                // Fallback to standard replace move if filesystem doesn't support atomic move across volumes
                java.nio.file.Files.move(
                        tmpFile.toPath(),
                        finalFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            }
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to asynchronously save snapshot for " + snapshot.getArenaName(), e);
            return false;
        } finally {
            if (tmpFile.exists()) {
                tmpFile.delete();
            }
        }
    }

    /**
     * Asynchronously loads and parses the snapshot file from disk.
     */
    public CompletableFuture<ArenaSnapshot> loadSnapshotAsync(String arenaName) {
        CompletableFuture<ArenaSnapshot> future = new CompletableFuture<>();
        if (!fun.endcore.esduels.arena.ArenaManager.isValidArenaName(arenaName)) {
            future.complete(null);
            return future;
        }
        File file;
        try {
            file = getSnapshotFile(arenaName);
        } catch (Exception e) {
            future.complete(null);
            return future;
        }
        if (!file.exists() || file.length() == 0) {
            future.complete(null);
            return future;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                String world = config.getString("world", "world");
                int minX = config.getInt("minX");
                int minY = config.getInt("minY");
                int minZ = config.getInt("minZ");
                int maxX = config.getInt("maxX");
                int maxY = config.getInt("maxY");
                int maxZ = config.getInt("maxZ");

                List<Map<?, ?>> rawList = config.getMapList("blocks");
                List<BlockSnapshotData> blocks = new ArrayList<>(rawList.size());

                for (Map<?, ?> raw : rawList) {
                    int x = (Integer) raw.get("x");
                    int y = (Integer) raw.get("y");
                    int z = (Integer) raw.get("z");
                    String data = (String) raw.get("data");

                    Map<Integer, Map<String, Object>> inv = null;
                    if (raw.containsKey("items")) {
                        inv = new HashMap<>();
                        Map<?, ?> itemsMap = (Map<?, ?>) raw.get("items");
                        for (Map.Entry<?, ?> entry : itemsMap.entrySet()) {
                            int slot = Integer.parseInt(entry.getKey().toString());
                            @SuppressWarnings("unchecked")
                            Map<String, Object> itemData = (Map<String, Object>) entry.getValue();
                            inv.put(slot, itemData);
                        }
                    }

                    String name = (String) raw.get("name");
                    @SuppressWarnings("unchecked")
                    List<String> sign = (List<String>) raw.get("sign");

                    blocks.add(new BlockSnapshotData(x, y, z, data, inv, name, sign));
                }

                ArenaSnapshot snapshot = new ArenaSnapshot(arenaName, world, minX, minY, minZ, maxX, maxY, maxZ, blocks);
                future.complete(snapshot);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to asynchronously load snapshot for " + arenaName, e);
                future.complete(null);
            }
        });

        return future;
    }

    public boolean deleteSnapshot(String arenaName) {
        if (!fun.endcore.esduels.arena.ArenaManager.isValidArenaName(arenaName)) {
            return false;
        }
        try {
            File file = getSnapshotFile(arenaName);
            if (file.exists()) {
                return file.delete();
            }
        } catch (Exception ignored) {
        }
        return false;
    }
}
