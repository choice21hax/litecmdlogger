package site.choice21.litecmdlogger;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Litecmdlogger extends JavaPlugin {
    private DatabaseManager databaseManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        // Load config and initialize services
        saveDefaultConfig();

        this.databaseManager = new DatabaseManager(this);
        this.guiManager = new GUIManager(this, databaseManager);

        // Register event listeners
        getServer().getPluginManager().registerEvents(new CommandListener(this, databaseManager), this);
        getServer().getPluginManager().registerEvents(guiManager, this);

        // Register command executor
        if (getCommand("commandlogs") != null) {
            getCommand("commandlogs").setExecutor((sender, cmd, label, args) -> {
                if (sender instanceof Player player) {
                    guiManager.openCommandGUI(player, 1);
                } else {
                    sender.sendMessage("Players only.");
                }
                return true;
            });
        }
    }

    @Override
    public void onDisable() {
        // Shutdown database pool
        if (databaseManager != null) {
            databaseManager.close();
        }
    }
}
