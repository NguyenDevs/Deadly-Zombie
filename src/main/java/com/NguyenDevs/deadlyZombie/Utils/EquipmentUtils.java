package com.NguyenDevs.deadlyZombie.Utils;

import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Random;

public class EquipmentUtils {
    private final ConfigManager configManager;
    private final EnchantmentUtils enchantmentUtils;
    private final JavaPlugin plugin;
    private final Random random;

    public EquipmentUtils(ConfigManager configManager, JavaPlugin plugin) {
        this.configManager = configManager;
        this.enchantmentUtils = new EnchantmentUtils(configManager);
        this.plugin = plugin;
        this.random = new Random();
    }

    private double getDifficultyCoefficient() {
        String difficulty = plugin.getServer().getWorlds().get(0).getDifficulty().name();
        return configManager.getConfig().getDouble("difficulty-coef." + difficulty, 0.0);
    }

    public void equipRandomWeapon(EntityEquipment equipment) {
        ConfigurationSection toolsSection = configManager.getToolsConfig().getConfigurationSection("tools");
        if (toolsSection == null) return;

        double difficultyCoef = getDifficultyCoefficient();
        double totalProbability = 0.0;

        for (String key : toolsSection.getKeys(false)) {
            double base = toolsSection.getDouble(key + ".probability", 0.0);
            if (!isNoneTool(toolsSection, key)) {
                base += difficultyCoef;
            }
            totalProbability += base;
        }

        double randomValue = random.nextDouble() * totalProbability;
        double current = 0.0;
        String selectedTool = null;

        for (String key : toolsSection.getKeys(false)) {
            double base = toolsSection.getDouble(key + ".probability", 0.0);
            if (!isNoneTool(toolsSection, key)) {
                base += difficultyCoef;
            }
            current += base;
            if (randomValue <= current) {
                selectedTool = key;
                break;
            }
        }

        if (selectedTool != null) {
            String materialName = toolsSection.getString(selectedTool + ".material");
            Material material = Material.matchMaterial(materialName);

            if (material == null) return;

            if (material == Material.AIR) {
                equipment.setItemInMainHand(null);
                equipment.setItemInMainHandDropChance(0f);
                return;
            }

            ItemStack weapon = new ItemStack(material);
            int maxDurability = material.getMaxDurability();
            if (maxDurability > 0) {
                weapon.setDurability((short) (random.nextInt(maxDurability)));
            }

            enchantmentUtils.applyRandomEnchantments(weapon, selectedTool);
            equipment.setItemInMainHand(weapon);
            equipment.setItemInMainHandDropChance((float) toolsSection.getDouble(selectedTool + ".drop-chance"));
        }
    }

    public void equipRandomArmor(EntityEquipment equipment) {
        ConfigurationSection armorSection = configManager.getArmorConfig().getConfigurationSection("armor");
        if (armorSection == null) return;

        double difficultyCoef = getDifficultyCoefficient();
        double totalProbability = 0.0;

        for (String key : armorSection.getKeys(false)) {
            double base = armorSection.getDouble(key + ".probability", 0.0);
            if (!isNoneArmor(armorSection, key)) {
                base += difficultyCoef;
            }
            totalProbability += base;
        }

        double randomValue = random.nextDouble() * totalProbability;
        double current = 0.0;
        String selectedSet = null;

        for (String key : armorSection.getKeys(false)) {
            double base = armorSection.getDouble(key + ".probability", 0.0);
            if (!isNoneArmor(armorSection, key)) {
                base += difficultyCoef;
            }
            current += base;
            if (randomValue <= current) {
                selectedSet = key;
                break;
            }
        }

        if (selectedSet == null) return;

        ConfigurationSection selectedArmor = armorSection.getConfigurationSection(selectedSet);
        if (selectedArmor == null) return;

        var pieces = selectedArmor.getStringList("pieces");
        if (pieces.isEmpty()) return;

        double dropChance = selectedArmor.getDouble("drop-chance", 0.0);

        for (String piece : pieces) {
            if (random.nextDouble() > 0.5) continue;

            Material material = Material.matchMaterial(piece);
            if (material == null) continue;

            ItemStack armorPiece = new ItemStack(material);
            int maxDurability = material.getMaxDurability();
            if (maxDurability > 0) {
                armorPiece.setDurability((short) random.nextInt(maxDurability));
            }

            enchantmentUtils.applyRandomArmorEnchantments(armorPiece);

            switch (material.name().toLowerCase()) {
                case "leather_helmet", "iron_helmet", "golden_helmet", "diamond_helmet", "netherite_helmet" -> {
                    equipment.setHelmet(armorPiece);
                    equipment.setHelmetDropChance((float) dropChance);
                }
                case "leather_chestplate", "iron_chestplate", "golden_chestplate", "diamond_chestplate", "netherite_chestplate" -> {
                    equipment.setChestplate(armorPiece);
                    equipment.setChestplateDropChance((float) dropChance);
                }
                case "leather_leggings", "iron_leggings", "golden_leggings", "diamond_leggings", "netherite_leggings" -> {
                    equipment.setLeggings(armorPiece);
                    equipment.setLeggingsDropChance((float) dropChance);
                }
                case "leather_boots", "iron_boots", "golden_boots", "diamond_boots", "netherite_boots" -> {
                    equipment.setBoots(armorPiece);
                    equipment.setBootsDropChance((float) dropChance);
                }
            }
        }
    }

    private boolean isNoneTool(ConfigurationSection toolsSection, String key) {
        String materialName = toolsSection.getString(key + ".material", "");
        return "AIR".equalsIgnoreCase(materialName);
    }

    private boolean isNoneArmor(ConfigurationSection armorSection, String key) {
        var pieces = armorSection.getStringList(key + ".pieces");
        return pieces == null || pieces.isEmpty();
    }
}
