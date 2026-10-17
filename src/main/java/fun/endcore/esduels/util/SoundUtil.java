package fun.endcore.esduels.util;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

/**
 * Utility for safely playing native Minecraft sounds without throwing runtime exceptions.
 */
public final class SoundUtil {

    private SoundUtil() {}

    /**
     * Plays a sound for a specific player by sound name.
     */
    public static void playSound(Player player, String soundName, float volume, float pitch) {
        if (player == null || !player.isOnline() || soundName == null || soundName.isEmpty()) {
            return;
        }

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, SoundCategory.MASTER, volume, pitch);
        } catch (IllegalArgumentException e) {
            // In case sound enum differs across minor versions, attempt playback by raw key
            try {
                player.playSound(player.getLocation(), soundName.toLowerCase(), SoundCategory.MASTER, volume, pitch);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Plays a sound at a location for all nearby players.
     */
    public static void playSound(Location location, String soundName, float volume, float pitch) {
        if (location == null || location.getWorld() == null || soundName == null || soundName.isEmpty()) {
            return;
        }

        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            location.getWorld().playSound(location, sound, SoundCategory.MASTER, volume, pitch);
        } catch (IllegalArgumentException e) {
            try {
                location.getWorld().playSound(location, soundName.toLowerCase(), SoundCategory.MASTER, volume, pitch);
            } catch (Exception ignored) {
            }
        }
    }
}
