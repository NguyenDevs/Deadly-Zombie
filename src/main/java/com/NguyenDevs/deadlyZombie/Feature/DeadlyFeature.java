package com.NguyenDevs.deadlyZombie.Feature;

import com.NguyenDevs.deadlyZombie.DeadlyZombie;
import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Listener;

public abstract class DeadlyFeature implements Listener {
    protected final DeadlyZombie plugin;
    protected final ConfigManager configManager;
    private final String featureName;

    public DeadlyFeature(DeadlyZombie plugin, String featureName) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.featureName = featureName;
    }

    public boolean shouldRun(World world) {
        if (!isEnabled()) return false;
        if (configManager.getConfig().getStringList("settings.disabled-worlds").contains(world.getName())) {
            return false;
        }
        return true;
    }

    public boolean isEnabled() {
        return configManager.getConfig().getBoolean("features." + featureName + ".enabled", true);
    }

    protected ConfigurationSection getFeatureConfig() {
        return configManager.getConfig().getConfigurationSection("features." + featureName);
    }

    public void cleanup() {}
}