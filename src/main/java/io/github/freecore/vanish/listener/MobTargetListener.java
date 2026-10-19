package io.github.freecore.vanish.listener;

import io.github.freecore.vanish.FreeCoreVanish;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public class MobTargetListener implements Listener {

    private final FreeCoreVanish plugin;

    public MobTargetListener(FreeCoreVanish plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMobTarget(EntityTargetLivingEntityEvent event) {
        if (!plugin.getConfigManager().getConfig().getBoolean("features.clear-mob-targets", true)) return;

        if (event.getTarget() instanceof Player player) {
            if (plugin.getVanishManager().isVanished(player)) {
                // 隐身状态下取消所有生物把该玩家作为目标
                event.setCancelled(true);
                event.setTarget(null);
            }
        }
    }
}
