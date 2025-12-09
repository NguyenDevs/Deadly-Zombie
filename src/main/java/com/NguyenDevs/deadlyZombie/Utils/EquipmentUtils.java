package com.NguyenDevs.deadlyZombie.Utils;

import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

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

        double totalWeight = 0.0;
        for (String key : tools.getKeys(false)) {
            totalWeight += tools.getDouble(key + ".chance");
        }

        double difficultyBonus = getDifficultyBonus();
        double chanceToHaveWeapon = totalWeight + difficultyBonus;

        if (ThreadLocalRandom.current().nextDouble() * 100 > chanceToHaveWeapon) {
            return;
        }

        double randomWeight = ThreadLocalRandom.current().nextDouble() * totalWeight;

        for (String key : tools.getKeys(false)) {
            double itemWeight = tools.getDouble(key + ".chance");

            if (randomWeight < itemWeight) {
                equipItem(equipment, tools.getConfigurationSection(key), true);
                return;
            }
            randomWeight -= itemWeight;
        }
    }

    public void equipArmor(EntityEquipment equipment) {
        ConfigurationSection armorSection = configManager.getGearConfig().getConfigurationSection("armor");
        if (armorSection == null) return;

        double difficultyBonus = getDifficultyBonus();

        double totalWeight = 0.0;
        for (String key : armorSection.getKeys(false)) {
            totalWeight += armorSection.getDouble(key + ".chance");
        }

        double chanceToHaveArmor = totalWeight + difficultyBonus;

        if (ThreadLocalRandom.current().nextDouble() * 100 > chanceToHaveArmor) {
            return;
        }

        double randomWeight = ThreadLocalRandom.current().nextDouble() * totalWeight;

        for (String key : armorSection.getKeys(false)) {
            double itemWeight = armorSection.getDouble(key + ".chance");

            if (randomWeight < itemWeight) {
                equipItemSet(equipment, armorSection.getConfigurationSection(key));
                return;
            }
            randomWeight -= itemWeight;
        }
    }

    private void equipItem(EntityEquipment equipment, ConfigurationSection data, boolean isMainHand) {
        if (data == null) return;
        Material mat = Material.matchMaterial(data.getString("material", "AIR"));
        if (mat == null || mat == Material.AIR) return;

        ItemStack item = new ItemStack(mat);
        enchantmentUtils.applyEnchants(item);

        float dropChance = (float) data.getDouble("drop-chance") / 100f;

        if (isMainHand) {
            equipment.setItemInMainHand(item);
            equipment.setItemInMainHandDropChance(dropChance);
        }
    }

    private void equipItemSet(EntityEquipment equipment, ConfigurationSection data) {
        if (data == null) return;
        float dropChance = (float) data.getDouble("drop-chance") / 100f;

        for (String piece : data.getStringList("pieces")) {
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
    }

    private double getDifficultyBonus() {
        return configManager.getConfig().getDouble("difficulty-bonus.NORMAL", 0.0);
    }
}