package fun.endcore.esduels.duel;

import fun.endcore.esduels.ESDuels;
import fun.endcore.esduels.arena.Arena;
import fun.endcore.esduels.arena.ArenaState;
import fun.endcore.esduels.util.MessageUtil;
import fun.endcore.esduels.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active duels, combat transitions, defeat/victory handling, and arena cleanup triggers.
 */
public class DuelManager {
    private final ESDuels plugin;
    private final Map<UUID, Duel> activePlayerDuels = new ConcurrentHashMap<>();
    private final Map<Arena, Duel> activeArenaDuels = new ConcurrentHashMap<>();
    private final java.util.Set<UUID> pendingSpawnRespawns = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public DuelManager(ESDuels plugin) {
        this.plugin = plugin;
    }

    public boolean isInDuel(UUID uuid) {
        return activePlayerDuels.containsKey(uuid);
    }

    public Duel getDuel(UUID uuid) {
        return activePlayerDuels.get(uuid);
    }

    public Duel getDuelByArena(Arena arena) {
        return activeArenaDuels.get(arena);
    }

    public void addPendingRespawn(UUID uuid) {
        if (uuid != null) {
            pendingSpawnRespawns.add(uuid);
        }
    }

    public boolean consumePendingRespawn(UUID uuid) {
        return uuid != null && pendingSpawnRespawns.remove(uuid);
    }

    public boolean hasPendingRespawn(UUID uuid) {
        return uuid != null && pendingSpawnRespawns.contains(uuid);
    }

    /**
     * Prepares and starts a duel match between two players in a reserved arena.
     */
    public void startDuel(Arena arena, Player player1, Player player2) {
        Duel duel = new Duel(arena, player1, player2);
        activePlayerDuels.put(player1.getUniqueId(), duel);
        activePlayerDuels.put(player2.getUniqueId(), duel);
        activeArenaDuels.put(arena, duel);

        // Stop any queue or waiting music for both participants
        plugin.getMusicManager().stopMusic(player1);
        plugin.getMusicManager().stopMusic(player2);

        arena.setState(ArenaState.COUNTDOWN);

        // Pre-duel inventory backups
        plugin.getBackupManager().createBackup(player1);
        plugin.getBackupManager().createBackup(player2);

        // Teleport players to arena spawn positions
        player1.teleport(arena.getPos1());
        player2.teleport(arena.getPos2());

        // Prepare player state for combat without altering inventory items
        preparePlayer(player1);
        preparePlayer(player2);

        // Notify players of match found
        String matchFoundSound = plugin.getConfigManager().getConfig().getString("sounds.match-found", "ENTITY_PLAYER_LEVELUP");
        SoundUtil.playSound(player1, matchFoundSound, 1.0f, 1.0f);
        SoundUtil.playSound(player2, matchFoundSound, 1.0f, 1.0f);

        String matchMsgP1 = plugin.getConfigManager().getMessage("duel.match-found").replace("{OPPONENT}", player2.getName());
        String matchMsgP2 = plugin.getConfigManager().getMessage("duel.match-found").replace("{OPPONENT}", player1.getName());
        player1.sendMessage(MessageUtil.toComponent(matchMsgP1));
        player2.sendMessage(MessageUtil.toComponent(matchMsgP2));

        // Start pre-duel countdown
        startCountdown(duel);
    }

    private void preparePlayer(Player player) {
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setFireTicks(0);
        player.setFallDistance(0);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
    }

    private void startCountdown(Duel duel) {
        int countdownTotal = plugin.getConfigManager().getConfig().getInt("countdown-seconds", 5);

        org.bukkit.scheduler.BukkitTask task = new BukkitRunnable() {
            private int count = countdownTotal;

            @Override
            public void run() {
                if (duel.getState() != DuelState.COUNTDOWN) {
                    cancel();
                    return;
                }

                Player p1 = duel.getPlayer1();
                Player p2 = duel.getPlayer2();

                // If either player quit during countdown
                if (p1 == null || !p1.isOnline() || p2 == null || !p2.isOnline()) {
                    cancel();
                    handleCountdownDisconnection(duel);
                    return;
                }

                if (count > 0) {
                    String title = plugin.getConfigManager().getMessage("duel.countdown-title").replace("{COUNT}", String.valueOf(count));
                    String subtitle = plugin.getConfigManager().getMessage("duel.countdown-subtitle").replace("{COUNT}", String.valueOf(count));
                    MessageUtil.sendTitle(p1, title, subtitle, 0, 25, 5);
                    MessageUtil.sendTitle(p2, title, subtitle, 0, 25, 5);

                    String tickSound = plugin.getConfigManager().getConfig().getString("sounds.countdown-tick", "BLOCK_NOTE_BLOCK_PLING");
                    SoundUtil.playSound(p1, tickSound, 1.0f, 1.0f);
                    SoundUtil.playSound(p2, tickSound, 1.0f, 1.0f);

                    count--;
                } else {
                    cancel();
                    // Begin active combat
                    duel.setState(DuelState.ACTIVE);
                    duel.getArena().setState(ArenaState.ACTIVE);

                    String startTitle = plugin.getConfigManager().getMessage("duel.start-title");
                    String startSub = plugin.getConfigManager().getMessage("duel.start-subtitle");
                    MessageUtil.sendTitle(p1, startTitle, startSub, 5, 30, 10);
                    MessageUtil.sendTitle(p2, startTitle, startSub, 5, 30, 10);

                    String startSound = plugin.getConfigManager().getConfig().getString("sounds.duel-start", "ENTITY_ENDER_DRAGON_GROWL");
                    SoundUtil.playSound(p1, startSound, 1.0f, 1.2f);
                    SoundUtil.playSound(p2, startSound, 1.0f, 1.2f);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        duel.setCountdownTask(task);
    }

    private void handleCountdownDisconnection(Duel duel) {
        Player p1 = duel.getPlayer1();
        Player p2 = duel.getPlayer2();

        if (p1 != null && p1.isOnline()) {
            if (p1.isDead()) {
                addPendingRespawn(p1.getUniqueId());
            } else {
                plugin.getSpawnManager().teleportToSpawn(p1);
            }
            plugin.getBackupManager().clearBackup(p1.getUniqueId());
        }
        if (p2 != null && p2.isOnline()) {
            if (p2.isDead()) {
                addPendingRespawn(p2.getUniqueId());
            } else {
                plugin.getSpawnManager().teleportToSpawn(p2);
            }
            plugin.getBackupManager().clearBackup(p2.getUniqueId());
        }

        cleanupDuelState(duel);
        triggerArenaRestoration(duel.getArena());
    }

    /**
     * Handles death or forfeit of a participant.
     */
    public synchronized void handlePlayerDefeat(Duel duel, Player loser, Player winner, boolean isForfeit) {
        if (duel.getState() != DuelState.ACTIVE && duel.getState() != DuelState.COUNTDOWN) {
            return;
        }

        duel.setState(DuelState.LOOTING);
        duel.getArena().setState(ArenaState.LOOTING);
        duel.setWinnerUuid(winner.getUniqueId());
        duel.setLoserUuid(loser.getUniqueId());

        // Update statistics
        plugin.getStatsManager().recordDuelResult(winner.getUniqueId(), loser.getUniqueId());

        // Clear backup records (legitimate duel outcome)
        plugin.getBackupManager().clearBackup(winner.getUniqueId());
        plugin.getBackupManager().clearBackup(loser.getUniqueId());

        // Titles and announcements
        String vicTitle = plugin.getConfigManager().getMessage("duel.victory-title");
        String vicSub = plugin.getConfigManager().getMessage("duel.victory-subtitle").replace("{LOSER}", loser.getName());
        MessageUtil.sendTitle(winner, vicTitle, vicSub, 5, 60, 15);

        String defTitle = plugin.getConfigManager().getMessage("duel.defeat-title");
        String defSub = plugin.getConfigManager().getMessage("duel.defeat-subtitle").replace("{WINNER}", winner.getName());
        MessageUtil.sendTitle(loser, defTitle, defSub, 5, 60, 15);

        String fanfare = plugin.getConfigManager().getConfig().getString("sounds.winner-fanfare", "UI_TOAST_CHALLENGE_COMPLETE");
        SoundUtil.playSound(winner, fanfare, 1.0f, 1.0f);

        String broadcastMsg = plugin.getConfigManager().getMessage("duel.broadcast-victory")
                .replace("{WINNER}", winner.getName())
                .replace("{LOSER}", loser.getName());
        Bukkit.broadcast(MessageUtil.toComponent(broadcastMsg));

        if (isForfeit) {
            String forfeitMsg = plugin.getConfigManager().getMessage("duel.forfeit").replace("{PLAYER}", loser.getName());
            winner.sendMessage(MessageUtil.toComponent(forfeitMsg));
        }

        // Teleport loser immediately to main spawn if alive, or queue for respawn event if dead
        if (loser.isOnline()) {
            if (loser.isDead()) {
                addPendingRespawn(loser.getUniqueId());
            } else {
                plugin.getSpawnManager().teleportToSpawn(loser);
            }
            String loserMsg = plugin.getConfigManager().getMessage("loot.loser-teleported");
            loser.sendMessage(MessageUtil.toComponent(loserMsg));
        }

        // Unregister loser from active duel so loser can interact / do things outside
        activePlayerDuels.remove(loser.getUniqueId());

        // Start Phase E: Post-duel loot period for the winner
        plugin.getLootTimerManager().startLootTimer(duel, winner, d -> {
            cleanupDuelState(d);
            triggerArenaRestoration(d.getArena());
        });
    }

    /**
     * Handles simultaneous deaths (draw outcome).
     */
    public synchronized void handleDraw(Duel duel) {
        if (duel.getState() != DuelState.ACTIVE && duel.getState() != DuelState.COUNTDOWN) {
            return;
        }

        duel.setState(DuelState.FINISHED);
        duel.getArena().setState(ArenaState.RESTORING);

        Player p1 = duel.getPlayer1();
        Player p2 = duel.getPlayer2();

        if (duel.getPlayer1Uuid() != null) {
            addPendingRespawn(duel.getPlayer1Uuid());
            plugin.getBackupManager().clearBackup(duel.getPlayer1Uuid());
        }
        if (duel.getPlayer2Uuid() != null) {
            addPendingRespawn(duel.getPlayer2Uuid());
            plugin.getBackupManager().clearBackup(duel.getPlayer2Uuid());
        }

        String drawMsg = plugin.getConfigManager().getMessage("duel.draw", "{PREFIX}&eThe duel ended in a draw! Both warriors fell in battle.");
        if (p1 != null && p1.isOnline()) p1.sendMessage(MessageUtil.toComponent(drawMsg));
        if (p2 != null && p2.isOnline()) p2.sendMessage(MessageUtil.toComponent(drawMsg));

        cleanupDuelState(duel);
        triggerArenaRestoration(duel.getArena());
    }

    /**
     * Handles when a player forfeits mid-combat or runs /duels leave.
     */
    public void handleForfeit(Player player) {
        Duel duel = getDuel(player.getUniqueId());
        if (duel == null) return;

        if (duel.getState() == DuelState.LOOTING) {
            // If winner chooses to leave loot period early
            if (duel.getWinnerUuid() != null && duel.getWinnerUuid().equals(player.getUniqueId())) {
                plugin.getLootTimerManager().endLootSession(player.getUniqueId(), true);
                cleanupDuelState(duel);
                triggerArenaRestoration(duel.getArena());
            }
            return;
        }

        // Cancel countdown task if still running
        if (duel.getCountdownTask() != null) {
            duel.getCountdownTask().cancel();
        }

        Player opponent = duel.getOpponent(player.getUniqueId());
        if (opponent != null && opponent.isOnline()) {
            // Drop player's inventory as legitimate loot at their location
            Location dropLoc = player.getLocation();
            if (dropLoc.getWorld() != null) {
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && !item.getType().isAir()) {
                        dropLoc.getWorld().dropItemNaturally(dropLoc, item);
                    }
                }
                for (ItemStack item : player.getInventory().getArmorContents()) {
                    if (item != null && !item.getType().isAir()) {
                        dropLoc.getWorld().dropItemNaturally(dropLoc, item);
                    }
                }
                ItemStack offhand = player.getInventory().getItemInOffHand();
                if (offhand != null && !offhand.getType().isAir()) {
                    dropLoc.getWorld().dropItemNaturally(dropLoc, offhand);
                }
            }

            // Wipe entire inventory, armor, offhand, and experience
            player.getInventory().clear();
            player.getInventory().setArmorContents(new ItemStack[4]);
            player.getInventory().setItemInOffHand(null);
            player.setTotalExperience(0);
            player.setExp(0);
            player.setLevel(0);

            handlePlayerDefeat(duel, player, opponent, true);
        } else {
            // Both gone or exceptional state
            cleanupDuelState(duel);
            triggerArenaRestoration(duel.getArena());
        }
    }

    /**
     * Handles when a player disconnects/leaves the server while inside an arena duel.
     * The player dies at their location in the arena, drops all items/armor/offhand as loot,
     * the opponent is awarded victory and gets the loot collection period,
     * and the leaver is registered so when they reconnect they are confirmed dead at spawn with no loot.
     */
    public synchronized void handleDisconnectInDuel(Player player) {
        Duel duel = getDuel(player.getUniqueId());
        if (duel == null) return;

        if (duel.getState() == DuelState.LOOTING) {
            return;
        }

        // Cancel countdown task if still running
        if (duel.getCountdownTask() != null) {
            duel.getCountdownTask().cancel();
        }

        UUID loserUuid = player.getUniqueId();
        // Register combat logger for persistent death penalty upon reconnecting
        plugin.getCombatLogManager().addCombatLogger(loserUuid);

        // Drop player's inventory, armor, and offhand as legitimate loot at their location in the arena
        Location dropLoc = player.getLocation();
        if (dropLoc.getWorld() != null) {
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    dropLoc.getWorld().dropItemNaturally(dropLoc, item);
                }
            }
            for (ItemStack item : player.getInventory().getArmorContents()) {
                if (item != null && !item.getType().isAir()) {
                    dropLoc.getWorld().dropItemNaturally(dropLoc, item);
                }
            }
            ItemStack offhand = player.getInventory().getItemInOffHand();
            if (offhand != null && !offhand.getType().isAir()) {
                dropLoc.getWorld().dropItemNaturally(dropLoc, offhand);
            }

            // Death visual and audio cues in the arena
            dropLoc.getWorld().strikeLightningEffect(dropLoc);
            dropLoc.getWorld().playSound(dropLoc, org.bukkit.Sound.ENTITY_PLAYER_DEATH, 1.0f, 1.0f);
        }

        // Wipe inventory completely
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);
        player.setTotalExperience(0);
        player.setExp(0);
        player.setLevel(0);
        try {
            player.setHealth(0.0);
        } catch (Exception ignored) {
        }

        // Ensure saved player position points to spawn
        plugin.getSpawnManager().teleportToSpawn(player);

        Player opponent = duel.getOpponent(loserUuid);
        if (opponent != null && opponent.isOnline()) {
            handlePlayerDefeat(duel, player, opponent, true);
        } else {
            cleanupDuelState(duel);
            triggerArenaRestoration(duel.getArena());
        }
    }

    public void triggerArenaRestoration(Arena arena) {
        arena.setState(ArenaState.RESTORING);
        activeArenaDuels.remove(arena);

        plugin.getArenaRestorer().restoreArena(arena, success -> {
            if (success) {
                arena.setState(ArenaState.IDLE);
                // Immediately check if queued players can use this arena!
                plugin.getQueueManager().checkAndMatch();
            } else {
                arena.setState(ArenaState.INCOMPLETE);
                plugin.getLogger().severe("Arena " + arena.getName() + " marked INCOMPLETE due to restoration failure.");
            }
        });
    }

    public void cleanupDuelState(Duel duel) {
        if (duel.getPlayer1Uuid() != null) activePlayerDuels.remove(duel.getPlayer1Uuid());
        if (duel.getPlayer2Uuid() != null) activePlayerDuels.remove(duel.getPlayer2Uuid());
        activeArenaDuels.remove(duel.getArena());
        duel.setState(DuelState.FINISHED);
    }

    public void cancelAll() {
        for (Duel duel : activeArenaDuels.values()) {
            handleCountdownDisconnection(duel);
        }
        activePlayerDuels.clear();
        activeArenaDuels.clear();
        pendingSpawnRespawns.clear();
    }
}
