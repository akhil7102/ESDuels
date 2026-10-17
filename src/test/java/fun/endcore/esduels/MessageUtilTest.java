package fun.endcore.esduels;

import fun.endcore.esduels.util.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MessageUtilTest {

    private boolean anyNodeHasItalic(Component component) {
        if (component.decoration(TextDecoration.ITALIC) == TextDecoration.State.TRUE) {
            return true;
        }
        for (Component child : component.children()) {
            if (anyNodeHasItalic(child)) {
                return true;
            }
        }
        return false;
    }

    private boolean allNodesExplicitlyDisableItalics(Component component) {
        if (component.decoration(TextDecoration.ITALIC) != TextDecoration.State.FALSE) {
            return false;
        }
        for (Component child : component.children()) {
            if (!allNodesExplicitlyDisableItalics(child)) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void testToComponentDisablesItalicsOnAllNodes() {
        Component comp = MessageUtil.toComponent("&aHello &bWorld &cTest");

        // Verify no node is italic
        assertFalse(anyNodeHasItalic(comp), "No node should be formatted in italics");
        // Verify all nodes explicitly have ITALIC set to FALSE
        assertTrue(allNodesExplicitlyDisableItalics(comp), "All nodes must explicitly disable italics");
    }

    @Test
    public void testToComponentPreservesColorsAndBold() {
        Component comp = MessageUtil.toComponent("&#33A2FF&lBOLD TEXT &eYellow");

        // Italics must be explicitly disabled
        assertFalse(anyNodeHasItalic(comp));
        assertTrue(allNodesExplicitlyDisableItalics(comp));

        // Bold formatting must be preserved
        assertTrue(comp.hasDecoration(TextDecoration.BOLD) || comp.children().stream().anyMatch(c -> c.hasDecoration(TextDecoration.BOLD)),
                "Bold decoration must be preserved");
    }

    @Test
    public void testToComponentPreservesIntentionalItalics() {
        Component comp = MessageUtil.toComponent("&oIntentional Italic");
        assertTrue(anyNodeHasItalic(comp), "Intentional italic code (&o) must be preserved");
    }

    @Test
    public void testNullAndEmptyTextDisablesItalics() {
        Component emptyComp = MessageUtil.toComponent("");
        assertEquals(TextDecoration.State.FALSE, emptyComp.decoration(TextDecoration.ITALIC));

        Component nullComp = MessageUtil.toComponent(null);
        assertEquals(TextDecoration.State.FALSE, nullComp.decoration(TextDecoration.ITALIC));
    }

    @Test
    public void testMiniMessageDisablesItalics() {
        Component comp = MessageUtil.fromMiniMessage("<green>Normal text <bold>bold text</bold></green>");
        assertFalse(anyNodeHasItalic(comp));
        assertTrue(allNodesExplicitlyDisableItalics(comp));
    }

    @Test
    public void testGuiTitleAndItemLoreDoNotHaveItalics() {
        String guiTitle = "QUEUE CONFIRM";
        Component titleComp = MessageUtil.toComponent(guiTitle);
        assertTrue(allNodesExplicitlyDisableItalics(titleComp));

        String itemName = "&aJoin Queue";
        Component nameComp = MessageUtil.toComponent(itemName);
        assertTrue(allNodesExplicitlyDisableItalics(nameComp));

        List<String> lore = List.of("&7Waiting: &f5", "&7Estimated Wait: &f15s", "&7Status: &eClick green pane to join");
        for (String line : lore) {
            Component loreComp = MessageUtil.toComponent(line);
            assertTrue(allNodesExplicitlyDisableItalics(loreComp), "Lore line must not have italics: " + line);
        }
    }

    @Test
    public void testBossBarAndActionBarsDoNotHaveItalics() {
        String bossBarText = "&6&lLoot Collection &8- &e60s remaining";
        Component bossBarComp = MessageUtil.toComponent(bossBarText);
        assertTrue(allNodesExplicitlyDisableItalics(bossBarComp));

        String actionBarText = "&6&lLoot Timer: &e60s &7| &fCollect drops before returning to spawn!";
        Component actionComp = MessageUtil.toComponent(actionBarText);
        assertTrue(allNodesExplicitlyDisableItalics(actionComp));
    }
}
