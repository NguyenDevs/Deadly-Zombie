package com.NguyenDevs.deadlyZombie.Listener;

import com.NguyenDevs.deadlyZombie.DeadlyZombie;
import com.NguyenDevs.deadlyZombie.Feature.DeadlyFeature;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class ArmorPiercingListener extends DeadlyFeature {
    private final Map<UUID, Long> processingPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastEffectTime = new ConcurrentHashMap<>();
    private BukkitTask cleanupTask;

    private static final long PROCESSING_TIMEOUT = 1000L;
    private static final long EFFECT_COOLDOWN = 100L;
    private static final long CLEANUP_INTERVAL = 60 * 20L;

    public ArmorPiercingListener(DeadlyZombie plugin) {
        super(plugin, "armor-piercing");
        startAutoCleanup();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof LivingEntity mob)) return;
        if (!shouldRun(player.getWorld())) return;

        UUID playerId = player.getUniqueId();
        if (processingPlayers.containsKey(playerId)) {
            return;
        }

        ConfigurationSection config = getFeatureConfig();
        double chance = config.getDouble("chance", 15.0);

        if (ThreadLocalRandom.current().nextDouble() * 100 < chance) {
            bypassArmorAndDamage(event, player, mob, config);
        }
    }

    private void bypassArmorAndDamage(EntityDamageByEntityEvent event, Player player,
                                      LivingEntity damager, ConfigurationSection config) {
        event.setCancelled(true);
        double rawDamage = calculateRawMobDamage(damager);
        UUID playerId = player.getUniqueId();

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead() || !damager.isValid()) return;
                processingPlayers.put(playerId, System.currentTimeMillis());

                PlayerInventory equipment = player.getInventory();
                EquipmentSnapshot snapshot = null;

                try {
                    if (equipment != null) {
                        snapshot = new EquipmentSnapshot(equipment);
                        equipment.setHelmet(null);
                        equipment.setChestplate(null);
                        equipment.setLeggings(null);
                        equipment.setBoots(null);
                    }

                    PotionEffect resistance = player.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                    boolean hadResistance = resistance != null;
                    if (hadResistance) {
                        player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                    }

                    player.setNoDamageTicks(0);
                    player.damage(rawDamage, damager);

                    if (snapshot != null) {
                        snapshot.restore(equipment);
                    }

                    if (hadResistance) {
                        player.addPotionEffect(resistance);
                    }

                    applyEffects(player, config);

                } catch (Exception e) {
                    if (snapshot != null && equipment != null) {
                        snapshot.restore(equipment);
                    }
                    e.printStackTrace();
                } finally {
                    processingPlayers.remove(playerId);
                }
            }
        }.runTask(plugin);
    }

    private void applyEffects(Player player, ConfigurationSection config) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        Long lastEffect = lastEffectTime.get(playerId);
        if (lastEffect != null && currentTime - lastEffect < EFFECT_COOLDOWN) {
            return;
        }
        lastEffectTime.put(playerId, currentTime);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    if (config.getBoolean("visual.particles", true)) {
                        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                                player.getLocation().add(0, 1, 0), 5, 0.2, 0.3, 0.2, 0.1);
                    }

                    if (config.getBoolean("visual.sound", true)) {
                        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.8f);
                    }
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    private double calculateRawMobDamage(LivingEntity mob) {
        var damageAttr = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        double baseDamage = damageAttr != null ? damageAttr.getValue() : 1.0;
        return Math.min(Math.max(baseDamage, 0.5), 50.0);
    }

    private void startAutoCleanup() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                processingPlayers.entrySet().removeIf(entry -> currentTime - entry.getValue() > PROCESSING_TIMEOUT * 2);
                lastEffectTime.entrySet().removeIf(entry -> currentTime - entry.getValue() > EFFECT_COOLDOWN * 10);
            }
        }.runTaskTimerAsynchronously(plugin, CLEANUP_INTERVAL, CLEANUP_INTERVAL);
    }

    @Override
    public void cleanup() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }
        processingPlayers.clear();
        lastEffectTime.clear();
    }

    private static class EquipmentSnapshot {
        private final ItemStack helmet;
        private final ItemStack chestplate;
        private final ItemStack leggings;
        private final ItemStack boots;

        public EquipmentSnapshot(PlayerInventory equipment) {
            this.helmet = cloneItem(equipment.getHelmet());
            this.chestplate = cloneItem(equipment.getChestplate());
            this.leggings = cloneItem(equipment.getLeggings());
            this.boots = cloneItem(equipment.getBoots());
        }

        private ItemStack cloneItem(ItemStack item) {
            return item != null ? item.clone() : null;
        }

        public void restore(PlayerInventory equipment) {
            if (equipment == null) return;
            try {
                equipment.setHelmet(helmet);
                equipment.setChestplate(chestplate);
                equipment.setLeggings(leggings);
                equipment.setBoots(boots);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}