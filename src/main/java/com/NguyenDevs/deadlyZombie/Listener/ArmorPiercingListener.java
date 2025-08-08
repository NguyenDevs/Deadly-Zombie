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

import java.util.Random;

public class ArmorPiercingListener implements Listener {
    private final ConfigManager configManager;
    private static final Random RANDOM = new Random();

    public ArmorPiercingListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    private boolean isWorldDisabled(World world) {
        return configManager.getConfig().getStringList("disable-worlds").contains(world.getName());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof LivingEntity mob)) return;
        if (isWorldDisabled(player.getWorld())) return;

        var config = configManager.getConfig().getConfigurationSection("armor-piercing");
        if (config == null || !config.getBoolean("enabled", true)) return;

        double chance = config.getDouble("chance", 0.0) / 100.0;
        if (RANDOM.nextDouble() > chance) return;

        double damage = event.getDamage();

        event.setCancelled(true);
        player.damage(damage, mob);

        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, player.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
    }
}
