package com.NguyenDevs.deadlyZombie.Manager;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ConfigManager {
    private final JavaPlugin plugin;

    private FileConfiguration config;
    private FileConfiguration toolsConfig;
    private FileConfiguration enchantsConfig;
    private FileConfiguration armorConfig;
    private FileConfiguration blocksConfig;
    private FileConfiguration blockPropertiesConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        // Load config.yml
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();

        // Load tools.yml
        File toolsFile = new File(plugin.getDataFolder(), "tools.yml");
        if (!toolsFile.exists()) {
            plugin.saveResource("tools.yml", false);
        }
        toolsConfig = YamlConfiguration.loadConfiguration(toolsFile);

        // Load enchants.yml
        File enchantsFile = new File(plugin.getDataFolder(), "enchants.yml");
        if (!enchantsFile.exists()) {
            plugin.saveResource("enchants.yml", false);
        }
        enchantsConfig = YamlConfiguration.loadConfiguration(enchantsFile);

        // Load armor.yml
        File armorFile = new File(plugin.getDataFolder(), "armor.yml");
        if (!armorFile.exists()) {
            plugin.saveResource("armor.yml", false);
        }
        armorConfig = YamlConfiguration.loadConfiguration(armorFile);

        // Load blocks.yml
        File blocksFile = new File(plugin.getDataFolder(), "blocks.yml");
        if (!blocksFile.exists()) {
            plugin.saveResource("blocks.yml", false);
        }
        blocksConfig = YamlConfiguration.loadConfiguration(blocksFile);
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        loadConfigs();
    }


    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getToolsConfig() {
        return toolsConfig;
    }

    public FileConfiguration getEnchantsConfig() {
        return enchantsConfig;
    }

    public FileConfiguration getArmorConfig() {
        return armorConfig;
    }

    public FileConfiguration getBlocksConfig() {
        return blocksConfig;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }
}
