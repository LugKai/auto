package me.plugin.autoreplant;

import me.plugin.autoreplant.commands.AutoReplantCommand;
import me.plugin.autoreplant.listeners.CropHarvestListener;
import me.plugin.autoreplant.managers.PlayerDataManager;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoReplant extends JavaPlugin {

    private static AutoReplant instance;
    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        playerDataManager = new PlayerDataManager(this);
        getServer().getPluginManager().registerEvents(new CropHarvestListener(this), this);
        AutoReplantCommand cmd = new AutoReplantCommand(this);
        getCommand("autoreplant").setExecutor(cmd);
        getCommand("autoreplant").setTabCompleter(cmd);
        getLogger().info("AutoReplant v" + getDescription().getVersion() + " เปิดใช้งานแล้ว! (MC 1.21.4)");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) playerDataManager.saveAll();
        getLogger().info("AutoReplant ปิดใช้งานแล้ว!");
    }

    public static AutoReplant getInstance() { return instance; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public void reload() { reloadConfig(); }
}
