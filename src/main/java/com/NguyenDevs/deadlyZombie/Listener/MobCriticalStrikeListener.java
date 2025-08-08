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

public class MobCriticalStrikeListener implements Listener {
    private final ConfigManager configManager;
    private static final Random RANDOM = new Random();

    public MobCriticalStrikeListener(ConfigManager configManager) {
        this.configManager = configManager;
    }
    private boolean isWorldDisabled(World world) {
        return configManager.getConfig().getStringList("disable-worlds").contains(world.getName());
    }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof LivingEntity mob)) return;
        if (isWorldDisabled(player.getWorld())) return;

        var config = configManager.getConfig().getConfigurationSection("mob-critical-strikes");
        if (config == null || !config.getBoolean("enabled", true)) return;

        double chance = config.getDouble("crit-chance." + mob.getType().name(), 0.0) / 100.0;
        if (RANDOM.nextDouble() > chance) return;

        // Play effects
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 2.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 32, 0, 0, 0, 0.5);
        player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation().add(0, 1, 0), 0);

        double multiplier = config.getDouble("damage-percent", 50.0) / 100.0;
        event.setDamage(event.getDamage() * (1 + multiplier));
    }
}

