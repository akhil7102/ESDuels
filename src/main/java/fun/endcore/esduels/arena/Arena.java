package fun.endcore.esduels.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Represents a configured duel arena.
 */
public class Arena {
    private final String name;
    private String worldName;
    private Location pos1;
    private Location pos2;
    private Location corner1;
    private Location corner2;
    private boolean hasSnapshot;
    private ArenaState state = ArenaState.INCOMPLETE;

    public Arena(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public World getWorld() {
        return worldName != null ? Bukkit.getWorld(worldName) : null;
    }

    public Location getPos1() {
        return pos1 != null ? pos1.clone() : null;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
        if (pos1 != null && pos1.getWorld() != null) {
            this.worldName = pos1.getWorld().getName();
        }
    }

    public Location getPos2() {
        return pos2 != null ? pos2.clone() : null;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
        if (pos2 != null && pos2.getWorld() != null) {
            this.worldName = pos2.getWorld().getName();
        }
    }

    public Location getCorner1() {
        return corner1 != null ? corner1.clone() : null;
    }

    public void setCorner1(Location corner1) {
        this.corner1 = corner1;
        if (corner1 != null && corner1.getWorld() != null) {
            this.worldName = corner1.getWorld().getName();
        }
    }

    public Location getCorner2() {
        return corner2 != null ? corner2.clone() : null;
    }

    public void setCorner2(Location corner2) {
        this.corner2 = corner2;
        if (corner2 != null && corner2.getWorld() != null) {
            this.worldName = corner2.getWorld().getName();
        }
    }

    public boolean hasSnapshot() {
        return hasSnapshot;
    }

    public void setHasSnapshot(boolean hasSnapshot) {
        this.hasSnapshot = hasSnapshot;
    }

    public ArenaState getState() {
        return state;
    }

    public void setState(ArenaState state) {
        this.state = state;
    }

    /**
     * Checks if all required configuration parameters and snapshot are valid.
     */
    public boolean isConfigured() {
        return pos1 != null && pos2 != null && corner1 != null && corner2 != null
                && worldName != null && getWorld() != null && hasSnapshot;
    }

    /**
     * An arena is available if it is configured and currently IDLE.
     */
    public boolean isAvailable() {
        return isConfigured() && state == ArenaState.IDLE;
    }

    /**
     * Generates a Cuboid representing the arena bounding box.
     */
    public Cuboid getCuboid() {
        if (corner1 == null || corner2 == null || getWorld() == null) {
            return null;
        }
        return new Cuboid(corner1, corner2);
    }
}
