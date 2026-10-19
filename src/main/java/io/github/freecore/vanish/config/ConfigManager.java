package io.github.freecore.vanish.config;

import io.github.freecore.vanish.FreeCoreVanish;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    private final FreeCoreVanish plugin;
    private FileConfiguration config;
    private FileConfiguration langConfig;
    private File langFile;

    private String prefix;

    public ConfigManager(FreeCoreVanish plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        // 保存并载入默认 config.yml
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // 加载 lang.yml
        this.langFile = new File(plugin.getDataFolder(), "lang.yml");
        if (!langFile.exists()) {
            plugin.saveResource("lang.yml", false);
        }
        this.langConfig = YamlConfiguration.loadConfiguration(langFile);

        // 前缀设置，严格根据 FreeCorePluginFormat_New 规范
        this.prefix = langConfig.getString("prefix", " &b&l隐身 &f&l>>> &f");
    }

    public String getRawMessage(String key) {
        String msg = langConfig.getString("messages." + key, "");
        if (msg.isEmpty()) {
            msg = "&7[Missing message: " + key + "]";
        }
        return msg;
    }

    public Component getMessage(String key) {
        String raw = prefix + getRawMessage(key);
        String colored = ChatColor.translateAlternateColorCodes('&', raw);
        return LegacyComponentSerializer.legacySection().deserialize(colored);
    }

    public String getRawPrefix() {
        return prefix;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getLangConfig() {
        return langConfig;
    }
}
