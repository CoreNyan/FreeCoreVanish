package io.github.freecore.vanish.listener;

import io.github.freecore.vanish.FreeCoreVanish;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;

public class ContainerSilentListener implements Listener {

    private final FreeCoreVanish plugin;

    public ContainerSilentListener(FreeCoreVanish plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getVanishManager().isVanished(player)) return;

        if (!plugin.getConfigManager().getConfig().getBoolean("features.silent-containers", true)) return;

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        // 拦截右键末影箱或常规容器，实现完全静音与防动画展开
        Material type = block.getType();
        if (type == Material.ENDER_CHEST) {
            event.setCancelled(true);
            player.openInventory(player.getEnderChest());
        } else if (block.getState() instanceof Container container) {
            // 打开普通箱子、木桶、潜影盒时静音打开虚拟视图
            Inventory inv = container.getInventory();
            event.setCancelled(true);
            player.openInventory(inv);
        }
    }
}
