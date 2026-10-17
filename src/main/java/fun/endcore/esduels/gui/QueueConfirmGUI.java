package fun.endcore.esduels.gui;

import fun.endcore.esduels.ESDuels;
import fun.endcore.esduels.stats.PlayerStats;
import fun.endcore.esduels.util.ColorUtil;
import fun.endcore.esduels.util.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the DonutSMP-inspired Queue Confirm GUI replicating the visual reference.
 */
public class QueueConfirmGUI implements InventoryHolder {
    public static final String GUI_TITLE = "QUEUE CONFIRM";
    public static final int SIZE = 27;

    public static final int SLOT_CANCEL = 10;
    public static final int SLOT_STATUS = 12;
    public static final int SLOT_STATS = 13;
    public static final int SLOT_REGION = 14;
    public static final int SLOT_JOIN = 16;

    private final ESDuels plugin;
    private final Player player;
    private final Inventory inventory;

    public QueueConfirmGUI(ESDuels plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        String title = plugin.getConfigManager().getConfig().getString("gui.title", GUI_TITLE);
        this.inventory = Bukkit.createInventory(this, SIZE, MessageUtil.toComponent(title));
        build();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }

    public void build() {
        Material fillerMat = getMaterial("gui.filler-material", Material.GRAY_STAINED_GLASS_PANE);
        ItemStack filler = createItem(fillerMat, " ", null);

        // Fill entire inventory with background glass panes
        for (int i = 0; i < SIZE; i++) {
            inventory.setItem(i, filler);
        }

        // Slot 10: Red Stained Glass Pane (Cancel / Close)
        Material cancelMat = getMaterial("gui.cancel-material", Material.RED_STAINED_GLASS_PANE);
        inventory.setItem(SLOT_CANCEL, createItem(cancelMat, "&cCancel", List.of("&7Click to close menu")));

        // Slot 12: Compass (Queue Status)
        updateStatusItem();

        // Slot 13: Gray Dye (Statistics)
        updateStatsItem();

        // Slot 14: Feather (Region)
        updateRegionItem();

        // Slot 16: Lime Stained Glass Pane (Join Queue)
        Material joinMat = getMaterial("gui.join-material", Material.LIME_STAINED_GLASS_PANE);
        inventory.setItem(SLOT_JOIN, createItem(joinMat, "&aJoin Queue", List.of("&7Click to enter the queue")));
    }

    public void updateStatusItem() {
        int waiting = plugin.getQueueManager().getQueueCount();
        String estWait = plugin.getQueueManager().getEstimatedWaitTime();
        boolean queued = plugin.getQueueManager().isQueued(player.getUniqueId());

        List<String> lore = new ArrayList<>();
        lore.add("&7Waiting: &f" + waiting);
        lore.add("&7Estimated Wait: &f" + estWait);
        if (queued) {
            lore.add("&7Status: &aSearching for match...");
        } else {
            lore.add("&7Status: &eClick green pane to join");
        }

        Material statusMat = getMaterial("gui.status-material", Material.COMPASS);
        inventory.setItem(SLOT_STATUS, createItem(statusMat, "&aQUEUE STATUS", lore));
    }

    public void updateStatsItem() {
        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        List<String> lore = new ArrayList<>();
        lore.add("&fWins: " + stats.getWins());
        lore.add("&fLosses: " + stats.getLosses());
        lore.add("&fStreak: " + stats.getStreak());

        Material statsMat = getMaterial("gui.stats-material", Material.GRAY_DYE);
        inventory.setItem(SLOT_STATS, createItem(statsMat, "&aSTATISTICS", lore));
    }

    public void updateRegionItem() {
        String regionName = plugin.getConfigManager().getConfig().getString("queue.region-name", "NA East");
        int ping = player.getPing();
        List<String> lore = new ArrayList<>();
        lore.add("&f" + regionName + ": &b(" + ping + "ms)");

        Material regionMat = getMaterial("gui.region-material", Material.FEATHER);
        inventory.setItem(SLOT_REGION, createItem(regionMat, "&aREGION", lore));
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) {
                meta.displayName(fun.endcore.esduels.util.MessageUtil.toComponent(name));
            }
            if (lore != null) {
                List<Component> componentLore = new ArrayList<>();
                for (String line : lore) {
                    componentLore.add(fun.endcore.esduels.util.MessageUtil.toComponent(line));
                }
                meta.lore(componentLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material getMaterial(String configPath, Material fallback) {
        String matName = plugin.getConfigManager().getConfig().getString(configPath);
        if (matName != null) {
            try {
                return Material.valueOf(matName.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return fallback;
    }
}
