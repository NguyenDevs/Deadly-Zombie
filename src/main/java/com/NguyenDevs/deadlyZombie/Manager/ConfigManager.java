package com.NguyenDevs.deadlyZombie.Manager;

import com.NguyenDevs.deadlyZombie.DeadlyZombie;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class ConfigManager {
    private final DeadlyZombie plugin;
    private FileConfiguration gearConfig;
    private FileConfiguration blocksConfig;
    private FileConfiguration languageConfig;
    private FileConfiguration breakBlockConfig;

    public ConfigManager(DeadlyZombie plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.gearConfig = loadCustomConfig("gear.yml");
        this.blocksConfig = loadCustomConfig("blocks.yml");
        this.languageConfig = loadCustomConfig("language.yml");
        this.breakBlockConfig = createBreakBlockSection();
    }

    private FileConfiguration loadCustomConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
        return YamlConfiguration.loadConfiguration(file);
    }

    private FileConfiguration createBreakBlockSection() {
        YamlConfiguration wrapper = new YamlConfiguration();
        ConfigurationSection breakBlock = plugin.getConfig().getConfigurationSection("features.break-block");

        if (breakBlock != null) {
            for (String key : breakBlock.getKeys(false)) {
                wrapper.set(key, breakBlock.get(key));
            }
        } else {
            wrapper.set("max-target-distance", 150.0);
            wrapper.set("drop-blocks", true);
            wrapper.set("drop-remove-interval", 30.0);
        }

        return wrapper;
    }

    public void reloadConfigs() {
        loadConfigs();
    }

    public FileConfiguration getConfig() {
        return plugin.getConfig();
    }

    public FileConfiguration getGearConfig() {
        return gearConfig;
    }

    public FileConfiguration getBlocksConfig() {
        return blocksConfig;
    }

    public FileConfiguration getLanguageConfig() {
        return languageConfig;
    }

    public FileConfiguration getBreakBlockConfig() {
        return breakBlockConfig;
    }

    public boolean isFeatureEnabled(String featureName) {
        return plugin.getConfig().getBoolean("features." + featureName + ".enabled", false);
    }

    public List<String> getDisabledWorlds() {
        return plugin.getConfig().getStringList("settings.disabled-worlds");
    }

    public double getDifficultyBonus(String difficulty) {
        return plugin.getConfig().getDouble("difficulty-bonus." + difficulty, 0.0);
    }

    public double getMaxTargetDistance() {
        return breakBlockConfig.getDouble("max-target-distance", 150.0);
    }

    public boolean shouldDropBlocks() {
        return breakBlockConfig.getBoolean("drop-blocks", true);
    }

    public double getDropRemoveInterval() {
        return breakBlockConfig.getDouble("drop-remove-interval", 30.0);
    }

    public ConfigurationSection getGearSection(String path) {
        return gearConfig.getConfigurationSection(path);
    }

    public List<String> getBreakableBlocks(String toolType) {
        return blocksConfig.getStringList("blocks." + toolType);
    }

    public String getMessage(String key) {
        return languageConfig.getString(key, "Missing message: " + key);
    }

    public String getMessage(String key, String... replacements) {
        String message = getMessage(key);
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }
        return message;
    }

    public void cleanup() {
        this.gearConfig = null;
        this.blocksConfig = null;
        this.languageConfig = null;
        this.breakBlockConfig = null;
    }
}