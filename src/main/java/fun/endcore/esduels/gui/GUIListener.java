package fun.endcore.esduels.gui;

import fun.endcore.esduels.ESDuels;
import fun.endcore.esduels.util.MessageUtil;
import fun.endcore.esduels.util.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class GUIListener implements Listener {
    private final ESDuels plugin;
    private org.bukkit.scheduler.BukkitTask updaterTask;

    public GUIListener(ESDuels plugin) {
        this.plugin = plugin;
        startDynamicUpdaterTask();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof QueueConfirmGUI gui) {
            event.setCancelled(true);

            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }

            player.updateInventory();

            int slot = event.getRawSlot();
            String clickSound = plugin.getConfigManager().getConfig().getString("sounds.click", "UI_BUTTON_CLICK");

            if (slot == QueueConfirmGUI.SLOT_CANCEL) {
                SoundUtil.playSound(player, clickSound, 1.0f, 1.0f);
                player.closeInventory();
            } else if (slot == QueueConfirmGUI.SLOT_JOIN) {
                SoundUtil.playSound(player, clickSound, 1.0f, 1.2f);
                player.closeInventory();

                if (plugin.getQueueManager().isQueued(player.getUniqueId())) {
                    String msg = plugin.getConfigManager().getMessage("queue.already-queued");
                    player.sendMessage(MessageUtil.toComponent(msg));
                    return;
                }

                if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
                    String msg = plugin.getConfigManager().getMessage("challenge.sender-in-duel");
                    player.sendMessage(MessageUtil.toComponent(msg));
                    return;
                }

                if (plugin.getLootTimerManager().isInLootSession(player.getUniqueId())) {
                    MessageUtil.sendMessage(player, "&cYou cannot join the queue while collecting duel loot.");
                    return;
                }

                plugin.getQueueManager().joinQueue(player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof QueueConfirmGUI) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.updateInventory();
            }
        }
    }

    private void startDynamicUpdaterTask() {
        int interval = plugin.getConfigManager().getConfig().getInt("queue.refresh-interval-ticks", 20);
        updaterTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getOpenInventory().getTopInventory().getHolder() instanceof QueueConfirmGUI gui) {
                        gui.updateStatusItem();
                        gui.updateRegionItem();
                    }
                }
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    public void shutdown() {
        if (updaterTask != null) {
            updaterTask.cancel();
            updaterTask = null;
        }
    }
}
