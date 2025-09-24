package site.choice21.litecmdlogger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private final JavaPlugin plugin;
    private final HikariDataSource dataSource;
    private final StorageType storageType;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;

        this.storageType = StorageType.fromString(plugin.getConfig().getString("storage.type", "sqlite"));

        HikariConfig config = new HikariConfig();

        if (storageType == StorageType.SQLITE) {
            // Ensure data folder exists
            if (!plugin.getDataFolder().exists()) {
                //noinspection ResultOfMethodCallIgnored
                plugin.getDataFolder().mkdirs();
            }
            String filename = plugin.getConfig().getString("sqlite.file", "data.db");
            String path = new java.io.File(plugin.getDataFolder(), filename).getAbsolutePath();
            config.setJdbcUrl("jdbc:sqlite:" + path);
            config.setMaximumPoolSize(1); // SQLite is file-based; keep small pool
            config.setConnectionTestQuery("SELECT 1");
        } else {
            String host = plugin.getConfig().getString("mysql.host");
            int port = plugin.getConfig().getInt("mysql.port");
            String db = plugin.getConfig().getString("mysql.database");
            String user = plugin.getConfig().getString("mysql.username");
            String pass = plugin.getConfig().getString("mysql.password");

            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=false&useUnicode=true&characterEncoding=utf8");
            config.setUsername(user);
            config.setPassword(pass);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.setMaximumPoolSize(plugin.getConfig().getInt("mysql.pool.maximumPoolSize", 10));
            config.setMinimumIdle(plugin.getConfig().getInt("mysql.pool.minimumIdle", 2));
            config.setConnectionTimeout(plugin.getConfig().getLong("mysql.pool.connectionTimeoutMs", 30000));
        }

        this.dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            if (storageType == StorageType.SQLITE) {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS command_logs (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "player TEXT NOT NULL," +
                        "command TEXT NOT NULL," +
                        "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
            } else {
                stmt.executeUpdate("CREATE TABLE IF NOT EXISTS command_logs (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "player VARCHAR(16) NOT NULL," +
                        "command TEXT NOT NULL," +
                        "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create table!");
            e.printStackTrace();
        }
    }

    public void logCommandAsync(String player, String command) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO command_logs(player, command) VALUES (?, ?)")) {
                ps.setString(1, player);
                ps.setString(2, command);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public void getCommandsPageAsync(int page, int pageSize, DatabasePageCallback callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int offset = (page - 1) * pageSize;
            List<CommandLogEntry> list = new ArrayList<>();
            long total = 0;

            try (Connection conn = dataSource.getConnection()) {
                try (PreparedStatement countPs = conn.prepareStatement("SELECT COUNT(*) AS c FROM command_logs")) {
                    ResultSet crs = countPs.executeQuery();
                    if (crs.next()) total = crs.getLong("c");
                }
                try (PreparedStatement ps = conn.prepareStatement(
                        storageType == StorageType.SQLITE
                                ? "SELECT player, command, timestamp FROM command_logs ORDER BY id DESC LIMIT ? OFFSET ?"
                                : "SELECT player, command, timestamp FROM command_logs ORDER BY id DESC LIMIT ? OFFSET ?")) {
                    ps.setInt(1, pageSize);
                    ps.setInt(2, offset);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        list.add(new CommandLogEntry(
                                rs.getString("player"),
                                rs.getString("command"),
                                rs.getTimestamp("timestamp")
                        ));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            long totalPages = Math.max(1, (long) Math.ceil((double) total / pageSize));
            int safePage = (int) Math.max(1, Math.min(page, totalPages));
            callback.onResult(list, safePage, (int) totalPages, total);
        });
    }

    public void close() {
        dataSource.close();
    }

    public interface DatabasePageCallback {
        void onResult(List<CommandLogEntry> entries, int page, int totalPages, long totalCount);
    }

    private enum StorageType {
        SQLITE, MYSQL;

        static StorageType fromString(String s) {
            if (s == null) return SQLITE;
            switch (s.toLowerCase()) {
                case "mysql":
                    return MYSQL;
                case "sqlite":
                default:
                    return SQLITE;
            }
        }
    }
}
