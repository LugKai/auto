package me.plugin.autoreplant.managers;

import me.plugin.autoreplant.AutoReplant;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final AutoReplant plugin;
    private final Map<UUID, Boolean> states = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public PlayerDataManager(AutoReplant plugin) {
        this.plugin = plugin;
        load();
    }

    private void load() {
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) {
                plugin.getLogger().severe("ไม่สามารถสร้าง playerdata.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (dataConfig.contains("players")) {
            for (String s : dataConfig.getConfigurationSection("players").getKeys(false)) {
                try { states.put(UUID.fromString(s), dataConfig.getBoolean("players." + s + ".enabled", true)); }
                catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void saveAll() {
        states.forEach((uuid, enabled) ->
            dataConfig.set("players." + uuid + ".enabled", enabled));
        try { dataConfig.save(dataFile); }
        catch (IOException e) { plugin.getLogger().severe("บันทึก playerdata.yml ไม่ได้: " + e.getMessage()); }
    }

    public boolean isEnabled(UUID uuid) {
        return states.computeIfAbsent(uuid, k ->
            plugin.getConfig().getBoolean("settings.default-enabled-for-new-players", true));
    }

    public boolean toggle(UUID uuid) {
        boolean next = !isEnabled(uuid);
        states.put(uuid, next);
        saveAll();
        return next;
    }
}
