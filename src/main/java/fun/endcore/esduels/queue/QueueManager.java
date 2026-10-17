package fun.endcore.esduels.queue;

import fun.endcore.esduels.ESDuels;
import fun.endcore.esduels.arena.Arena;
import fun.endcore.esduels.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe matchmaking queue manager and scheduler.
 */
public class QueueManager {
    private final ESDuels plugin;
    private final List<QueueEntry> queueList = Collections.synchronizedList(new ArrayList<>());
    private final Set<UUID> queuedUuids = ConcurrentHashMap.newKeySet();

    private org.bukkit.scheduler.BukkitTask matchmakerTask;

    public QueueManager(ESDuels plugin) {
        this.plugin = plugin;
        startMatchmakerTask();
    }

    /**
     * Adds a player to the matchmaking queue.
     */
    public synchronized boolean joinQueue(Player player) {
        if (player == null || !player.isOnline()) return false;
        if (isQueued(player.getUniqueId())) return false;
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) return false;
        if (plugin.getLootTimerManager().isInLootSession(player.getUniqueId())) return false;

        QueueEntry entry = new QueueEntry(player);
        queuedUuids.add(player.getUniqueId());
        queueList.add(entry);

        // Start waiting music
        plugin.getMusicManager().startQueueMusic(player);

        String msg = plugin.getConfigManager().getMessage("queue.joined");
        player.sendMessage(MessageUtil.toComponent(msg));

        // Immediately check if a match can be made
        checkAndMatch();
        return true;
    }

    /**
     * Removes a player from the matchmaking queue.
     */
    public synchronized boolean leaveQueue(Player player) {
        if (player == null) return false;
        return leaveQueue(player.getUniqueId(), true);
    }

    public synchronized boolean leaveQueue(UUID uuid, boolean notify) {
        if (!queuedUuids.remove(uuid)) {
            return false;
        }

        queueList.removeIf(entry -> entry.getUuid().equals(uuid));

        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            plugin.getMusicManager().stopMusic(player);
            if (notify) {
                String msg = plugin.getConfigManager().getMessage("queue.left");
                player.sendMessage(MessageUtil.toComponent(msg));
            }
        } else {
            plugin.getMusicManager().stopMusic(uuid);
        }
        return true;
    }

    public boolean isQueued(UUID uuid) {
        return queuedUuids.contains(uuid);
    }

    public int getQueueCount() {
        return queueList.size();
    }

    public String getEstimatedWaitTime() {
        int count = getQueueCount();
        if (count >= 2) {
            return "Instant";
        }
        return plugin.getConfigManager().getConfig().getString("queue.default-estimated-wait", "15s");
    }

    /**
     * Attempts to pair 2 eligible players and assign an available arena.
     */
    public synchronized void checkAndMatch() {
        if (queueList.size() < 2) {
            return;
        }

        Arena arena = plugin.getArenaManager().allocateAvailableArena();
        if (arena == null) {
            return; // No arena available right now; players stay queued
        }

        Player player1 = null;
        Player player2 = null;

        // Find two valid online players
        Iterator<QueueEntry> it = queueList.iterator();
        while (it.hasNext() && (player1 == null || player2 == null)) {
            QueueEntry entry = it.next();
            Player p = Bukkit.getPlayer(entry.getUuid());
            if (p == null || !p.isOnline() || plugin.getDuelManager().isInDuel(p.getUniqueId())) {
                it.remove();
                queuedUuids.remove(entry.getUuid());
                continue;
            }

            if (player1 == null) {
                player1 = p;
                it.remove();
                queuedUuids.remove(p.getUniqueId());
            } else if (!player1.getUniqueId().equals(p.getUniqueId())) {
                player2 = p;
                it.remove();
                queuedUuids.remove(p.getUniqueId());
            }
        }

        if (player1 != null && player2 != null) {
            // Stop waiting music
            plugin.getMusicManager().stopMusic(player1);
            plugin.getMusicManager().stopMusic(player2);

            // Launch duel!
            Player p1 = player1;
            Player p2 = player2;
            if (Bukkit.isPrimaryThread()) {
                plugin.getDuelManager().startDuel(arena, p1, p2);
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getDuelManager().startDuel(arena, p1, p2);
                });
            }
        } else {
            // If we couldn't find 2 players, release arena
            arena.setState(fun.endcore.esduels.arena.ArenaState.IDLE);
            if (player1 != null) {
                // Re-add player1 to the front of queue
                queuedUuids.add(player1.getUniqueId());
                queueList.add(0, new QueueEntry(player1));
            }
        }
    }

    private void startMatchmakerTask() {
        matchmakerTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkAndMatch();
            }
        }.runTaskTimer(plugin, 20L, 20L); // every 1 second
    }

    public void clearQueue() {
        for (UUID uuid : new ArrayList<>(queuedUuids)) {
            leaveQueue(uuid, false);
        }
    }

    public void shutdown() {
        if (matchmakerTask != null) {
            matchmakerTask.cancel();
            matchmakerTask = null;
        }
        clearQueue();
    }
}
