package com.NguyenDevs.deadlyZombie.Listener;

import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.util.Vector;

import java.util.Random;

public class ParasiteSummonListener implements Listener {
    private final ConfigManager configManager;
    private static final Random RANDOM = new Random();

    public ParasiteSummonListener(ConfigManager configManager) {
        this.configManager = configManager;
    }
    private boolean isWorldDisabled(World world) {
        return configManager.getConfig().getStringList("disable-worlds").contains(world.getName());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onZombieDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (isWorldDisabled(zombie.getWorld())) return;
        if (zombie.hasMetadata("NPC")) return;

        var config = configManager.getConfig().getConfigurationSection("parasite-summon");
        if (config == null || !config.getBoolean("enabled", true)) return;

        double chance = config.getDouble("chance-percent", 10.0) / 100.0;
        int min = config.getInt("min", 1);
        int max = config.getInt("max", 3);

        if (RANDOM.nextDouble() > chance) return;

        int count = min + RANDOM.nextInt(Math.max(1, max - min + 1));

        for (int i = 0; i < count; i++) {
            Vector velocity = new Vector(RANDOM.nextDouble() - 0.5, RANDOM.nextDouble() - 0.5, RANDOM.nextDouble() - 0.5);
            zombie.getWorld().spawnParticle(Particle.SMOKE_LARGE, zombie.getLocation(), 0);
            zombie.getWorld().spawnEntity(zombie.getLocation(), EntityType.SILVERFISH).setVelocity(velocity);
        }
    }
}
