package io.github.freecore.vanish;

import io.github.freecore.vanish.command.VanishCommand;
import io.github.freecore.vanish.command.VanishReloadCommand;
import io.github.freecore.vanish.config.ConfigManager;
import io.github.freecore.vanish.listener.ContainerSilentListener;
import io.github.freecore.vanish.listener.GameModeListener;
import io.github.freecore.vanish.listener.MobTargetListener;
import io.github.freecore.vanish.listener.PlayerConnectionListener;
import io.github.freecore.vanish.manager.VanishManager;
import io.github.freecore.vanish.sync.RedisSyncManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class FreeCoreVanish extends JavaPlugin {

    private static FreeCoreVanish instance;
    private ConfigManager configManager;
    private RedisSyncManager redisSyncManager;
    private VanishManager vanishManager;

    @Override
    public void onEnable() {
        instance = this;

        // 1. 加载配置与语言文件
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfigs();

        // 2. 初始化 Redis 跨服同步服务
        this.redisSyncManager = new RedisSyncManager(this);
        this.redisSyncManager.init();

        // 3. 初始化核心管理器
        this.vanishManager = new VanishManager(this);

        // 4. 注册事件监听器
        registerListeners();

        // 5. 注册指令
        registerCommands();

        // 6. 清理旧 SimpleVanish 静态 Cache，防止 FreeCoreEssentials 循环广播旧状态
        cleanLegacySimpleVanishCache();

        // 7. 启动隐身状态常驻 Actionbar 提示任务（每 2 秒刷新一次）
        startActionbarTask();

        getLogger().info("FreeCoreVanish v1.0.0 已成功加载！由小可（CoreNyan）为 FreeCore 社区特别打造。");
    }

    private void startActionbarTask() {
        getServer().getScheduler().runTaskTimer(this, () -> {
            net.kyori.adventure.text.Component actionbar = configManager.getMessage("vanish-actionbar");
            for (java.util.UUID uuid : vanishManager.getVanishedPlayers()) {
                org.bukkit.entity.Player p = getServer().getPlayer(uuid);
                if (p != null && p.isOnline()) {
                    p.sendActionBar(actionbar);
                }
            }
        }, 20L, 40L);
    }

    private void cleanLegacySimpleVanishCache() {
        try {
            Class<?> cacheClass = Class.forName("simplexity.simplevanish.saving.Cache");
            java.lang.reflect.Method getVanished = cacheClass.getMethod("getVanishedPlayers");
            Object set = getVanished.invoke(null);
            if (set instanceof java.util.Collection<?> col) {
                col.clear();
                getLogger().info("已成功清空旧 SimpleVanish 内存 Cache，阻断 FreeCoreEssentials 脏同步。");
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onDisable() {
        if (this.redisSyncManager != null) {
            this.redisSyncManager.shutdown();
        }
        getLogger().info("FreeCoreVanish 已安全卸载。");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new GameModeListener(this), this);
        getServer().getPluginManager().registerEvents(new ContainerSilentListener(this), this);
        getServer().getPluginManager().registerEvents(new MobTargetListener(this), this);
    }

    private void registerCommands() {
        VanishCommand vanishCmd = new VanishCommand(this);
        getCommand("vanish").setExecutor(vanishCmd);
        getCommand("vanish").setTabCompleter(vanishCmd);

        VanishReloadCommand reloadCmd = new VanishReloadCommand(this);
        getCommand("vanishreload").setExecutor(reloadCmd);
        getCommand("vanishreload").setTabCompleter(reloadCmd);
    }

    public static FreeCoreVanish getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public RedisSyncManager getRedisSyncManager() {
        return redisSyncManager;
    }

    public VanishManager getVanishManager() {
        return vanishManager;
    }
}
