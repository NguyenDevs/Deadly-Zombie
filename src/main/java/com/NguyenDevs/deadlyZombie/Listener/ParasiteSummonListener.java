package com.NguyenDevs.deadlyZombie.Listener;

import com.NguyenDevs.deadlyZombie.DeadlyZombie;
import com.NguyenDevs.deadlyZombie.Feature.DeadlyFeature;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class ParasiteSummonListener extends DeadlyFeature {

    public ParasiteSummonListener(DeadlyZombie plugin) {
        super(plugin, "parasite");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onZombieDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (!shouldRun(zombie.getWorld())) return;
        if (zombie.hasMetadata("NPC")) return;

        ConfigurationSection config = getFeatureConfig();
        double chance = config.getDouble("chance", 35.0);

        if (ThreadLocalRandom.current().nextDouble() * 100 < chance) {
            int min = config.getInt("amount.min", 1);
            int max = config.getInt("amount.max", 3);
            int count = min + ThreadLocalRandom.current().nextInt(Math.max(1, max - min + 1));

            for (int i = 0; i < count; i++) {
                Vector velocity = new Vector(ThreadLocalRandom.current().nextDouble() - 0.5, ThreadLocalRandom.current().nextDouble() - 0.5, ThreadLocalRandom.current().nextDouble() - 0.5);
                zombie.getWorld().spawnParticle(Particle.SMOKE_LARGE, zombie.getLocation(), 0);
                zombie.getWorld().spawnEntity(zombie.getLocation(), EntityType.SILVERFISH).setVelocity(velocity);
            }
        }
    }
}