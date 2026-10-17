package fun.endcore.esduels.loot;

import fun.endcore.esduels.ESDuels;
import fun.endcore.esduels.duel.Duel;
import fun.endcore.esduels.util.MessageUtil;
import fun.endcore.esduels.util.SoundUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages the 60-second post-duel loot collection timer, BossBar, Action Bar, and loot music.
 */
public class LootTimerManager {
    private final ESDuels plugin;
    private final Map<UUID, ActiveLootSession> activeSessions = new ConcurrentHashMap<>();

    public LootTimerManager(ESDuels plugin) {
        this.plugin = plugin;
    }

    private static class ActiveLootSession {
        private final Duel duel;
        private final Player winner;
        private final BossBar bossBar;
        private BukkitTask task;
        private int remainingSeconds;
        private final int totalSeconds;

        public ActiveLootSession(Duel duel, Player winner, BossBar bossBar, int totalSeconds) {
            this.duel = duel;
            this.winner = winner;
            this.bossBar = bossBar;
            this.totalSeconds = totalSeconds;
            this.remainingSeconds = totalSeconds;
        }
    }

    /**
     * Starts the loot timer countdown for the winner.
     */
    public void startLootTimer(Duel duel, Player winner, Consumer<Duel> onFinish) {
        if (winner == null || !winner.isOnline()) {
            if (onFinish != null) onFinish.accept(duel);
            return;
        }

        int totalSeconds = plugin.getConfigManager().getConfig().getInt("loot-timer-seconds", 60);

        // Start loot music
        plugin.getMusicManager().startLootMusic(winner);

        // Initialize BossBar
        BossBar bossBar = BossBar.bossBar(
                Component.empty().decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false),
                1.0f,
                BossBar.Color.YELLOW,
                BossBar.Overlay.PROGRESS
        );
        winner.showBossBar(bossBar);

        ActiveLootSession session = new ActiveLootSession(duel, winner, bossBar, totalSeconds);
        activeSessions.put(winner.getUniqueId(), session);

        session.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!winner.isOnline()) {
                    cancel();
                    endLootSession(winner.getUniqueId(), false);
                    if (onFinish != null) onFinish.accept(duel);
                    return;
                }

                if (session.remainingSeconds <= 0) {
                    cancel();
                    endLootSession(winner.getUniqueId(), true);
                    if (onFinish != null) onFinish.accept(duel);
                    return;
                }

                // Update BossBar and Action Bar
                float progress = Math.max(0.0f, Math.min(1.0f, (float) session.remainingSeconds / (float) session.totalSeconds));
                bossBar.progress(progress);

                String barTitle = plugin.getConfigManager().getMessage("loot.bossbar")
                        .replace("{TIME}", String.valueOf(session.remainingSeconds));
                bossBar.name(MessageUtil.toComponent(barTitle));

                String actionText = plugin.getConfigManager().getMessage("loot.actionbar")
                        .replace("{TIME}", String.valueOf(session.remainingSeconds));
                MessageUtil.sendActionBar(winner, actionText);

                // Warning sound on last 5 seconds
                if (session.remainingSeconds <= 5) {
                    String warningSound = plugin.getConfigManager().getConfig().getString("sounds.loot-timer-warning", "BLOCK_NOTE_BLOCK_BELL");
                    SoundUtil.playSound(winner, warningSound, 0.8f, 1.2f);
                }

                session.remainingSeconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // every 1 second
    }

    /**
     * Terminates a loot session early (e.g. player left or timer expired).
     */
    public void endLootSession(UUID winnerUuid, boolean teleportToSpawn) {
        ActiveLootSession session = activeSessions.remove(winnerUuid);
        if (session != null) {
            if (session.task != null) {
                session.task.cancel();
            }

            Player winner = session.winner;
            if (winner != null && winner.isOnline()) {
                winner.hideBossBar(session.bossBar);
                plugin.getMusicManager().stopMusic(winner);

                if (teleportToSpawn) {
                    plugin.getSpawnManager().teleportToSpawn(winner);
                    String endedMsg = plugin.getConfigManager().getMessage("loot.ended");
                    winner.sendMessage(MessageUtil.toComponent(endedMsg));
                }
            }
        }
    }

    public boolean isInLootSession(UUID uuid) {
        return activeSessions.containsKey(uuid);
    }

    public void cancelAll() {
        for (UUID uuid : activeSessions.keySet()) {
            endLootSession(uuid, true);
        }
    }
}
