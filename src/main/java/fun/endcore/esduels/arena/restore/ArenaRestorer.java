package fun.endcore.esduels.arena.restore;

import fun.endcore.esduels.ESDuels;
import fun.endcore.esduels.arena.Arena;
import fun.endcore.esduels.arena.Cuboid;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Handles bounded main-thread restoration of arena blocks and clearance of dropped entities.
 */
public class ArenaRestorer {
    private final ESDuels plugin;

    public ArenaRestorer(ESDuels plugin) {
        this.plugin = plugin;
    }

    /**
     * Executes the complete restoration workflow:
     * 1. Safely eject any remaining players to spawn.
     * 2. Clear remaining dropped items/entities.
     * 3. Load snapshot asynchronously from disk.
     * 4. Restore blocks in bounded main-thread batches.
     * 5. Invoke callback upon completion.
     */
    public void restoreArena(Arena arena, Consumer<Boolean> onComplete) {
        Cuboid cuboid = arena.getCuboid();
        if (cuboid == null || cuboid.getWorld() == null) {
            plugin.getLogger().warning("Cannot restore arena " + arena.getName() + ": Invalid cuboid or world.");
            if (onComplete != null) onComplete.accept(false);
            return;
        }

        World world = cuboid.getWorld();
        org.bukkit.util.BoundingBox box = new org.bukkit.util.BoundingBox(
                cuboid.getMinX(), cuboid.getMinY(), cuboid.getMinZ(),
                cuboid.getMaxX() + 1.0, cuboid.getMaxY() + 1.0, cuboid.getMaxZ() + 1.0
        );

        // 1 & 2. Clear players to spawn and clear lingering dropped items, projectiles, primed TNT, crystals
        clearArenaEntities(world, box);

        // 3. Load snapshot file asynchronously
        plugin.getSnapshotStorage().loadSnapshotAsync(arena.getName()).thenAccept(snapshot -> {
            if (snapshot == null || snapshot.getBlocks().isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getLogger().severe("Failed to load snapshot for arena " + arena.getName() + " or snapshot was empty!");
                    if (onComplete != null) onComplete.accept(false);
                });
                return;
            }

            // 4. Dispatch bounded restoration batches to main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                executeBatchedRestoration(arena, snapshot, onComplete);
            });
        });
    }

    private void executeBatchedRestoration(Arena arena, ArenaSnapshot snapshot, Consumer<Boolean> onComplete) {
        List<BlockSnapshotData> blocks = snapshot.getBlocks();
        int batchSize = Math.max(50, plugin.getConfigManager().getConfig().getInt("restoration.batch-size", 250));
        boolean logProgress = plugin.getConfigManager().getConfig().getBoolean("restoration.log-restoration", true);

        World world = arena.getWorld();
        if (world == null) {
            plugin.getLogger().severe("Arena world is null during restoration of " + arena.getName());
            if (onComplete != null) onComplete.accept(false);
            return;
        }

        new BukkitRunnable() {
            private int currentIndex = 0;
            private int alteredBlocks = 0;

            @Override
            public void run() {
                int count = 0;
                while (currentIndex < blocks.size() && count < batchSize) {
                    BlockSnapshotData data = blocks.get(currentIndex);
                    Block block = world.getBlockAt(data.getX(), data.getY(), data.getZ());

                    String currentDataString = block.getBlockData().getAsString();

                    // Optimize: only mutate blocks if data differs or block is a container
                    if (!currentDataString.equals(data.getBlockDataString()) || data.isContainer()) {
                        try {
                            BlockData newBlockData = Bukkit.createBlockData(data.getBlockDataString());
                            block.setBlockData(newBlockData, false);
                            alteredBlocks++;

                            BlockState state = block.getState();
                            if (state instanceof Container container && data.getSerializedInventory() != null) {
                                container.getInventory().clear();
                                for (Map.Entry<Integer, Map<String, Object>> entry : data.getSerializedInventory().entrySet()) {
                                    ItemStack item = ItemStack.deserialize(entry.getValue());
                                    container.getInventory().setItem(entry.getKey(), item);
                                }
                                if (data.getCustomName() != null) {
                                    container.customName(fun.endcore.esduels.util.MessageUtil.toComponent(data.getCustomName()));
                                }
                                container.update(true, false);
                            } else if (state instanceof Sign sign && data.getSignLines() != null) {
                                for (int i = 0; i < Math.min(4, data.getSignLines().size()); i++) {
                                    sign.getSide(Side.FRONT).line(i, fun.endcore.esduels.util.MessageUtil.toComponent(data.getSignLines().get(i)));
                                }
                                sign.update(true, false);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING, "Error restoring block at " + data.getX() + "," + data.getY() + "," + data.getZ(), e);
                        }
                    }

                    currentIndex++;
                    count++;
                }

                if (currentIndex >= blocks.size()) {
                    cancel();
                    // Final sweep to ensure any dropped item or projectile created during restoration is cleared
                    Cuboid cuboid = arena.getCuboid();
                    if (cuboid != null && world != null) {
                        org.bukkit.util.BoundingBox box = new org.bukkit.util.BoundingBox(
                                cuboid.getMinX(), cuboid.getMinY(), cuboid.getMinZ(),
                                cuboid.getMaxX() + 1.0, cuboid.getMaxY() + 1.0, cuboid.getMaxZ() + 1.0
                        );
                        clearArenaEntities(world, box);
                    }
                    if (logProgress) {
                        plugin.getLogger().info("Successfully restored arena '" + arena.getName() + "' (Processed: " + blocks.size() + ", Altered: " + alteredBlocks + ")");
                    }
                    if (onComplete != null) {
                        onComplete.accept(true);
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void clearArenaEntities(World world, org.bukkit.util.BoundingBox box) {
        if (world == null || box == null) return;
        for (Entity entity : world.getNearbyEntities(box)) {
            if (entity instanceof Player player) {
                plugin.getSpawnManager().teleportToSpawn(player);
            } else if (entity instanceof Item ||
                    entity instanceof org.bukkit.entity.Projectile ||
                    entity instanceof org.bukkit.entity.TNTPrimed ||
                    entity instanceof org.bukkit.entity.EnderCrystal ||
                    entity instanceof org.bukkit.entity.AreaEffectCloud) {
                entity.remove();
            }
        }
    }
}
