package fun.endcore.esduels.arena.restore;

import java.util.Collections;
import java.util.List;

/**
 * In-memory representation of an entire arena's captured block state snapshot.
 */
public class ArenaSnapshot {
    private final String arenaName;
    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final List<BlockSnapshotData> blocks;

    public ArenaSnapshot(String arenaName, String worldName,
                         int minX, int minY, int minZ,
                         int maxX, int maxY, int maxZ,
                         List<BlockSnapshotData> blocks) {
        this.arenaName = arenaName;
        this.worldName = worldName;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.blocks = blocks != null ? blocks : Collections.emptyList();
    }

    public String getArenaName() {
        return arenaName;
    }

    public String getWorldName() {
        return worldName;
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

    public List<BlockSnapshotData> getBlocks() {
        return blocks;
    }

    public int getTotalBlocks() {
        return blocks.size();
    }
}
