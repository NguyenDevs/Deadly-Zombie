package com.NguyenDevs.deadlyZombie.Listener;

import com.NguyenDevs.deadlyZombie.DeadlyZombie;
import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public class ZombieRageListener implements Listener {
    private final ConfigManager configManager;
    private final JavaPlugin plugin;

    public ZombieRageListener(ConfigManager configManager, JavaPlugin plugin) {
        this.configManager = configManager;
        this.plugin = plugin;
    }
    private boolean isWorldDisabled(World world) {
        return configManager.getConfig().getStringList("disable-worlds").contains(world.getName());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onZombieDamage(EntityDamageEvent event) {
        try {
            if (!(event.getEntity() instanceof Zombie zombie)) return;
            if (isWorldDisabled(zombie.getWorld())) return;
            if (event.getDamage() <= 0 || zombie.hasMetadata("NPC")) return;

            if (!DeadlyZombie.getInstance().getWorldGuard().isFlagAllowedAtLocation(zombie.getLocation(), "zd-rage")) return;
            if (!isZombieRageEnabled()) return;

            applyZombieRage(zombie);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Lỗi khi xử lý EntityDamageEvent cho zombie: " + event.getEntity().getType(), e);
        }
    }

    private void applyZombieRage(Zombie zombie) {
        try {
            if (zombie == null || zombie.getHealth() <= 0) return;

            ConfigurationSection rageConfig = configManager.getConfig().getConfigurationSection("zombie-rage");
            if (rageConfig == null) return;

            double rageDurationSeconds = rageConfig.getDouble("rage-duration", 10.0);
            int rageDurationTicks = (int) (rageDurationSeconds * 20);
            int damageLevel = rageConfig.getInt("damage-level", 1);
            int speedLevel = rageConfig.getInt("speed-level", 1);

            // Hardcoded effects
            int particleCount = 6;
            double offset = 0.35;
            double heightOffset = 1.7;

            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        if (!zombie.isValid() || zombie.getHealth() <= 0) return;

                        Location loc = zombie.getLocation().add(0, heightOffset, 0);
                        World world = zombie.getWorld();

                        world.spawnParticle(Particle.VILLAGER_ANGRY, loc, particleCount, offset, offset, offset, 0);

                        zombie.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, rageDurationTicks, damageLevel));
                        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, rageDurationTicks, speedLevel));

                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING,
                                "Lỗi khi áp dụng Zombie Rage cho zombie: " + zombie.getUniqueId(), e);
                    }
                }
            }.runTask(plugin);


        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Lỗi trong phương thức applyZombieRage cho zombie: " + zombie.getUniqueId(), e);
        }
    }

    private boolean isZombieRageEnabled() {
        ConfigurationSection rageConfig = configManager.getConfig().getConfigurationSection("zombie-rage");
        return rageConfig != null && rageConfig.getBoolean("enabled", true);
    }
}
