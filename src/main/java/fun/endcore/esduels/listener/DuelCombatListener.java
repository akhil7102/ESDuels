package fun.endcore.esduels.listener;

import fun.endcore.esduels.ESDuels;
import fun.endcore.esduels.arena.Cuboid;
import fun.endcore.esduels.duel.Duel;
import fun.endcore.esduels.duel.DuelState;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Handles in-duel combat, damage protection, deaths, drops, and movement boundaries.
 */
public class DuelCombatListener implements Listener {
    private final ESDuels plugin;

    public DuelCombatListener(ESDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGeneralDamage(org.bukkit.event.entity.EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Duel victimDuel = plugin.getDuelManager().getDuel(victim.getUniqueId());
        if (victimDuel != null && victimDuel.getState() != DuelState.ACTIVE) {
            // Cancel all environmental, fall, fire, lava, and potion damage during countdown and loot period
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }

        Duel victimDuel = plugin.getDuelManager().getDuel(victim.getUniqueId());

        if (attacker != null) {
            Duel attackerDuel = plugin.getDuelManager().getDuel(attacker.getUniqueId());

            // If victim is in a duel
            if (victimDuel != null) {
                // Must be the same duel
                if (victimDuel != attackerDuel) {
                    event.setCancelled(true);
                    return;
                }

                // If in COUNTDOWN or LOOTING, cannot damage
                if (victimDuel.getState() != DuelState.ACTIVE) {
                    event.setCancelled(true);
                    return;
                }
            } else if (attackerDuel != null) {
                // Attacker in duel, victim is not
                event.setCancelled(true);
            }
        } else if (victimDuel != null && victimDuel.getState() != DuelState.ACTIVE) {
            // Environment damage during countdown or loot period is cancelled
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Duel duel = plugin.getDuelManager().getDuel(victim.getUniqueId());
        if (duel == null) {
            return;
        }

        // Cancel default death broadcast
        event.deathMessage(null);

        // Ensure drops drop in the arena even if server world has keepInventory enabled
        if (event.getKeepInventory()) {
            event.setKeepInventory(false);
        }
        if (event.getKeepLevel()) {
            event.setKeepLevel(false);
        }

        // Register victim for automatic duels spawn respawn
        plugin.getDuelManager().addPendingRespawn(victim.getUniqueId());

        Player opponent = duel.getOpponent(victim.getUniqueId());

        // Paper respawn prompt bypass: automatically respawn the player after 1 tick
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (victim.isOnline() && victim.isDead()) {
                victim.spigot().respawn();
            }
        });

        if (opponent == null || !opponent.isOnline()) {
            plugin.getDuelManager().handleForfeit(victim);
            return;
        }

        // Check for simultaneous death / draw condition
        if (opponent.isDead() || opponent.getHealth() <= 0.0) {
            plugin.getDuelManager().addPendingRespawn(opponent.getUniqueId());
            plugin.getDuelManager().handleDraw(duel);
            return;
        }

        // Trigger defeat and loot transition
        plugin.getDuelManager().handlePlayerDefeat(duel, victim, opponent, false);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Duel duel = plugin.getDuelManager().getDuel(player.getUniqueId());
        if (duel == null) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Freeze player during countdown
        if (duel.getState() == DuelState.COUNTDOWN) {
            if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
                Location lockLoc = from.clone();
                lockLoc.setYaw(to.getYaw());
                lockLoc.setPitch(to.getPitch());
                event.setTo(lockLoc);
            }
            return;
        }

        // Boundary check during active duel
        if (duel.getState() == DuelState.ACTIVE) {
            Cuboid cuboid = duel.getArena().getCuboid();
            if (cuboid != null && !cuboid.contains(to)) {
                // Push back to from location
                event.setTo(from);
                fun.endcore.esduels.util.MessageUtil.sendMessage(player, "&cYou cannot leave the arena boundary!");
            }
        }
    }
}
