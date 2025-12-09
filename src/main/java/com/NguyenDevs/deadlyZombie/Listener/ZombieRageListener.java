package com.NguyenDevs.deadlyZombie.Listener;

import com.NguyenDevs.deadlyZombie.DeadlyZombie;
import com.NguyenDevs.deadlyZombie.Feature.DeadlyFeature;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public class ZombieRageListener extends DeadlyFeature {

    public ZombieRageListener(DeadlyZombie plugin) {
        super(plugin, "rage");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onZombieDamage(EntityDamageEvent event) {
        try {
            if (!(event.getEntity() instanceof Zombie zombie)) return;
            if (!shouldRun(zombie.getWorld())) return;
            if (event.getDamage() <= 0 || zombie.hasMetadata("NPC")) return;

            if (!plugin.getWorldGuard().isFlagAllowedAtLocation(zombie.getLocation(), "zd-rage")) return;

            applyZombieRage(zombie);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error in ZombieRageListener", e);
        }
    }

    private void applyZombieRage(Zombie zombie) {
        try {
            if (zombie == null || zombie.getHealth() <= 0) return;

            ConfigurationSection config = getFeatureConfig();
            double rageDurationSeconds = config.getDouble("duration", 10.0);
            int rageDurationTicks = (int) (rageDurationSeconds * 20);
            int damageLevel = config.getInt("effects.strength", 1);
            int speedLevel = config.getInt("effects.speed", 2);

            int particleCount = 6;
            double offset = 0.35;
            double heightOffset = 1.7;

            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        if (!zombie.isValid() || zombie.getHealth() <= 0) return;

                        if (config.getBoolean("visual.particles")) {
                            Location loc = zombie.getLocation().add(0, heightOffset, 0);
                            World world = zombie.getWorld();
                            world.spawnParticle(Particle.VILLAGER_ANGRY, loc, particleCount, offset, offset, offset, 0);
                        }

                        zombie.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, rageDurationTicks, damageLevel));
                        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, rageDurationTicks, speedLevel));

                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING, "Error applying rage effects", e);
                    }
                }
            }.runTask(plugin);

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error applying rage", e);
        }
    }
}