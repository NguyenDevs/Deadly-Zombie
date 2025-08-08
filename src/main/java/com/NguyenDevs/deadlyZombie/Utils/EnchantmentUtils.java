package com.NguyenDevs.deadlyZombie.Utils;

import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public class EnchantmentUtils {
    private final ConfigManager configManager;
    private final Random random;

    public EnchantmentUtils(ConfigManager configManager) {
        this.configManager = configManager;
        this.random = new Random();
    }

    public void applyRandomEnchantments(ItemStack item, String toolType) {
        ConfigurationSection enchantSection = configManager.getEnchantsConfig().getConfigurationSection("enchants");
        if (enchantSection == null) return;

        for (String enchantName : enchantSection.getKeys(false)) {
            ConfigurationSection enchant = enchantSection.getConfigurationSection(enchantName);
            List<String> applicableTools = enchant.getStringList("applicable-tools");

            if (applicableTools.contains(toolType)) {
                double probability = enchant.getDouble("probability");
                if (random.nextDouble() < probability) {
                    Enchantment enchantment = Enchantment.getByName(enchantName.toUpperCase());
                    if (enchantment != null) {
                        int minLevel = enchant.getInt("min-level");
                        int maxLevel = enchant.getInt("max-level");
                        int level = random.nextInt(maxLevel - minLevel + 1) + minLevel;
                        item.addEnchantment(enchantment, level);
                    }
                }
            }
        }
    }

    public void applyRandomArmorEnchantments(ItemStack armor) {
        ConfigurationSection enchantSection = configManager.getEnchantsConfig().getConfigurationSection("enchants");
        if (enchantSection == null) return;

        String armorType = armor.getType().name().toLowerCase();
        String armorPiece = getArmorPiece(armorType);

        for (String enchantName : enchantSection.getKeys(false)) {
            ConfigurationSection enchant = enchantSection.getConfigurationSection(enchantName);
            List<String> applicableTools = enchant.getStringList("applicable-tools");
            List<String> applicableArmorPieces = enchant.getStringList("applicable-armor-pieces");

            if (applicableTools.contains("armor") && applicableArmorPieces.contains(armorPiece)) {
                double probability = enchant.getDouble("probability");
                if (random.nextDouble() < probability) {
                    Enchantment enchantment = Enchantment.getByName(enchantName.toUpperCase());
                    if (enchantment != null) {
                        int minLevel = enchant.getInt("min-level");
                        int maxLevel = enchant.getInt("max-level");
                        int level = random.nextInt(maxLevel - minLevel + 1) + minLevel;
                        try {
                            armor.addEnchantment(enchantment, level);
                        } catch (IllegalArgumentException e) {
                            configManager.getPlugin().getLogger().warning(
                                    "Không thể áp dụng phù phép " + enchantName + " cho " + armorType);
                        }
                    }
                }
            }
        }
    }

    private String getArmorPiece(String materialName) {
        if (materialName.endsWith("_helmet")) {
            return "helmet";
        } else if (materialName.endsWith("_chestplate")) {
            return "chestplate";
        } else if (materialName.endsWith("_leggings")) {
            return "leggings";
        } else if (materialName.endsWith("_boots")) {
            return "boots";
        }
        return "";
    }
}