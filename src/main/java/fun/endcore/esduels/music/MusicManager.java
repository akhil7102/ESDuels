package fun.endcore.esduels.music;

import fun.endcore.esduels.ESDuels;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages native Minecraft music playback for queue waiting and post-duel loot collection.
 */
public class MusicManager {
    private final ESDuels plugin;
    private final java.util.function.Supplier<org.bukkit.configuration.file.FileConfiguration> configSupplier;
    private final Random random = new Random();
    private final Map<UUID, String> activeTracks = new ConcurrentHashMap<>();

    public MusicManager(ESDuels plugin) {
        this.plugin = plugin;
        this.configSupplier = () -> plugin.getConfigManager().getConfig();
    }

    MusicManager(java.util.function.Supplier<org.bukkit.configuration.file.FileConfiguration> configSupplier) {
        this.plugin = null;
        this.configSupplier = configSupplier;
    }

    private org.bukkit.configuration.file.FileConfiguration getConfig() {
        return configSupplier.get();
    }

    /**
     * Plays a random waiting music track for a queued player.
     */
    public void startQueueMusic(Player player) {
        if (!getConfig().getBoolean("queue-music.enabled", true)) {
            return;
        }
        List<String> tracks = getConfig().getStringList("queue-music.tracks");
        if (tracks.isEmpty()) {
            return;
        }

        stopMusic(player);

        String track = tracks.get(random.nextInt(tracks.size()));
        playTrack(player, track,
                (float) getConfig().getDouble("queue-music.volume", 1.0),
                (float) getConfig().getDouble("queue-music.pitch", 1.0));
    }

    /**
     * Plays a random loot period music track for the winning player.
     */
    public void startLootMusic(Player player) {
        if (!getConfig().getBoolean("loot-music.enabled", true)) {
            return;
        }
        List<String> tracks = getConfig().getStringList("loot-music.tracks");
        if (tracks.isEmpty()) {
            return;
        }

        stopMusic(player);

        String track = tracks.get(random.nextInt(tracks.size()));
        playTrack(player, track,
                (float) getConfig().getDouble("loot-music.volume", 1.0),
                (float) getConfig().getDouble("loot-music.pitch", 1.0));
    }

    private void playTrack(Player player, String trackName, float volume, float pitch) {
        if (player == null || !player.isOnline()) {
            return;
        }

        activeTracks.put(player.getUniqueId(), trackName);

        try {
            Sound sound = Sound.valueOf(trackName.toUpperCase());
            player.playSound(player.getLocation(), sound, SoundCategory.MUSIC, volume, pitch);
        } catch (Throwable t) {
            try {
                player.playSound(player.getLocation(), trackName.toLowerCase(), SoundCategory.MUSIC, volume, pitch);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Immediately stops any currently playing music for a player.
     */
    public void stopMusic(Player player) {
        if (player == null) {
            return;
        }

        String track = activeTracks.remove(player.getUniqueId());
        if (player.isOnline()) {
            if (track != null) {
                try {
                    Sound sound = Sound.valueOf(track.toUpperCase());
                    player.stopSound(sound, SoundCategory.MUSIC);
                    player.stopSound(sound, SoundCategory.RECORDS);
                } catch (Throwable t) {
                    try {
                        player.stopSound(track.toLowerCase(), SoundCategory.MUSIC);
                        player.stopSound(track.toLowerCase(), SoundCategory.RECORDS);
                    } catch (Exception ignored) {
                    }
                }
            }
            // Ensure all music category (and records) sounds are stopped for safety
            try {
                player.stopSound(SoundCategory.MUSIC);
            } catch (Exception ignored) {
            }
            try {
                player.stopSound(SoundCategory.RECORDS);
            } catch (Exception ignored) {
            }
        }
    }

    public void stopMusic(UUID uuid) {
        if (uuid == null) {
            return;
        }
        activeTracks.remove(uuid);
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            stopMusic(player);
        }
    }

    public void clearAll() {
        for (UUID uuid : activeTracks.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline()) {
                stopMusic(player);
            }
        }
        activeTracks.clear();
    }
}
