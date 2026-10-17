package fun.endcore.esduels.placeholder;

import fun.endcore.esduels.ESDuels;
import fun.endcore.esduels.stats.PlayerStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * PlaceholderAPI expansion for ESDuels statistics and queue metrics.
 */
public class ESDuelsExpansion extends PlaceholderExpansion {
    private final ESDuels plugin;

    public ESDuelsExpansion(ESDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "esduels";
    }

    @Override
    public @NotNull String getAuthor() {
        return "AKHILPLAYZYT";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("queued_players")) {
            return String.valueOf(plugin.getQueueManager().getQueueCount());
        }

        if (player == null) {
            return "";
        }

        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());

        return switch (params.toLowerCase(Locale.ROOT)) {
            case "wins" -> String.valueOf(stats.getWins());
            case "losses" -> String.valueOf(stats.getLosses());
            case "streak" -> String.valueOf(stats.getStreak());
            case "best_streak" -> String.valueOf(stats.getBestStreak());
            case "win_rate" -> String.format(Locale.ROOT, "%.1f%%", stats.getWinRate());
            case "in_duel" -> String.valueOf(plugin.getDuelManager().isInDuel(player.getUniqueId()));
            case "in_queue" -> String.valueOf(plugin.getQueueManager().isQueued(player.getUniqueId()));
            default -> null;
        };
    }
}
