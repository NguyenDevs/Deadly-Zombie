package com.NguyenDevs.deadlyZombie.Listener;

import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class ArmorPiercingListener implements Listener {
    private final ConfigManager configManager;
    private static final Random RANDOM = new Random();

    public ArmorPiercingListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    private boolean isWorldDisabled(World world) {
        return configManager.getConfig().getStringList("disable-worlds").contains(world.getName());
    }

    private final Set<UUID> bypassing = new HashSet<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof LivingEntity mob)) return;
        if (isWorldDisabled(player.getWorld())) return;
        if (bypassing.contains(player.getUniqueId())) return;

        var config = configManager.getConfig().getConfigurationSection("armor-piercing");
        if (config == null || !config.getBoolean("enabled", true)) return;

        double chance = config.getDouble("chance", 0.0) / 100.0;
        if (RANDOM.nextDouble() > chance) return;

        double damage = event.getDamage();

        boolean wouldDie = damage >= player.getHealth();

        try {
            bypassing.add(player.getUniqueId());
            player.setNoDamageTicks(0);

            var equipment = player.getEquipment();
            ItemStack helmet = equipment.getHelmet() != null ? equipment.getHelmet().clone() : null;
            ItemStack chest = equipment.getChestplate() != null ? equipment.getChestplate().clone() : null;
            ItemStack legs = equipment.getLeggings() != null ? equipment.getLeggings().clone() : null;
            ItemStack boots = equipment.getBoots() != null ? equipment.getBoots().clone() : null;

            equipment.setHelmet(null);
            equipment.setChestplate(null);
            equipment.setLeggings(null);
            equipment.setBoots(null);

            if (wouldDie) {
                event.setDamage(damage);
            } else {
                event.setCancelled(true);
                player.damage(damage, mob);
            }

            equipment.setHelmet(helmet);
            equipment.setChestplate(chest);
            equipment.setLeggings(legs);
            equipment.setBoots(boots);

        } finally {
            bypassing.remove(player.getUniqueId());
        }

        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
    }


}
