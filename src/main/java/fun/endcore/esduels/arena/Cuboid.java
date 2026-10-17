package fun.endcore.esduels.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Represents a 3D cuboid bounding box between two coordinates in a world.
 */
public class Cuboid implements Iterable<Block> {
    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public Cuboid(Location point1, Location point2) {
        if (!Objects.equals(point1.getWorld(), point2.getWorld())) {
            throw new IllegalArgumentException("Points must be in the same world!");
        }
        this.worldName = point1.getWorld() != null ? point1.getWorld().getName() : "world";
        this.minX = Math.min(point1.getBlockX(), point2.getBlockX());
        this.minY = Math.min(point1.getBlockY(), point2.getBlockY());
        this.minZ = Math.min(point1.getBlockZ(), point2.getBlockZ());
        this.maxX = Math.max(point1.getBlockX(), point2.getBlockX());
        this.maxY = Math.max(point1.getBlockY(), point2.getBlockY());
        this.maxZ = Math.max(point1.getBlockZ(), point2.getBlockZ());
    }

    public Cuboid(String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.worldName = worldName;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public String getWorldName() {
        return worldName;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public int getVolume() {
        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(worldName)) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public List<Block> getBlocks() {
        List<Block> blocks = new ArrayList<>(getVolume());
        World world = getWorld();
        if (world == null) return blocks;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    blocks.add(world.getBlockAt(x, y, z));
                }
            }
        }
        return blocks;
    }

    @Override
    public Iterator<Block> iterator() {
        return getBlocks().iterator();
    }
}
