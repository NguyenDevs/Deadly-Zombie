package com.NguyenDevs.deadlyZombie.Manager;

import com.NguyenDevs.deadlyZombie.DeadlyZombie;
import com.NguyenDevs.deadlyZombie.Listener.ArmorPiercingListener;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Arrays;

public class ConfigManager {
    private final JavaPlugin plugin;
    private ArmorPiercingListener armorPiercingListener;
    private FileConfiguration config;
    private FileConfiguration toolsConfig;
    private FileConfiguration enchantsConfig;
    private FileConfiguration armorConfig;
    private FileConfiguration blocksConfig;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        // Load config.yml
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
        setupDefaultConfig();

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

    private void setupDefaultConfig() {
        config.addDefault("update-notify", true);

        // Equipment settings
        ConfigurationSection equipmentSection = config.getConfigurationSection("equipment");
        if (equipmentSection == null) {
            equipmentSection = config.createSection("equipment");
        }
        if (!equipmentSection.contains("enable")) {
            equipmentSection.set("enable", true);
        }
        if (!equipmentSection.contains("zombie")) {
            equipmentSection.set("zombie", true);
        }
        if (!equipmentSection.contains("drowned")) {
            equipmentSection.set("drowned", true);
        }
        if (!equipmentSection.contains("husk")) {
            equipmentSection.set("husk", true);
        }
        if (!equipmentSection.contains("zombified-piglin")) {
            equipmentSection.set("zombified-piglin", true);
        }
        if (!equipmentSection.contains("zombie-villager")) {
            equipmentSection.set("zombie-villager", false);
        }

        // Zombie break block
        config.addDefault("zombie-break-block.enabled", true);

        // Zombie rage
        config.addDefault("zombie-rage.enabled", true);
        config.addDefault("zombie-rage.rage-duration", 10.0);
        config.addDefault("zombie-rage.damage-level", 1);
        config.addDefault("zombie-rage.speed-level", 2);

        // Tanky monsters
        config.addDefault("tanky-monsters.enabled", true);
        ConfigurationSection tankyMonsters = config.getConfigurationSection("tanky-monsters.dmg-reduction-percent");
        if (tankyMonsters == null) {
            tankyMonsters = config.createSection("tanky-monsters.dmg-reduction-percent");
        }
        if (!tankyMonsters.contains("ZOMBIE")) {
            tankyMonsters.set("ZOMBIE", 30.0);
        }

        // Parasite summon
        config.addDefault("parasite-summon.enabled", true);
        config.addDefault("parasite-summon.chance-percent", 35.0);
        config.addDefault("parasite-summon.min", 1);
        config.addDefault("parasite-summon.max", 3);

        // Mob critical strikes
        config.addDefault("mob-critical-strikes.enabled", true);
        config.addDefault("mob-critical-strikes.damage-percent", 50.0);
        ConfigurationSection critChance = config.getConfigurationSection("mob-critical-strikes.crit-chance");
        if (critChance == null) {
            critChance = config.createSection("mob-critical-strikes.crit-chance");
        }
        if (!critChance.contains("ZOMBIE")) {
            critChance.set("ZOMBIE", 35.0);
        }

        // Armor piercing
        config.addDefault("armor-piercing.enabled", true);
        config.addDefault("armor-piercing.chance", 15);
        ConfigurationSection armorPiercingEffects = config.getConfigurationSection("armor-piercing.effects");
        if (armorPiercingEffects == null) {
            armorPiercingEffects = config.createSection("armor-piercing.effects");
        }
        armorPiercingEffects.addDefault("particles", true);
        armorPiercingEffects.addDefault("sound", true);

        // Disable worlds
        config.addDefault("disable-worlds", Arrays.asList("example", "example_nether", "example_the_end"));

        // Difficulty coefficients
        ConfigurationSection difficultyCoef = config.getConfigurationSection("difficulty-coef");
        if (difficultyCoef == null) {
            difficultyCoef = config.createSection("difficulty-coef");
        }
        if (!difficultyCoef.contains("EASY")) {
            difficultyCoef.set("EASY", 0.0);
        }
        if (!difficultyCoef.contains("NORMAL")) {
            difficultyCoef.set("NORMAL", 10.0);
        }
        if (!difficultyCoef.contains("HARD")) {
            difficultyCoef.set("HARD", 20.0);
        }

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    public void reloadConfigs() {
        cleanup();
        if (armorPiercingListener != null) {
            armorPiercingListener.cleanup();
        }
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        setupDefaultConfig();
        loadConfigs();
    }

    public void cleanup() {
        this.config = null;
        this.toolsConfig = null;
        this.enchantsConfig = null;
        this.armorConfig = null;
        this.blocksConfig = null;
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