package com.NguyenDevs.deadlyZombie.Listener;

import com.NguyenDevs.deadlyZombie.DeadlyZombie;
import com.NguyenDevs.deadlyZombie.Feature.DeadlyFeature;
import org.bukkit.ChatColor;
import org.bukkit.EntityEffect;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ArmorPiercingListener extends DeadlyFeature {
    private final Map<UUID, String> pendingDeathMessages = new ConcurrentHashMap<>();

    public ArmorPiercingListener(DeadlyZombie plugin) {
        super(plugin, "armor-piercing");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof LivingEntity mob)) return;
        if (!shouldRun(player.getWorld())) return;

        ConfigurationSection config = getFeatureConfig();
        double chance = config.getDouble("chance", 15.0);

        if (ThreadLocalRandom.current().nextDouble() * 100 < chance) {
            performTrueDamage(event, player, mob, config);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (pendingDeathMessages.containsKey(player.getUniqueId())) {
            String killerName = pendingDeathMessages.remove(player.getUniqueId());
            event.setDeathMessage(ChatColor.RED + player.getName() + " was pierced to death by " + killerName);
        }
    }

    private void performTrueDamage(EntityDamageByEntityEvent event, Player player,
                                   LivingEntity damager, ConfigurationSection config) {

        double rawDamage = getRawDamage(damager);

        event.setCancelled(true);

        double currentHealth = player.getHealth();
        double newHealth = currentHealth - rawDamage;

        if (newHealth <= 0) {
            String mobName = damager.getCustomName() != null ? damager.getCustomName() : damager.getType().name();
            pendingDeathMessages.put(player.getUniqueId(), mobName);

            player.setHealth(0);
        } else {
            player.setHealth(newHealth);
        }

        player.playEffect(EntityEffect.HURT);

        Vector knockback = player.getLocation().toVector().subtract(damager.getLocation().toVector()).normalize();
        player.setVelocity(knockback.multiply(0.4).setY(0.3));

        if (config.getBoolean("visual.particles", true)) {
            player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                    player.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.1);
            player.getWorld().spawnParticle(Particle.CRIT_MAGIC,
                    player.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.1);
        }

        if (config.getBoolean("visual.sound", true)) {
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
        }
    }

    private double getRawDamage(LivingEntity mob) {
        var damageAttr = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        return damageAttr != null ? damageAttr.getValue() : 2.0;
    }
}