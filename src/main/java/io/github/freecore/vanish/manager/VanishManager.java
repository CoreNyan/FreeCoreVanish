package io.github.freecore.vanish.manager;

import io.github.freecore.vanish.FreeCoreVanish;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VanishManager {

    private final FreeCoreVanish plugin;
    private final Set<UUID> vanishedPlayers = ConcurrentHashMap.newKeySet();

    public VanishManager(FreeCoreVanish plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(Player player) {
        return player != null && vanishedPlayers.contains(player.getUniqueId());
    }

    public boolean isVanished(UUID uuid) {
        return uuid != null && vanishedPlayers.contains(uuid);
    }

    public Set<UUID> getVanishedPlayers() {
        return Collections.unmodifiableSet(vanishedPlayers);
    }

    public void toggleVanish(Player player) {
        if (isVanished(player)) {
            unvanishPlayer(player, true);
        } else {
            vanishPlayer(player, true);
        }
    }

    public void vanishPlayer(Player player, boolean notify) {
        UUID uuid = player.getUniqueId();
        vanishedPlayers.add(uuid);

        // 对所有无查看权限的在线玩家隐藏
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (online.hasPermission("freecore.vanish.view")) continue;
            online.hidePlayer(plugin, player);
        }

        var cfg = plugin.getConfigManager().getConfig();
        if (cfg.getBoolean("features.ignore-sleep", true)) {
            player.setSleepingIgnored(true);
        }
        if (cfg.getBoolean("features.disable-mob-spawning", true)) {
            player.setAffectsSpawning(false);
        }
        if (cfg.getBoolean("features.allow-flight", true)) {
            player.setAllowFlight(true);
        }
        if (cfg.getBoolean("features.clear-mob-targets", true)) {
            clearSurroundingMobTargets(player);
        }
        if (cfg.getBoolean("features.night-vision", true)) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.NIGHT_VISION,
                    org.bukkit.potion.PotionEffect.INFINITE_DURATION,
                    0, false, false, true));
        }

        // 旁观者模式下关闭碰撞
        if (player.getGameMode() == GameMode.SPECTATOR) {
            player.setCollidable(false);
        }

        // 跨服 Redis 同步状态
        if (cfg.getBoolean("features.persist-across-servers", true)) {
            plugin.getRedisSyncManager().setVanishedInRedis(uuid, true);
        }

        if (notify) {
            player.sendMessage(plugin.getConfigManager().getMessage("vanish-enabled"));
        }
    }

    public void unvanishPlayer(Player player, boolean notify) {
        UUID uuid = player.getUniqueId();
        vanishedPlayers.remove(uuid);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            online.showPlayer(plugin, player);
        }

        player.setSleepingIgnored(false);
        player.setAffectsSpawning(true);
        player.setCollidable(true);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION);
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setAllowFlight(false);
            player.setFlying(false);
        }

        var cfg = plugin.getConfigManager().getConfig();
        if (cfg.getBoolean("features.persist-across-servers", true)) {
            plugin.getRedisSyncManager().setVanishedInRedis(uuid, false);
        }

        if (notify) {
            player.sendMessage(plugin.getConfigManager().getMessage("vanish-disabled"));
        }
    }

    public void onPlayerJoin(Player newPlayer) {
        if (!newPlayer.hasPermission("freecore.vanish.view")) {
            for (UUID uuid : vanishedPlayers) {
                Player vanished = Bukkit.getPlayer(uuid);
                if (vanished != null && !vanished.equals(newPlayer)) {
                    newPlayer.hidePlayer(plugin, vanished);
                }
            }
        }
    }

    public void clearSurroundingMobTargets(Player player) {
        for (Entity e : player.getNearbyEntities(24, 24, 24)) {
            if (e instanceof Mob mob) {
                LivingEntity target = mob.getTarget();
                if (target != null && target.equals(player)) {
                    mob.setTarget(null);
                }
            }
        }
    }
}
