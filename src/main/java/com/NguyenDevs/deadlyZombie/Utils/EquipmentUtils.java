package com.NguyenDevs.deadlyZombie.Utils;

import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EquipmentUtils {
    private final ConfigManager configManager;
    private final EnchantmentUtils enchantmentUtils;

    public EquipmentUtils(ConfigManager configManager) {
        this.configManager = configManager;
        this.enchantmentUtils = new EnchantmentUtils(configManager);
    }

    public void equipWeapon(EntityEquipment equipment) {
        ConfigurationSection tools = configManager.getGearConfig().getConfigurationSection("tools");
        if (tools == null) return;

        double difficultyBonus = getDifficultyBonus();
        double randomVal = ThreadLocalRandom.current().nextDouble() * 100;

        for (String key : tools.getKeys(false)) {
            ConfigurationSection toolData = tools.getConfigurationSection(key);
            if (toolData == null) continue;

            double chance = toolData.getDouble("chance") + difficultyBonus;

            if (randomVal < chance) {
                Material mat = Material.matchMaterial(toolData.getString("material", "AIR"));
                if (mat != null && mat != Material.AIR) {
                    ItemStack item = new ItemStack(mat);

                    enchantmentUtils.applyEnchants(item);

                    equipment.setItemInMainHand(item);
                    equipment.setItemInMainHandDropChance((float) toolData.getDouble("drop-chance") / 100f);
                    return;
                }
            }
            randomVal -= chance;
        }
    }

    public void equipArmor(EntityEquipment equipment) {
        ConfigurationSection armorSection = configManager.getGearConfig().getConfigurationSection("armor");
        if (armorSection == null) return;

        double difficultyBonus = getDifficultyBonus();
        double randomVal = ThreadLocalRandom.current().nextDouble() * 100;

        for (String key : armorSection.getKeys(false)) {
            ConfigurationSection armorData = armorSection.getConfigurationSection(key);
            if (armorData == null) continue;

            double chance = armorData.getDouble("chance") + difficultyBonus;

            if (randomVal < chance) {
                List<String> pieces = armorData.getStringList("pieces");
                float dropChance = (float) armorData.getDouble("drop-chance") / 100f;

                for (String piece : pieces) {
                    Material mat = Material.matchMaterial(piece);
                    if (mat != null) {
                        ItemStack item = new ItemStack(mat);

                        enchantmentUtils.applyEnchants(item);

                        String name = mat.name();
                        if (name.endsWith("_HELMET")) {
                            equipment.setHelmet(item);
                            equipment.setHelmetDropChance(dropChance);
                        } else if (name.endsWith("_CHESTPLATE")) {
                            equipment.setChestplate(item);
                            equipment.setChestplateDropChance(dropChance);
                        } else if (name.endsWith("_LEGGINGS")) {
                            equipment.setLeggings(item);
                            equipment.setLeggingsDropChance(dropChance);
                        } else if (name.endsWith("_BOOTS")) {
                            equipment.setBoots(item);
                            equipment.setBootsDropChance(dropChance);
                        }
                    }
                }
                return;
            }
            randomVal -= chance;
        }
    }

    private double getDifficultyBonus() {
        return configManager.getConfig().getDouble("difficulty-bonus.NORMAL", 0.0);
    }
}