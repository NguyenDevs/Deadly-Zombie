package com.NguyenDevs.deadlyZombie.Listener;

import com.NguyenDevs.deadlyZombie.DeadlyZombie;
import com.NguyenDevs.deadlyZombie.Feature.DeadlyFeature;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.concurrent.ThreadLocalRandom;

public class MobCriticalStrikeListener extends DeadlyFeature {

    public MobCriticalStrikeListener(DeadlyZombie plugin) {
        super(plugin, "critical-strike");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof LivingEntity mob)) return;
        if (!shouldRun(player.getWorld())) return;

        ConfigurationSection config = getFeatureConfig();

        String path = "chance." + mob.getType().name();
        if (!config.contains(path)) return;

        double chance = config.getDouble(path);

        if (ThreadLocalRandom.current().nextDouble() * 100 < chance) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 2.0f, 1.0f);
            player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 32, 0, 0, 0, 0.5);
            player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation().add(0, 1, 0), 0);

            double multiplier = config.getDouble("bonus-damage", 50.0) / 100.0;
            event.setDamage(event.getDamage() * (1 + multiplier));
        }
    }
}