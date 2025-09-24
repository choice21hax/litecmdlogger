package site.choice21.litecmdlogger;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener implements Listener {
    private final Litecmdlogger plugin;
    private final DatabaseManager db;

    public CommandListener(Litecmdlogger plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
    }

    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage();
        db.logCommandAsync(event.getPlayer().getName(), cmd);
    }
}
