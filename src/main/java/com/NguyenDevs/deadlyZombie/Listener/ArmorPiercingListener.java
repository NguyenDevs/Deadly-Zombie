package com.NguyenDevs.deadlyZombie.Listener;

import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ArmorPiercingListener implements Listener, CleanupListener {
    private final ConfigManager configManager;
    private static final Random RANDOM = new Random();

    private final Map<UUID, Long> processingPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastEffectTime = new ConcurrentHashMap<>();

    private BukkitTask cleanupTask;

    private static final long PROCESSING_TIMEOUT = 1000L;
    private static final long EFFECT_COOLDOWN = 100L;
    private static final long CLEANUP_INTERVAL = 60 * 20L;

    public ArmorPiercingListener(ConfigManager configManager) {
        this.configManager = configManager;
        startAutoCleanup();
    }

    private boolean isWorldDisabled(World world) {
        return configManager.getConfig().getStringList("disable-worlds").contains(world.getName());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof LivingEntity mob)) return;
        if (isWorldDisabled(player.getWorld())) return;

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        Long processingTime = processingPlayers.get(playerId);
        if (processingTime != null) {
            if (currentTime - processingTime < PROCESSING_TIMEOUT) {
                return;
            } else {
                processingPlayers.remove(playerId);
            }
        }

        var config = configManager.getConfig().getConfigurationSection("armor-piercing");
        if (config == null || !config.getBoolean("enabled", true)) return;

        double chance = config.getDouble("chance", 0.0) / 100.0;
        if (RANDOM.nextDouble() > chance) return;

        processingPlayers.put(playerId, currentTime);

        try {
            bypassArmorAndDamage(event, player, mob, config);
        } finally {
            processingPlayers.remove(playerId);
        }
    }

    private void bypassArmorAndDamage(EntityDamageByEntityEvent event, Player player,
                                      LivingEntity damager, org.bukkit.configuration.ConfigurationSection config) {

        double rawDamage = calculateRawMobDamage(damager);
        event.setCancelled(true);

        PlayerInventory equipment = player.getInventory();
        if (equipment == null) {
            player.damage(rawDamage, damager);
            return;
        }

        EquipmentSnapshot snapshot = new EquipmentSnapshot(equipment);

        try {
            equipment.setHelmet(null);
            equipment.setChestplate(null);
            equipment.setLeggings(null);
            equipment.setBoots(null);

            PotionEffect resistance = player.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            boolean hadResistance = resistance != null;
            if (hadResistance) {
                player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
            }

            player.setNoDamageTicks(0);
            player.damage(rawDamage, damager);

            snapshot.restore(equipment);

            if (hadResistance) {
                player.addPotionEffect(resistance);
            }

            applyEffects(player, config);

        } catch (Exception e) {
            snapshot.restore(equipment);
            e.printStackTrace();
        }
    }

    private void applyEffects(Player player, org.bukkit.configuration.ConfigurationSection config) {
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
                    if (config.getBoolean("effects.particles", true)) {
                        player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                                player.getLocation().add(0, 1, 0), 5, 0.2, 0.3, 0.2, 0.1);
                    }

                    if (config.getBoolean("effects.sound", true)) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.8f);
                    }
                }
            }
        }.runTaskLater(configManager.getPlugin(), 1L);
    }

    private void startAutoCleanup() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();

                processingPlayers.entrySet().removeIf(entry ->
                        currentTime - entry.getValue() > PROCESSING_TIMEOUT * 2);

                lastEffectTime.entrySet().removeIf(entry ->
                        currentTime - entry.getValue() > EFFECT_COOLDOWN * 10);
            }
        }.runTaskTimerAsynchronously(configManager.getPlugin(), CLEANUP_INTERVAL, CLEANUP_INTERVAL);
    }

    private double calculateRawMobDamage(LivingEntity mob) {
        var damageAttr = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        double baseDamage = damageAttr != null ? damageAttr.getValue() : 1.0;

        return Math.min(Math.max(baseDamage, 0.5), 50.0);
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

    @Override
    public void cleanup() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }
        processingPlayers.clear();
        lastEffectTime.clear();
    }
}