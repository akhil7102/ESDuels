package fun.endcore.esduels.config;

import fun.endcore.esduels.ESDuels;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Manages plugin configuration files (config.yml, messages.yml, spawn.yml).
 */
public class ConfigManager {
    private final ESDuels plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File messagesFile;
    private String prefix = "&8[&bES&fDuels&8] ";

    public ConfigManager(ESDuels plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        // Load config.yml
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Load messages.yml
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Sync missing defaults from internal resource if any
        InputStream defStream = plugin.getResource("messages.yml");
        if (defStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8));
            this.messages.setDefaults(defConfig);
        }

        this.prefix = messages.getString("prefix", "&8[&bES&fDuels&8] ");
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * Resolves a message by path with {PREFIX} replaced.
     */
    public String getMessage(String path, String def) {
        String msg = messages.getString(path, def);
        if (msg == null) return "";
        return msg.replace("{PREFIX}", prefix);
    }

    public String getMessage(String path) {
        return getMessage(path, "");
    }

    public List<String> getMessageList(String path) {
        List<String> list = messages.getStringList(path);
        if (list.isEmpty()) {
            String single = messages.getString(path);
            if (single != null && !single.isEmpty()) {
                return List.of(single.replace("{PREFIX}", prefix));
            }
            return Collections.emptyList();
        }
        return list.stream().map(s -> s.replace("{PREFIX}", prefix)).toList();
    }
}
