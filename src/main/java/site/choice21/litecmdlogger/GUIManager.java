package site.choice21.litecmdlogger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class GUIManager implements Listener {
    private final JavaPlugin plugin;
    private final DatabaseManager db;
    private final int pageSize;
    private final String title;

    public GUIManager(JavaPlugin plugin, DatabaseManager db) {
        this.plugin = plugin;
        this.db = db;
        this.pageSize = plugin.getConfig().getInt("gui.pageSize", 20);
        this.title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.title", "Command Logs"));
    }

    public void openCommandGUI(Player player, int page) {
        db.getCommandsPageAsync(page, pageSize, (entries, safePage, totalPages, totalCount) -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Inventory inv = Bukkit.createInventory(new CommandLogsView(safePage, totalPages), 54, title + " [" + safePage + "/" + totalPages + "]");
                buildPage(inv, entries, safePage, totalPages, totalCount);
                player.openInventory(inv);
            });
        });
    }

    private void buildPage(Inventory inv, List<CommandLogEntry> entries, int page, int totalPages, long totalCount) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.setDisplayName(" ");
        filler.setItemMeta(fm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        int[] contentSlots = contentSlots();
        int limit = Math.min(entries.size(), Math.min(pageSize, contentSlots.length));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (int i = 0; i < limit; i++) {
            CommandLogEntry e = entries.get(i);
            ItemStack paper = new ItemStack(Material.PAPER);
            ItemMeta meta = paper.getItemMeta();
            meta.setDisplayName(ChatColor.YELLOW + e.player() + ChatColor.GRAY + " ran:");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.WHITE + truncate(e.command(), 64));
            lore.add(ChatColor.DARK_GRAY + "Time: " + sdf.format(e.timestamp()));
            meta.setLore(lore);
            paper.setItemMeta(meta);
            inv.setItem(contentSlots[i], paper);
        }

        inv.setItem(45, navItem(Material.ARROW, ChatColor.GREEN + "Previous Page"));
        inv.setItem(49, navItem(Material.BARRIER, ChatColor.RED + "Close"));
        inv.setItem(53, navItem(Material.ARROW, ChatColor.GREEN + "Next Page"));

        inv.setItem(46, infoItem(ChatColor.AQUA + "Page " + page + "/" + totalPages));
        inv.setItem(52, infoItem(ChatColor.GRAY + "Total: " + totalCount));
    }

    private int[] contentSlots() {
        return new int[] {
                10,11,12,13,14,15,16,
                19,20,21,22,23,24,25,
                28,29,30,31,32,33,34
        };
    }

    private ItemStack navItem(Material mat, String name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(name);
        it.setItemMeta(im);
        return it;
    }

    private ItemStack infoItem(String name) {
        ItemStack it = new ItemStack(Material.OAK_SIGN);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(name);
        it.setItemMeta(im);
        return it;
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof CommandLogsView view)) return;

        event.setCancelled(true);

        int slot = event.getRawSlot();
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (slot == 45) {
            int target = Math.max(1, view.page - 1);
            if (target != view.page) openCommandGUI(player, target);
            return;
        }

        if (slot == 53) {
            int target = Math.min(view.totalPages, view.page + 1);
            if (target != view.page) openCommandGUI(player, target);
            return;
        }
    }

    private static class CommandLogsView implements InventoryHolder {
        final int page;
        final int totalPages;

        CommandLogsView(int page, int totalPages) {
            this.page = page;
            this.totalPages = totalPages;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
