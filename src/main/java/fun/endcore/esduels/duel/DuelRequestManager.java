package fun.endcore.esduels.duel;

import fun.endcore.esduels.ESDuels;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages direct duel challenges (/duel send, /duel accept, /duel deny).
 */
public class DuelRequestManager {
    private final ESDuels plugin;
    // Map of target UUID -> List of requests directed to them
    private final Map<UUID, List<DuelRequest>> pendingRequests = new ConcurrentHashMap<>();

    private org.bukkit.scheduler.BukkitTask cleanupTask;

    public DuelRequestManager(ESDuels plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    public void sendRequest(Player sender, Player target) {
        List<DuelRequest> list = pendingRequests.computeIfAbsent(target.getUniqueId(), k -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            // Remove any existing request from this sender to prevent duplicate accumulation
            list.removeIf(req -> req.getSender().equals(sender.getUniqueId()));
            list.add(new DuelRequest(sender.getUniqueId(), target.getUniqueId()));
        }
    }

    public DuelRequest getRequest(UUID target, UUID sender) {
        List<DuelRequest> list = pendingRequests.get(target);
        if (list == null) return null;
        synchronized (list) {
            for (DuelRequest req : list) {
                if (req.getSender().equals(sender)) {
                    return req;
                }
            }
        }
        return null;
    }

    public boolean removeRequest(UUID target, UUID sender) {
        List<DuelRequest> list = pendingRequests.get(target);
        if (list == null) return false;
        synchronized (list) {
            return list.removeIf(req -> req.getSender().equals(sender));
        }
    }

    public void cancelAllRequestsFrom(UUID sender) {
        for (List<DuelRequest> list : pendingRequests.values()) {
            synchronized (list) {
                list.removeIf(req -> req.getSender().equals(sender));
            }
        }
    }

    public void removeAllForPlayer(UUID uuid) {
        pendingRequests.remove(uuid);
        cancelAllRequestsFrom(uuid);
    }

    private void startCleanupTask() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                long expirationSeconds = plugin.getConfigManager().getConfig().getLong("request-expiration-seconds", 60);
                for (Map.Entry<UUID, List<DuelRequest>> entry : pendingRequests.entrySet()) {
                    List<DuelRequest> list = entry.getValue();
                    synchronized (list) {
                        list.removeIf(req -> {
                            if (req.isExpired(expirationSeconds)) {
                                Player sender = Bukkit.getPlayer(req.getSender());
                                if (sender != null && sender.isOnline()) {
                                    String msg = plugin.getConfigManager().getMessage("challenge.expired")
                                            .replace("{SENDER}", sender.getName());
                                    sender.sendMessage(fun.endcore.esduels.util.MessageUtil.toComponent(msg));
                                }
                                return true;
                            }
                            return false;
                        });
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L); // every 5 seconds
    }

    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
        pendingRequests.clear();
    }
}
