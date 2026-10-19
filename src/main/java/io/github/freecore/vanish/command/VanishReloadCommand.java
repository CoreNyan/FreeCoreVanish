package io.github.freecore.vanish.command;

import io.github.freecore.vanish.FreeCoreVanish;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class VanishReloadCommand implements CommandExecutor, TabCompleter {

    private final FreeCoreVanish plugin;

    public VanishReloadCommand(FreeCoreVanish plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("freecore.vanish.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        plugin.getConfigManager().loadConfigs();
        plugin.getRedisSyncManager().init();
        sender.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return Collections.emptyList();
    }
}
