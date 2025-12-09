package com.NguyenDevs.deadlyZombie.Utils;

import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EnchantmentUtils {
    private final ConfigManager configManager;

    public EnchantmentUtils(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void applyEnchants(ItemStack item) {
        if (item == null || item.getType().isAir()) return;

        ConfigurationSection enchantsConfig = configManager.getGearConfig().getConfigurationSection("enchants");
        if (enchantsConfig == null) return;

        String itemName = item.getType().name();

        for (String enchantKey : enchantsConfig.getKeys(false)) {
            ConfigurationSection data = enchantsConfig.getConfigurationSection(enchantKey);
            if (data == null) continue;

            List<String> targets = data.getStringList("targets");
            if (!isApplicable(itemName, targets)) continue;

            double chance = data.getDouble("chance");
            if (ThreadLocalRandom.current().nextDouble() * 100 < chance) {
                Enchantment enchant = Enchantment.getByName(enchantKey.toUpperCase());
                if (enchant != null) {
                    int min = data.getInt("min-level", 1);
                    int max = data.getInt("max-level", 1);
                    int level = ThreadLocalRandom.current().nextInt(min, max + 1);

                    try {
                        item.addEnchantment(enchant, level);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private boolean isApplicable(String materialName, List<String> targets) {
        for (String target : targets) {
            if (materialName.contains(target)) return true;

            if (target.equalsIgnoreCase("ARMOR")) {
                if (materialName.endsWith("_HELMET") ||
                        materialName.endsWith("_CHESTPLATE") ||
                        materialName.endsWith("_LEGGINGS") ||
                        materialName.endsWith("_BOOTS")) {
                    return true;
                }
            }
        }
        return false;
    }
}