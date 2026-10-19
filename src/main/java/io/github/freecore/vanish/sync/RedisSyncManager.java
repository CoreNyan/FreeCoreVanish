package io.github.freecore.vanish.sync;

import io.github.freecore.vanish.FreeCoreVanish;
import org.bukkit.Bukkit;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisSyncManager {

    private final FreeCoreVanish plugin;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean enabled;
    private String host;
    private int port;
    private String password;
    private String keyPrefix;
    private String publishChannel;

    public RedisSyncManager(FreeCoreVanish plugin) {
        this.plugin = plugin;
    }

    public void init() {
        var cfg = plugin.getConfigManager().getConfig();
        this.enabled = cfg.getBoolean("redis.enabled", true);
        this.host = cfg.getString("redis.host", "127.0.0.1");
        this.port = cfg.getInt("redis.port", 6379);
        this.password = cfg.getString("redis.password", "");
        this.keyPrefix = cfg.getString("redis.key-prefix", "freecore:vanish:");
        this.publishChannel = cfg.getString("redis.publish-channel", "fctransfer:vanish");
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    /**
     * 异步从 Redis 查询指定 UUID 的隐身状态
     */
    public CompletableFuture<Boolean> isVanishedInRedis(UUID uuid) {
        if (!enabled) {
            return CompletableFuture.completedFuture(false);
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                String val = executeRedisCommand("GET", keyPrefix + uuid.toString());
                return "1".equals(val) || "true".equalsIgnoreCase(val);
            } catch (Exception e) {
                plugin.getLogger().warning("读取 Redis 隐身状态失败: " + e.getMessage());
                return false;
            }
        }, executor);
    }

    /**
     * 异步更新 Redis 中的隐身状态并广播给跨服代理与兄弟服务器
     */
    public void setVanishedInRedis(UUID uuid, boolean vanished) {
        if (!enabled) return;
        executor.submit(() -> {
            try {
                String key = keyPrefix + uuid.toString();
                if (vanished) {
                    executeRedisCommand("SET", key, "1");
                } else {
                    executeRedisCommand("DEL", key);
                }
                // 发布广播通知代理端（FCTabBridge / Velocity）同步 TabList
                String payload = uuid.toString() + "|" + (vanished ? "1" : "0");
                executeRedisCommand("PUBLISH", publishChannel, payload);
            } catch (Exception e) {
                plugin.getLogger().warning("同步隐身状态至 Redis 失败: " + e.getMessage());
            }
        });
    }

    /**
     * 轻量级 RESP 协议与 Redis 通信，无需依赖繁重的第三方 jedis/lettuce
     */
    private synchronized String executeRedisCommand(String... args) throws IOException {
        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(3000);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            if (password != null && !password.isEmpty()) {
                sendRespCommand(out, "AUTH", password);
                readRespResponse(in);
            }

            sendRespCommand(out, args);
            return readRespResponse(in);
        }
    }

    private void sendRespCommand(OutputStream out, String... args) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(args.length).append("\r\n");
        for (String arg : args) {
            byte[] bytes = arg.getBytes(StandardCharsets.UTF_8);
            sb.append("$").append(bytes.length).append("\r\n").append(arg).append("\r\n");
        }
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    private String readRespResponse(InputStream in) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line = reader.readLine();
        if (line == null) return null;
        char prefix = line.charAt(0);
        if (prefix == '+') {
            return line.substring(1);
        } else if (prefix == '-') {
            throw new IOException("Redis error: " + line.substring(1));
        } else if (prefix == ':') {
            return line.substring(1);
        } else if (prefix == '$') {
            int len = Integer.parseInt(line.substring(1));
            if (len == -1) return null;
            char[] buf = new char[len];
            int read = 0;
            while (read < len) {
                int r = reader.read(buf, read, len - read);
                if (r == -1) break;
                read += r;
            }
            reader.readLine(); // 读取结尾 CRLF
            return new String(buf);
        }
        return null;
    }
}
