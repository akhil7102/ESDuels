package fun.endcore.esduels.listener;

import fun.endcore.esduels.ESDuels;
import fun.endcore.esduels.duel.Duel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles connection, disconnection, and quit events for players in duels, queues, or loot sessions.
 */
public class DuelConnectionListener implements Listener {
    private final ESDuels plugin;

    public DuelConnectionListener(ESDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // If player previously combat-logged out of an active duel, enforce death penalty
        if (plugin.getCombatLogManager().hasCombatLogger(player.getUniqueId())) {
            plugin.getCombatLogManager().handleCombatLogRejoin(player);
        }

        // Warm up stats cache asynchronously
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getStatsManager().getStats(player.getUniqueId());
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // 1. If queued, remove cleanly
        if (plugin.getQueueManager().isQueued(player.getUniqueId())) {
            plugin.getQueueManager().leaveQueue(player.getUniqueId(), false);
        }

        // 2. Stop music
        plugin.getMusicManager().stopMusic(player);

        // 3. If in loot period as winner
        if (plugin.getLootTimerManager().isInLootSession(player.getUniqueId())) {
            Duel duel = plugin.getDuelManager().getDuel(player.getUniqueId());
            plugin.getLootTimerManager().endLootSession(player.getUniqueId(), false);
            if (duel != null) {
                plugin.getDuelManager().cleanupDuelState(duel);
                plugin.getDuelManager().triggerArenaRestoration(duel.getArena());
            }
            plugin.getSpawnManager().teleportToSpawn(player);
        } else if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            // 4. If in active combat/countdown, handle combat-log disconnect
            plugin.getDuelManager().handleDisconnectInDuel(player);
        }

        // 5. Cleanup challenge requests and cache
        plugin.getDuelRequestManager().removeAllForPlayer(player.getUniqueId());
        plugin.getStatsManager().unloadPlayer(player.getUniqueId());
    }
}
