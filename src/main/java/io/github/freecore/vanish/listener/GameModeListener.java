package io.github.freecore.vanish.listener;

import io.github.freecore.vanish.FreeCoreVanish;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public class GameModeListener implements Listener {

    private final FreeCoreVanish plugin;

    public GameModeListener(FreeCoreVanish plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getVanishManager().isVanished(player)) return;

        // 仅处理碰撞体积，保持原汁原味的纯净隐身，绝不自动开关隐身！
        if (event.getNewGameMode() == GameMode.SPECTATOR) {
            player.setCollidable(false);
        } else {
            player.setCollidable(true);
        }
    }
}
