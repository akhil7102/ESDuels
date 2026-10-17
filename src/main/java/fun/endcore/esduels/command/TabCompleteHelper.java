package fun.endcore.esduels.command;

import fun.endcore.esduels.ESDuels;
import fun.endcore.esduels.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Rich tab-completion helper for /duel and /duels.
 */
public class TabCompleteHelper implements TabCompleter {
    private final ESDuels plugin;

    public TabCompleteHelper(ESDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> sub = new ArrayList<>(Arrays.asList("gui", "send", "accept", "deny", "leave"));
            if (sender.hasPermission("esduels.admin")) {
                sub.add("arena");
                sub.add("setspawn");
                sub.add("reload");
            }
            for (Player p : Bukkit.getOnlinePlayers()) {
                sub.add(p.getName());
            }
            return StringUtil.copyPartialMatches(args[0], sub, completions);
        }

        if (args.length == 2) {
            String first = args[0].toLowerCase();
            if (first.equals("send") || first.equals("accept") || first.equals("deny")) {
                List<String> players = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getName().equalsIgnoreCase(sender.getName())) {
                        players.add(p.getName());
                    }
                }
                return StringUtil.copyPartialMatches(args[1], players, completions);
            }
            if (first.equals("arena") && sender.hasPermission("esduels.admin")) {
                List<String> arenaSubs = Arrays.asList("create", "delete", "set", "save", "list");
                return StringUtil.copyPartialMatches(args[1], arenaSubs, completions);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("arena") && sender.hasPermission("esduels.admin")) {
            String action = args[1].toLowerCase();
            if (action.equals("set") || action.equals("save") || action.equals("delete")) {
                List<String> names = new ArrayList<>();
                for (Arena a : plugin.getArenaManager().getArenas()) {
                    names.add(a.getName());
                }
                return StringUtil.copyPartialMatches(args[2], names, completions);
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("arena") && args[1].equalsIgnoreCase("set") && sender.hasPermission("esduels.admin")) {
            List<String> points = Arrays.asList("pos1", "pos2", "corner1", "corner2");
            return StringUtil.copyPartialMatches(args[3], points, completions);
        }

        return Collections.emptyList();
    }
}
