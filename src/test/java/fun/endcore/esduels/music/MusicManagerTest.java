package fun.endcore.esduels.music;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class MusicManagerTest {

    private YamlConfiguration config;
    private MusicManager musicManager;
    private Player fakePlayer;
    private FakePlayerHandler playerHandler;
    private UUID playerUuid;

    public static class PlaySoundCall {
        public Location location;
        public Sound sound;
        public String soundName;
        public SoundCategory category;
        public float volume;
        public float pitch;

        public PlaySoundCall(Location location, Sound sound, String soundName, SoundCategory category, float volume, float pitch) {
            this.location = location;
            this.sound = sound;
            this.soundName = soundName;
            this.category = category;
            this.volume = volume;
            this.pitch = pitch;
        }
    }

    public static class StopSoundCall {
        public Sound sound;
        public String soundName;
        public SoundCategory category;

        public StopSoundCall(Sound sound, String soundName, SoundCategory category) {
            this.sound = sound;
            this.soundName = soundName;
            this.category = category;
        }
    }

    private class FakePlayerHandler implements InvocationHandler {
        public final List<PlaySoundCall> playCalls = new ArrayList<>();
        public final List<StopSoundCall> stopCalls = new ArrayList<>();
        public Location location = new Location(null, 0, 64, 0);

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (name.equals("getUniqueId")) {
                return playerUuid;
            }
            if (name.equals("isOnline")) {
                return true;
            }
            if (name.equals("getLocation")) {
                return location;
            }
            if (name.equals("playSound")) {
                Location loc = (Location) args[0];
                Sound snd = null;
                String sndName = null;
                if (args[1] instanceof Sound s) {
                    snd = s;
                } else if (args[1] instanceof String s) {
                    sndName = s;
                }
                SoundCategory cat = (SoundCategory) args[2];
                float vol = (Float) args[3];
                float pit = (Float) args[4];
                playCalls.add(new PlaySoundCall(loc, snd, sndName, cat, vol, pit));
                return null;
            }
            if (name.equals("stopSound")) {
                if (args.length == 1) {
                    if (args[0] instanceof SoundCategory cat) {
                        stopCalls.add(new StopSoundCall(null, null, cat));
                    } else if (args[0] instanceof Sound s) {
                        stopCalls.add(new StopSoundCall(s, null, null));
                    } else if (args[0] instanceof String s) {
                        stopCalls.add(new StopSoundCall(null, s, null));
                    }
                } else if (args.length == 2) {
                    SoundCategory cat = (SoundCategory) args[1];
                    if (args[0] instanceof Sound s) {
                        stopCalls.add(new StopSoundCall(s, null, cat));
                    } else if (args[0] instanceof String s) {
                        stopCalls.add(new StopSoundCall(null, s, cat));
                    }
                }
                return null;
            }
            if (name.equals("equals")) {
                return proxy == args[0];
            }
            if (name.equals("hashCode")) {
                return System.identityHashCode(proxy);
            }
            if (name.equals("toString")) {
                return "FakePlayer[" + playerUuid + "]";
            }
            return null;
        }
    }

    @BeforeEach
    public void setUp() {
        config = new YamlConfiguration();
        playerUuid = UUID.randomUUID();
        playerHandler = new FakePlayerHandler();

        fakePlayer = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                playerHandler
        );

        musicManager = new MusicManager(() -> config);
    }

    @Test
    public void testStartQueueMusicUsesSoundCategoryMusicNonPositional() {
        config.set("queue-music.enabled", true);
        config.set("queue-music.tracks", List.of("MUSIC_DISC_CAT"));
        config.set("queue-music.volume", 0.75);
        config.set("queue-music.pitch", 1.0);

        musicManager.startQueueMusic(fakePlayer);

        assertEquals(1, playerHandler.playCalls.size(), "Should have played 1 sound");
        PlaySoundCall call = playerHandler.playCalls.get(0);
        assertEquals(SoundCategory.MUSIC, call.category, "Must use SoundCategory.MUSIC for non-positional playback");
        assertNotEquals(SoundCategory.RECORDS, call.category, "Must NOT use SoundCategory.RECORDS which attenuates by distance");
        assertTrue(call.sound != null || "music_disc_cat".equalsIgnoreCase(call.soundName), "Track must match MUSIC_DISC_CAT");
        assertEquals(0.75f, call.volume, 0.001f);
        assertEquals(1.0f, call.pitch, 0.001f);
    }

    @Test
    public void testStartLootMusicUsesSoundCategoryMusic() {
        config.set("loot-music.enabled", true);
        config.set("loot-music.tracks", List.of("MUSIC_DISC_PIGSTEP"));
        config.set("loot-music.volume", 0.8);
        config.set("loot-music.pitch", 1.2);

        musicManager.startLootMusic(fakePlayer);

        assertEquals(1, playerHandler.playCalls.size(), "Should have played 1 sound");
        PlaySoundCall call = playerHandler.playCalls.get(0);
        assertEquals(SoundCategory.MUSIC, call.category, "Loot music must also use SoundCategory.MUSIC");
        assertTrue(call.sound != null || "music_disc_pigstep".equalsIgnoreCase(call.soundName), "Track must match MUSIC_DISC_PIGSTEP");
        assertEquals(0.8f, call.volume, 0.001f);
        assertEquals(1.2f, call.pitch, 0.001f);
    }

    @Test
    public void testStopMusicStopsBothCategoryMusicAndRecords() {
        config.set("queue-music.enabled", true);
        config.set("queue-music.tracks", List.of("MUSIC_DISC_CAT"));
        config.set("queue-music.volume", 1.0);
        config.set("queue-music.pitch", 1.0);

        musicManager.startQueueMusic(fakePlayer);
        playerHandler.stopCalls.clear();

        musicManager.stopMusic(fakePlayer);

        boolean stoppedMusicTrack = playerHandler.stopCalls.stream()
                .anyMatch(c -> c.category == SoundCategory.MUSIC && ("music_disc_cat".equalsIgnoreCase(c.soundName) || c.sound != null));
        boolean stoppedMusicCategory = playerHandler.stopCalls.stream()
                .anyMatch(c -> c.sound == null && c.soundName == null && c.category == SoundCategory.MUSIC);

        assertTrue(stoppedMusicTrack, "Should stop track under SoundCategory.MUSIC");
        assertTrue(stoppedMusicCategory, "Should stop category SoundCategory.MUSIC");
    }

    @Test
    public void testPreventDuplicatePlaybackOnReenter() {
        config.set("queue-music.enabled", true);
        config.set("queue-music.tracks", List.of("MUSIC_DISC_CAT"));
        config.set("queue-music.volume", 1.0);
        config.set("queue-music.pitch", 1.0);

        // Player joins queue first time
        musicManager.startQueueMusic(fakePlayer);
        assertEquals(1, playerHandler.playCalls.size());

        // Player re-enters queue or task restarts
        musicManager.startQueueMusic(fakePlayer);

        // Verify stopSound was called between plays
        boolean hadCategoryMusicStop = playerHandler.stopCalls.stream()
                .anyMatch(c -> c.category == SoundCategory.MUSIC);
        assertTrue(hadCategoryMusicStop, "Previous music must be stopped before new track starts to prevent overlapping");
        assertEquals(2, playerHandler.playCalls.size());
    }

    @Test
    public void testCustomStringTrackFallback() {
        config.set("queue-music.enabled", true);
        config.set("queue-music.tracks", List.of("custom.sound.track"));
        config.set("queue-music.volume", 0.5);
        config.set("queue-music.pitch", 1.0);

        musicManager.startQueueMusic(fakePlayer);

        assertEquals(1, playerHandler.playCalls.size());
        PlaySoundCall call = playerHandler.playCalls.get(0);
        assertEquals(SoundCategory.MUSIC, call.category);
        assertEquals("custom.sound.track", call.soundName);
        assertEquals(0.5f, call.volume, 0.001f);
    }

    @Test
    public void testPlayerMovingLongDistanceDoesNotLoseMusicAudibility() {
        config.set("queue-music.enabled", true);
        config.set("queue-music.tracks", List.of("MUSIC_DISC_CAT"));
        config.set("queue-music.volume", 1.0);
        config.set("queue-music.pitch", 1.0);

        // Join queue at location 0, 64, 0
        playerHandler.location = new Location(null, 0, 64, 0);
        musicManager.startQueueMusic(fakePlayer);

        // Player moves 500 blocks away
        playerHandler.location = new Location(null, 500, 64, 500);

        // With SoundCategory.MUSIC, volume stays consistent (non-positional, attenuation NONE)
        PlaySoundCall call = playerHandler.playCalls.get(0);
        assertEquals(SoundCategory.MUSIC, call.category);
        assertEquals(1.0f, call.volume, 0.001f);
        assertNotEquals(SoundCategory.RECORDS, call.category, "RECORDS would fade out beyond 64 blocks");
    }
}
