package com.NguyenDevs.deadlyZombie.Manager;

import com.NguyenDevs.deadlyZombie.DeadlyZombie;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class ConfigManager {
    private final DeadlyZombie plugin;
    private FileConfiguration gearConfig;
    private FileConfiguration blocksConfig;

    public ConfigManager(DeadlyZombie plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.gearConfig = loadCustomConfig("gear.yml");
        this.blocksConfig = loadCustomConfig("blocks.yml");
    }

    private FileConfiguration loadCustomConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    public void reloadConfigs() {
        loadConfigs();
    }

    public FileConfiguration getConfig() { return plugin.getConfig(); }
    public FileConfiguration getGearConfig() { return gearConfig; }
    public FileConfiguration getBlocksConfig() { return blocksConfig; }

    public void cleanup() {
        this.gearConfig = null;
        this.blocksConfig = null;
    }
}