package fun.endcore.esduels.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility for parsing text into Adventure Components, sending titles, and action bars.
 */
public final class MessageUtil {
    private static final LegacyComponentSerializer SECTION_SERIALIZER = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private MessageUtil() {}

    /**
     * Converts a string with legacy or hex colors into an Adventure Component with italics explicitly disabled.
     */
    public static Component toComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty().decoration(TextDecoration.ITALIC, false);
        }
        String colorized = ColorUtil.colorize(text);
        Component deserialized = SECTION_SERIALIZER.deserialize(colorized);
        return disableItalics(deserialized);
    }

    /**
     * Converts a MiniMessage string into an Adventure Component with italics explicitly disabled.
     */
    public static Component fromMiniMessage(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty().decoration(TextDecoration.ITALIC, false);
        }
        return disableItalics(MiniMessage.miniMessage().deserialize(text));
    }

    /**
     * Recursively ensures that unintended italic decoration is explicitly disabled across all nodes,
     * while preserving intentional styling such as explicit italics (&o) or bold formatting.
     */
    public static Component disableItalics(Component component) {
        if (component == null) {
            return Component.empty().decoration(TextDecoration.ITALIC, false);
        }
        Component result = component;
        if (result.decoration(TextDecoration.ITALIC) != TextDecoration.State.TRUE) {
            result = result.decoration(TextDecoration.ITALIC, false);
        }
        if (!result.children().isEmpty()) {
            List<Component> updatedChildren = new ArrayList<>(result.children().size());
            for (Component child : result.children()) {
                updatedChildren.add(disableItalics(child));
            }
            result = result.children(updatedChildren);
        }
        return result;
    }

    /**
     * Sends a colorized message to a CommandSender.
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null || message.isEmpty()) {
            return;
        }
        sender.sendMessage(toComponent(message));
    }

    /**
     * Sends an action bar message to a Player.
     */
    public static void sendActionBar(Player player, String message) {
        if (player == null || !player.isOnline() || message == null || message.isEmpty()) {
            return;
        }
        player.sendActionBar(toComponent(message));
    }

    /**
     * Sends a Title and Subtitle to a Player.
     */
    public static void sendTitle(Player player, String titleText, String subtitleText, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (player == null || !player.isOnline()) {
            return;
        }
        Component titleComponent = toComponent(titleText);
        Component subComponent = toComponent(subtitleText);

        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeInTicks * 50L),
                Duration.ofMillis(stayTicks * 50L),
                Duration.ofMillis(fadeOutTicks * 50L)
        );
        player.showTitle(Title.title(titleComponent, subComponent, times));
    }
}
