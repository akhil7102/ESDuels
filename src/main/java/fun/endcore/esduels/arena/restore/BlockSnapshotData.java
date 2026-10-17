package fun.endcore.esduels.arena.restore;

import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Stores serialized block state and optional tile entity / container contents.
 */
public class BlockSnapshotData {
    private final int x;
    private final int y;
    private final int z;
    private final String blockDataString;
    private final Map<Integer, Map<String, Object>> serializedInventory;
    private final String customName;
    private final List<String> signLines;

    public BlockSnapshotData(int x, int y, int z, String blockDataString,
                             Map<Integer, Map<String, Object>> serializedInventory,
                             String customName, List<String> signLines) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.blockDataString = blockDataString;
        this.serializedInventory = serializedInventory;
        this.customName = customName;
        this.signLines = signLines;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String getBlockDataString() {
        return blockDataString;
    }

    public Map<Integer, Map<String, Object>> getSerializedInventory() {
        return serializedInventory;
    }

    public String getCustomName() {
        return customName;
    }

    public List<String> getSignLines() {
        return signLines;
    }

    public boolean isContainer() {
        return serializedInventory != null && !serializedInventory.isEmpty();
    }
}
