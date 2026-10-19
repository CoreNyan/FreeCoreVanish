package io.github.freecore.vanish.listener;

import io.github.freecore.vanish.FreeCoreVanish;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    private final FreeCoreVanish plugin;

    public PlayerConnectionListener(FreeCoreVanish plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // 1. 如果该玩家已在 Redis 中标记为跨服隐身状态，恢复其隐身
        if (plugin.getConfigManager().getConfig().getBoolean("features.persist-across-servers", true)) {
            plugin.getRedisSyncManager().isVanishedInRedis(player.getUniqueId()).thenAccept(vanished -> {
                if (vanished) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.isOnline()) {
                            plugin.getVanishManager().vanishPlayer(player, false);
                            player.sendMessage(plugin.getConfigManager().getMessage("silent-join"));
                        }
                    });
                }
            });
        }

        // 2. 如果玩家是隐身状态进服，静音加入消息（其他玩家无法察觉）
        if (plugin.getVanishManager().isVanished(player)) {
            event.joinMessage(null);
        }

        // 3. 向新进服的普通玩家隐藏所有当前隐身的玩家
        plugin.getVanishManager().onPlayerJoin(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getVanishManager().isVanished(player)) {
            // 隐身玩家退出时不产生任何退出广播
            event.quitMessage(null);
        }
    }
}
