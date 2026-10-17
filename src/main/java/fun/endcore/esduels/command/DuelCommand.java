package fun.endcore.esduels.command;

import fun.endcore.esduels.ESDuels;
import fun.endcore.esduels.arena.Arena;
import fun.endcore.esduels.duel.Duel;
import fun.endcore.esduels.duel.DuelRequest;
import fun.endcore.esduels.gui.QueueConfirmGUI;
import fun.endcore.esduels.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles /duel commands: GUI opening, direct challenges, accept, deny, and leave.
 */
public class DuelCommand implements CommandExecutor {
    private final ESDuels plugin;
    private final DuelsAdminCommand adminHandler;

    public DuelCommand(ESDuels plugin) {
        this.plugin = plugin;
        this.adminHandler = new DuelsAdminCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // If console
        if (!(sender instanceof Player player)) {
            if (args.length > 0 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("arena"))) {
                return adminHandler.onCommand(sender, command, label, args);
            }
            MessageUtil.sendMessage(sender, "&cThis command can only be executed by players.");
            return true;
        }

        // Check base permission
        if (!player.hasPermission("esduels.use")) {
            String msg = plugin.getConfigManager().getMessage("general.no-permission");
            player.sendMessage(MessageUtil.toComponent(msg));
            return true;
        }

        // Delegate admin subcommands if invoked with /duel or /duels
        if (args.length > 0) {
            String sub = args[0].toLowerCase();
            if (sub.equals("arena") || sub.equals("setspawn") || sub.equals("reload")) {
                return adminHandler.onCommand(sender, command, label, args);
            }
        }

        // /duel with no arguments opens the DonutSMP Queue Confirm GUI
        if (args.length == 0) {
            new QueueConfirmGUI(plugin, player).open();
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "gui", "queue", "menu" -> {
                new QueueConfirmGUI(plugin, player).open();
                return true;
            }
            case "leave" -> {
                handleLeave(player);
                return true;
            }
            case "send" -> {
                if (args.length < 2) {
                    MessageUtil.sendMessage(player, "&cUsage: /duel send <player>");
                    return true;
                }
                handleSend(player, args[1]);
                return true;
            }
            case "accept" -> {
                if (args.length < 2) {
                    MessageUtil.sendMessage(player, "&cUsage: /duel accept <player>");
                    return true;
                }
                handleAccept(player, args[1]);
                return true;
            }
            case "deny", "decline" -> {
                if (args.length < 2) {
                    MessageUtil.sendMessage(player, "&cUsage: /duel deny <player>");
                    return true;
                }
                handleDeny(player, args[1]);
                return true;
            }
            default -> {
                // If typed /duel <player>, treat as /duel send <player>
                Player target = Bukkit.getPlayerExact(args[0]);
                if (target != null) {
                    handleSend(player, args[0]);
                    return true;
                }
                MessageUtil.sendMessage(player, "&cUnknown subcommand. Use /duel, /duel send <player>, /duel accept <player>, or /duel leave.");
                return true;
            }
        }
    }

    private void handleSend(Player sender, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null || !target.isOnline()) {
            String msg = plugin.getConfigManager().getMessage("general.player-not-found").replace("{PLAYER}", targetName);
            sender.sendMessage(MessageUtil.toComponent(msg));
            return;
        }

        if (sender.getUniqueId().equals(target.getUniqueId())) {
            String msg = plugin.getConfigManager().getMessage("challenge.cannot-challenge-self");
            sender.sendMessage(MessageUtil.toComponent(msg));
            return;
        }

        if (plugin.getDuelManager().isInDuel(sender.getUniqueId()) || plugin.getLootTimerManager().isInLootSession(sender.getUniqueId())) {
            String msg = plugin.getConfigManager().getMessage("challenge.sender-in-duel");
            sender.sendMessage(MessageUtil.toComponent(msg));
            return;
        }

        if (plugin.getQueueManager().isQueued(sender.getUniqueId())) {
            String msg = plugin.getConfigManager().getMessage("challenge.sender-in-queue");
            sender.sendMessage(MessageUtil.toComponent(msg));
            return;
        }

        if (plugin.getDuelManager().isInDuel(target.getUniqueId()) || plugin.getLootTimerManager().isInLootSession(target.getUniqueId())) {
            String msg = plugin.getConfigManager().getMessage("challenge.target-in-duel").replace("{TARGET}", target.getName());
            sender.sendMessage(MessageUtil.toComponent(msg));
            return;
        }

        if (plugin.getQueueManager().isQueued(target.getUniqueId())) {
            String msg = plugin.getConfigManager().getMessage("challenge.target-in-queue").replace("{TARGET}", target.getName());
            sender.sendMessage(MessageUtil.toComponent(msg));
            return;
        }

        long exp = plugin.getConfigManager().getConfig().getLong("request-expiration-seconds", 60);
        plugin.getDuelRequestManager().sendRequest(sender, target);

        String sentMsg = plugin.getConfigManager().getMessage("challenge.sent")
                .replace("{TARGET}", target.getName())
                .replace("{EXPIRES}", String.valueOf(exp));
        sender.sendMessage(MessageUtil.toComponent(sentMsg));

        String receivedMsg = plugin.getConfigManager().getMessage("challenge.received")
                .replace("{SENDER}", sender.getName())
                .replace("{EXPIRES}", String.valueOf(exp));
        target.sendMessage(MessageUtil.toComponent(receivedMsg));
    }

    private void handleAccept(Player target, String senderName) {
        Player sender = Bukkit.getPlayer(senderName);
        if (sender == null || !sender.isOnline()) {
            String msg = plugin.getConfigManager().getMessage("general.player-not-found").replace("{PLAYER}", senderName);
            target.sendMessage(MessageUtil.toComponent(msg));
            return;
        }

        DuelRequest req = plugin.getDuelRequestManager().getRequest(target.getUniqueId(), sender.getUniqueId());
        if (req == null) {
            String msg = plugin.getConfigManager().getMessage("challenge.no-request").replace("{PLAYER}", sender.getName());
            target.sendMessage(MessageUtil.toComponent(msg));
            return;
        }

        if (plugin.getDuelManager().isInDuel(target.getUniqueId()) || plugin.getDuelManager().isInDuel(sender.getUniqueId())
                || plugin.getLootTimerManager().isInLootSession(target.getUniqueId()) || plugin.getLootTimerManager().isInLootSession(sender.getUniqueId())) {
            MessageUtil.sendMessage(target, "&cOne of the players is currently in a duel or loot collection!");
            return;
        }

        Arena arena = plugin.getArenaManager().allocateAvailableArena();
        if (arena == null) {
            String msg = plugin.getConfigManager().getMessage("queue.no-arena-available");
            target.sendMessage(MessageUtil.toComponent(msg));
            sender.sendMessage(MessageUtil.toComponent(msg));
            return;
        }

        plugin.getDuelRequestManager().removeRequest(target.getUniqueId(), sender.getUniqueId());
        plugin.getQueueManager().leaveQueue(target.getUniqueId(), false);
        plugin.getQueueManager().leaveQueue(sender.getUniqueId(), false);

        String acceptedMsg = plugin.getConfigManager().getMessage("challenge.accepted").replace("{SENDER}", sender.getName());
        target.sendMessage(MessageUtil.toComponent(acceptedMsg));

        plugin.getDuelManager().startDuel(arena, sender, target);
    }

    private void handleDeny(Player target, String senderName) {
        Player sender = Bukkit.getPlayer(senderName);
        if (sender != null && sender.isOnline()) {
            boolean removed = plugin.getDuelRequestManager().removeRequest(target.getUniqueId(), sender.getUniqueId());
            if (removed) {
                String msgSender = plugin.getConfigManager().getMessage("challenge.declined-sender").replace("{TARGET}", target.getName());
                sender.sendMessage(MessageUtil.toComponent(msgSender));

                String msgTarget = plugin.getConfigManager().getMessage("challenge.declined-target").replace("{SENDER}", sender.getName());
                target.sendMessage(MessageUtil.toComponent(msgTarget));
                return;
            }
        }
        String msg = plugin.getConfigManager().getMessage("challenge.no-request").replace("{PLAYER}", senderName);
        target.sendMessage(MessageUtil.toComponent(msg));
    }

    private void handleLeave(Player player) {
        // 1. Check if queued
        if (plugin.getQueueManager().isQueued(player.getUniqueId())) {
            plugin.getQueueManager().leaveQueue(player);
            return;
        }

        // 2. Check if in loot period as winner
        if (plugin.getLootTimerManager().isInLootSession(player.getUniqueId())) {
            Duel duel = plugin.getDuelManager().getDuel(player.getUniqueId());
            plugin.getLootTimerManager().endLootSession(player.getUniqueId(), true);
            if (duel != null) {
                plugin.getDuelManager().cleanupDuelState(duel);
                plugin.getDuelManager().triggerArenaRestoration(duel.getArena());
            }
            return;
        }

        // 3. Check if in active duel
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            plugin.getDuelManager().handleForfeit(player);
            String msg = plugin.getConfigManager().getMessage("duel.leave-duel");
            player.sendMessage(MessageUtil.toComponent(msg));
            return;
        }

        MessageUtil.sendMessage(player, "&cYou are not in a duel or matchmaking queue.");
    }
}
