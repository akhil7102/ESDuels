package fun.endcore.esduels.listener;

import fun.endcore.esduels.ESDuels;
import fun.endcore.esduels.arena.Arena;
import fun.endcore.esduels.arena.ArenaState;
import fun.endcore.esduels.arena.Cuboid;
import fun.endcore.esduels.duel.Duel;
import fun.endcore.esduels.duel.DuelState;
import fun.endcore.esduels.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import java.util.List;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Protects arenas from unauthorized modifications and prevents escape/interference teleports.
 */
public class ArenaProtectionListener implements Listener {
    private final ESDuels plugin;

    public ArenaProtectionListener(ESDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();

        for (Arena arena : plugin.getArenaManager().getArenas()) {
            Cuboid cuboid = arena.getCuboid();
            if (cuboid != null && cuboid.contains(loc)) {
                // If arena is restoring or incomplete
                if (arena.getState() == ArenaState.RESTORING || arena.getState() == ArenaState.INCOMPLETE) {
                    if (!player.hasPermission("esduels.admin")) {
                        event.setCancelled(true);
                        return;
                    }
                }

                // If arena is active, only participants can break blocks (restored later!)
                Duel duel = plugin.getDuelManager().getDuelByArena(arena);
                if (duel != null) {
                    if (!duel.containsPlayer(player.getUniqueId()) || duel.getState() != DuelState.ACTIVE) {
                        event.setCancelled(true);
                    }
                } else if (!player.hasPermission("esduels.admin")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location loc = block.getLocation();

        for (Arena arena : plugin.getArenaManager().getArenas()) {
            Cuboid cuboid = arena.getCuboid();
            if (cuboid != null && cuboid.contains(loc)) {
                if (arena.getState() == ArenaState.RESTORING || arena.getState() == ArenaState.INCOMPLETE) {
                    if (!player.hasPermission("esduels.admin")) {
                        event.setCancelled(true);
                        return;
                    }
                }

                Duel duel = plugin.getDuelManager().getDuelByArena(arena);
                if (duel != null) {
                    if (!duel.containsPlayer(player.getUniqueId()) || duel.getState() != DuelState.ACTIVE) {
                        event.setCancelled(true);
                    }
                } else if (!player.hasPermission("esduels.admin")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(org.bukkit.event.entity.EntityExplodeEvent event) {
        handleExplosionBlocks(event.getLocation(), event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        handleExplosionBlocks(event.getBlock().getLocation(), event.blockList());
    }

    private void handleExplosionBlocks(Location source, List<Block> blocks) {
        if (blocks == null || blocks.isEmpty()) return;

        blocks.removeIf(block -> {
            Location loc = block.getLocation();
            for (Arena arena : plugin.getArenaManager().getArenas()) {
                Cuboid cuboid = arena.getCuboid();
                if (cuboid != null) {
                    if (cuboid.contains(loc)) {
                        Duel duel = plugin.getDuelManager().getDuelByArena(arena);
                        // Block is inside arena: only allow destruction if active duel
                        return duel == null || duel.getState() != DuelState.ACTIVE;
                    } else if (cuboid.contains(source)) {
                        // Explosion originated inside arena, but block is outside the cuboid: protect it!
                        return true;
                    }
                }
            }
            return false;
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        // Prevent external teleportation (like /home, /tpa) while in active duel
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            PlayerTeleportEvent.TeleportCause cause = event.getCause();
            if (cause == PlayerTeleportEvent.TeleportCause.COMMAND ||
                    cause == PlayerTeleportEvent.TeleportCause.PLUGIN) {
                // Check if plugin internal teleport
                // If destination is outside arena and player is in ACTIVE duel
                Duel duel = plugin.getDuelManager().getDuel(player.getUniqueId());
                if (duel != null && duel.getState() == DuelState.ACTIVE) {
                    Cuboid cuboid = duel.getArena().getCuboid();
                    if (cuboid != null && !cuboid.contains(event.getTo())) {
                        event.setCancelled(true);
                        MessageUtil.sendMessage(player, "&cYou cannot teleport away while in a duel! Use &e/duels leave &cto forfeit.");
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            String msg = event.getMessage().toLowerCase();
            if (msg.startsWith("/duels leave") || msg.startsWith("/duel leave")) {
                return; // Allowed to leave
            }
            if (msg.startsWith("/spawn") || msg.startsWith("/tpa") || msg.startsWith("/home") || msg.startsWith("/warp")) {
                event.setCancelled(true);
                MessageUtil.sendMessage(player, "&cYou cannot execute teleport commands in a duel! Type &e/duels leave &cto forfeit.");
            }
        }
    }
}
