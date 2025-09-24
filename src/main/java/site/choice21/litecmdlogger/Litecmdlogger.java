package site.choice21.litecmdlogger;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

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
            getCommand("commandlogs").setExecutor(this::handleCommandLogs);
        }
    }

    @Override
    public void onDisable() {
        // Shutdown database pool
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    private boolean handleCommandLogs(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        // Parse args: [PLAYER] [TIMEFRAME] [MODCOMMANDS]
        String playerFilter = null;
        java.time.Instant since = null;
        Boolean modOnly = null;

        if (args.length >= 1 && !args[0].isEmpty()) {
            if (!args[0].equalsIgnoreCase("all") && !args[0].equals("*")) {
                playerFilter = args[0];
            }
        }

        if (args.length >= 2 && !args[1].isEmpty()) {
            if (!args[1].equalsIgnoreCase("all") && !args[1].equals("*")) {
                java.time.Duration dur = parseDuration(args[1]);
                if (dur != null && !dur.isNegative() && !dur.isZero()) {
                    since = java.time.Instant.now().minus(dur);
                }
            }
        }

        if (args.length >= 3 && !args[2].isEmpty()) {
            if (args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("yes")) {
                modOnly = true;
            } else if (args[2].equalsIgnoreCase("false") || args[2].equalsIgnoreCase("no")) {
                modOnly = false;
            }
        }

        java.util.List<String> modPrefixes = getConfig().getStringList("moderation.modPrefixes");
        CommandQuery query = new CommandQuery(playerFilter, since, modOnly, modPrefixes);
        guiManager.openCommandGUI(player, query, 1);
        return true;
    }

    private java.time.Duration parseDuration(String text) {
        try {
            text = text.trim().toLowerCase();
            if (text.equals("0")) return java.time.Duration.ZERO;
            char unit = text.charAt(text.length() - 1);
            String numStr = text.substring(0, text.length() - 1);
            long amount = Long.parseLong(numStr);
            switch (unit) {
                case 's': return java.time.Duration.ofSeconds(amount);
                case 'm': return java.time.Duration.ofMinutes(amount);
                case 'h': return java.time.Duration.ofHours(amount);
                case 'd': return java.time.Duration.ofDays(amount);
                case 'w': return java.time.Duration.ofDays(amount * 7);
                default:
                    // If no unit, treat as minutes
                    long mins = Long.parseLong(text);
                    return java.time.Duration.ofMinutes(mins);
            }
        } catch (Exception e) {
            return null;
        }
    }
}
