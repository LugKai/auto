package me.plugin.autoreplant.commands;

import me.plugin.autoreplant.AutoReplant;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class AutoReplantCommand implements CommandExecutor, TabCompleter {

    private final AutoReplant plugin;

    public AutoReplantCommand(AutoReplant plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { sendHelp(sender); return true; }

        switch (args[0].toLowerCase()) {
            case "toggle" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage(c("&cIn-game only!")); return true; }
                if (!p.hasPermission("autoreplant.toggle")) { p.sendMessage(c(plugin.getConfig().getString("messages.no-permission", "&cไม่มีสิทธิ์!"))); return true; }
                boolean on = plugin.getPlayerDataManager().toggle(p.getUniqueId());
                p.sendMessage(c(plugin.getConfig().getString(on ? "messages.toggle-on" : "messages.toggle-off", on ? "&aเปิดแล้ว!" : "&cปิดแล้ว!")));
            }
            case "reload" -> {
                if (!sender.hasPermission("autoreplant.admin")) { sender.sendMessage(c(plugin.getConfig().getString("messages.no-permission", "&cไม่มีสิทธิ์!"))); return true; }
                plugin.reload();
                sender.sendMessage(c(plugin.getConfig().getString("messages.reloaded", "&aโหลดใหม่แล้ว!")));
            }
            case "info" -> {
                sender.sendMessage(c("&8&l━━━━━━━━━━━━━━━━━━━━"));
                sender.sendMessage(c("&a&lAutoReplant &7v" + plugin.getDescription().getVersion()));
                sender.sendMessage(c("&7รองรับ Minecraft &e1.21.4"));
                sender.sendMessage(c("&7ต้องถือ &eHoe &7ถึงจะเก็บและปลูกคืนได้"));
                if (sender instanceof Player p) {
                    boolean on = plugin.getPlayerDataManager().isEnabled(p.getUniqueId());
                    sender.sendMessage(c("&7สถานะ: " + (on ? "&aเปิด ✓" : "&cปิด ✗")));
                }
                sender.sendMessage(c("&8&l━━━━━━━━━━━━━━━━━━━━"));
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage(c("&8&l━━━━━━━━━━━━━━━━━━━━"));
        s.sendMessage(c("&a/ar toggle &7- เปิด/ปิด"));
        s.sendMessage(c("&a/ar info &7- ดูข้อมูล"));
        if (s.hasPermission("autoreplant.admin")) s.sendMessage(c("&a/ar reload &7- โหลดใหม่"));
        s.sendMessage(c("&8&l━━━━━━━━━━━━━━━━━━━━"));
    }

    private String c(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String a, String[] args) {
        if (args.length == 1) return s.hasPermission("autoreplant.admin")
                ? Arrays.asList("toggle", "info", "reload") : Arrays.asList("toggle", "info");
        return List.of();
    }
}
