package fun.endcore.esduels.command;

import fun.endcore.esduels.ESDuels;
import fun.endcore.esduels.arena.Arena;
import fun.endcore.esduels.arena.ArenaState;
import fun.endcore.esduels.arena.Cuboid;
import fun.endcore.esduels.util.MessageUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Handles administrative commands for arena management and main spawn setting.
 */
public class DuelsAdminCommand implements CommandExecutor {
    private final ESDuels plugin;

    public DuelsAdminCommand(ESDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "setspawn" -> {
                if (!(sender instanceof Player player)) {
                    MessageUtil.sendMessage(sender, "&cThis command can only be executed by players.");
                    return true;
                }
                if (!player.hasPermission("esduels.admin")) {
                    String msg = plugin.getConfigManager().getMessage("general.no-permission");
                    player.sendMessage(MessageUtil.toComponent(msg));
                    return true;
                }
                plugin.getSpawnManager().setSpawnLocation(player.getLocation());
                String msg = plugin.getConfigManager().getMessage("spawn.set");
                player.sendMessage(MessageUtil.toComponent(msg));
                return true;
            }

            case "reload" -> {
                if (!sender.hasPermission("esduels.admin")) {
                    String msg = plugin.getConfigManager().getMessage("general.no-permission");
                    sender.sendMessage(MessageUtil.toComponent(msg));
                    return true;
                }
                plugin.getConfigManager().load();
                plugin.getSpawnManager().load();
                plugin.getArenaManager().loadArenas();
                String msg = plugin.getConfigManager().getMessage("general.reload-success");
                sender.sendMessage(MessageUtil.toComponent(msg));
                return true;
            }

            case "arena" -> {
                if (!sender.hasPermission("esduels.admin") && !sender.hasPermission("esduels.arena")) {
                    String msg = plugin.getConfigManager().getMessage("general.no-permission");
                    sender.sendMessage(MessageUtil.toComponent(msg));
                    return true;
                }
                handleArenaSubcommand(sender, args);
                return true;
            }

            default -> {
                sendHelp(sender);
                return true;
            }
        }
    }

    private void handleArenaSubcommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendArenaHelp(sender);
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "create" -> {
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender, "&cUsage: /duels arena create <arenaname>");
                    return;
                }
                String name = args[2];
                if (!fun.endcore.esduels.arena.ArenaManager.isValidArenaName(name)) {
                    MessageUtil.sendMessage(sender, "&cArena names must be 1-32 alphanumeric characters (letters, numbers, hyphens, and underscores only).");
                    return;
                }
                Arena existing = plugin.getArenaManager().getArena(name);
                if (existing != null) {
                    String msg = plugin.getConfigManager().getMessage("arena.exists").replace("{ARENA}", name);
                    sender.sendMessage(MessageUtil.toComponent(msg));
                    return;
                }
                plugin.getArenaManager().createArena(name);
                String msg = plugin.getConfigManager().getMessage("arena.created").replace("{ARENA}", name);
                sender.sendMessage(MessageUtil.toComponent(msg));
            }

            case "delete" -> {
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender, "&cUsage: /duels arena delete <arenaname>");
                    return;
                }
                String name = args[2];
                if (!fun.endcore.esduels.arena.ArenaManager.isValidArenaName(name)) {
                    MessageUtil.sendMessage(sender, "&cInvalid arena name.");
                    return;
                }
                Arena arena = plugin.getArenaManager().getArena(name);
                if (arena != null && plugin.getDuelManager().getDuelByArena(arena) != null) {
                    MessageUtil.sendMessage(sender, "&cCannot delete arena while a duel is active in it!");
                    return;
                }
                boolean deleted = plugin.getArenaManager().deleteArena(name);
                if (deleted) {
                    String msg = plugin.getConfigManager().getMessage("arena.deleted").replace("{ARENA}", name);
                    sender.sendMessage(MessageUtil.toComponent(msg));
                } else {
                    String msg = plugin.getConfigManager().getMessage("arena.not-found").replace("{ARENA}", name);
                    sender.sendMessage(MessageUtil.toComponent(msg));
                }
            }

            case "list" -> {
                MessageUtil.sendMessage(sender, "&8&m----------------&r &bESDuels Arenas &8&m----------------");
                for (Arena a : plugin.getArenaManager().getArenas()) {
                    String status = a.isAvailable() ? "&a[READY]" : "&c[" + a.getState().name() + "]";
                    String missing = a.isConfigured() ? "" : " &7(Missing: &e" + plugin.getArenaManager().getIncompleteReason(a) + "&7)";
                    MessageUtil.sendMessage(sender, " &8• &e" + a.getName() + " " + status + missing);
                }
                MessageUtil.sendMessage(sender, "&8&m----------------------------------------");
            }

            case "set" -> {
                if (!(sender instanceof Player player)) {
                    MessageUtil.sendMessage(sender, "&cThis command can only be executed by players.");
                    return;
                }
                if (args.length < 4) {
                    MessageUtil.sendMessage(sender, "&cUsage: /duels arena set <arenaname> <pos1|pos2|corner1|corner2>");
                    return;
                }
                String arenaName = args[2];
                if (!fun.endcore.esduels.arena.ArenaManager.isValidArenaName(arenaName)) {
                    MessageUtil.sendMessage(sender, "&cInvalid arena name.");
                    return;
                }
                String point = args[3].toLowerCase();

                Arena arena = plugin.getArenaManager().getArena(arenaName);
                if (arena == null) {
                    String msg = plugin.getConfigManager().getMessage("arena.not-found").replace("{ARENA}", arenaName);
                    sender.sendMessage(MessageUtil.toComponent(msg));
                    return;
                }

                switch (point) {
                    case "pos1" -> {
                        arena.setPos1(player.getLocation());
                        plugin.getArenaManager().saveArenaConfig(arena);
                        String msg = plugin.getConfigManager().getMessage("arena.pos1-set").replace("{ARENA}", arena.getName());
                        player.sendMessage(MessageUtil.toComponent(msg));
                    }
                    case "pos2" -> {
                        arena.setPos2(player.getLocation());
                        plugin.getArenaManager().saveArenaConfig(arena);
                        String msg = plugin.getConfigManager().getMessage("arena.pos2-set").replace("{ARENA}", arena.getName());
                        player.sendMessage(MessageUtil.toComponent(msg));
                    }
                    case "corner1" -> {
                        arena.setCorner1(player.getLocation());
                        plugin.getArenaManager().saveArenaConfig(arena);
                        String msg = plugin.getConfigManager().getMessage("arena.corner1-set").replace("{ARENA}", arena.getName());
                        player.sendMessage(MessageUtil.toComponent(msg));
                    }
                    case "corner2" -> {
                        arena.setCorner2(player.getLocation());
                        plugin.getArenaManager().saveArenaConfig(arena);
                        String msg = plugin.getConfigManager().getMessage("arena.corner2-set").replace("{ARENA}", arena.getName());
                        player.sendMessage(MessageUtil.toComponent(msg));
                    }
                    default -> MessageUtil.sendMessage(player, "&cInvalid point. Use pos1, pos2, corner1, or corner2.");
                }
            }

            case "save" -> {
                if (args.length < 3) {
                    MessageUtil.sendMessage(sender, "&cUsage: /duels arena save <arenaname>");
                    return;
                }
                String name = args[2];
                if (!fun.endcore.esduels.arena.ArenaManager.isValidArenaName(name)) {
                    MessageUtil.sendMessage(sender, "&cInvalid arena name.");
                    return;
                }
                Arena arena = plugin.getArenaManager().getArena(name);
                if (arena == null) {
                    String msg = plugin.getConfigManager().getMessage("arena.not-found").replace("{ARENA}", name);
                    sender.sendMessage(MessageUtil.toComponent(msg));
                    return;
                }

                if (plugin.getDuelManager().getDuelByArena(arena) != null || arena.getState() == ArenaState.ACTIVE || arena.getState() == ArenaState.COUNTDOWN || arena.getState() == ArenaState.LOOTING || arena.getState() == ArenaState.RESTORING) {
                    MessageUtil.sendMessage(sender, "&cCannot save snapshot while arena is active, in a duel, or restoring!");
                    return;
                }

                if (arena.getCorner1() == null || arena.getCorner2() == null || arena.getWorld() == null) {
                    MessageUtil.sendMessage(sender, "&cCannot save snapshot: both corner1 and corner2 must be set first!");
                    return;
                }

                Cuboid cuboid = arena.getCuboid();
                MessageUtil.sendMessage(sender, "&7Capturing and saving snapshot for &e" + arena.getName() + "&7... (Volume: " + cuboid.getVolume() + " blocks)");

                plugin.getSnapshotStorage().captureAndSaveAsync(arena.getName(), cuboid, success -> {
                    if (success) {
                        arena.setHasSnapshot(true);
                        plugin.getArenaManager().saveArenaConfig(arena);
                        if (arena.isConfigured()) {
                            arena.setState(ArenaState.IDLE);
                        }
                        String msg = plugin.getConfigManager().getMessage("arena.saved").replace("{ARENA}", arena.getName());
                        sender.sendMessage(MessageUtil.toComponent(msg));
                    } else {
                        String msg = plugin.getConfigManager().getMessage("arena.save-failed").replace("{ERROR}", "Disk write error or concurrent save in progress");
                        sender.sendMessage(MessageUtil.toComponent(msg));
                    }
                });
            }

            default -> sendArenaHelp(sender);
        }
    }

    private void sendHelp(CommandSender sender) {
        MessageUtil.sendMessage(sender, "&8&m----------------&r &bESDuels Admin &8&m----------------");
        MessageUtil.sendMessage(sender, " &e/duels setspawn &7- Set global duels spawn point");
        MessageUtil.sendMessage(sender, " &e/duels arena create <arenaname> &7- Create arena");
        MessageUtil.sendMessage(sender, " &e/duels arena set <name> <pos1|pos2|corner1|corner2>");
        MessageUtil.sendMessage(sender, " &e/duels arena save <arenaname> &7- Capture & persist snapshot");
        MessageUtil.sendMessage(sender, " &e/duels arena delete <arenaname> &7- Delete arena");
        MessageUtil.sendMessage(sender, " &e/duels arena list &7- List all arenas and states");
        MessageUtil.sendMessage(sender, " &e/duels reload &7- Reload config and arenas");
        MessageUtil.sendMessage(sender, "&8&m----------------------------------------");
    }

    private void sendArenaHelp(CommandSender sender) {
        MessageUtil.sendMessage(sender, "&8&m----------------&r &bArena Commands &8&m----------------");
        MessageUtil.sendMessage(sender, " &e/duels arena create <name>");
        MessageUtil.sendMessage(sender, " &e/duels arena set <name> pos1");
        MessageUtil.sendMessage(sender, " &e/duels arena set <name> pos2");
        MessageUtil.sendMessage(sender, " &e/duels arena set <name> corner1");
        MessageUtil.sendMessage(sender, " &e/duels arena set <name> corner2");
        MessageUtil.sendMessage(sender, " &e/duels arena save <name>");
        MessageUtil.sendMessage(sender, " &e/duels arena delete <name>");
        MessageUtil.sendMessage(sender, " &e/duels arena list");
        MessageUtil.sendMessage(sender, "&8&m----------------------------------------");
    }
}
