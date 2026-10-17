package fun.endcore.esduels.listener;

import fun.endcore.esduels.ESDuels;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Ensures players defeated in duels respawn directly at the main duels spawn.
 */
public class RespawnListener implements Listener {
    private final ESDuels plugin;

    public RespawnListener(ESDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        java.util.UUID uuid = player.getUniqueId();
        if (plugin.getDuelManager().consumePendingRespawn(uuid) ||
                plugin.getDuelManager().isInDuel(uuid) ||
                plugin.getLootTimerManager().isInLootSession(uuid)) {
            Location spawn = plugin.getSpawnManager().getSpawnLocation();
            if (spawn != null) {
                event.setRespawnLocation(spawn);
            }
        }
    }
}
