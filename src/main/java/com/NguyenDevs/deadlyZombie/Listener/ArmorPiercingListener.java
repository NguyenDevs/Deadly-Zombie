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

public class ArmorPiercingListener implements Listener {
    private final ConfigManager configManager;
    private static final Random RANDOM = new Random();

    // Map này dùng để chặn đệ quy (khi zombie đánh -> kích hoạt event -> plugin đánh -> kích hoạt event -> lặp vô tận)
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

        // Kiểm tra nếu đang xử lý player này (do chính plugin gây sát thương) thì bỏ qua ngay
        if (processingPlayers.containsKey(playerId)) {
            return;
        }

        var config = configManager.getConfig().getConfigurationSection("armor-piercing");
        if (config == null || !config.getBoolean("enabled", true)) return;

        double chance = config.getDouble("chance", 0.0) / 100.0;
        if (RANDOM.nextDouble() > chance) return;

        // Gọi hàm xử lý
        bypassArmorAndDamage(event, player, mob, config);
    }

    private void bypassArmorAndDamage(EntityDamageByEntityEvent event, Player player,
                                      LivingEntity damager, org.bukkit.configuration.ConfigurationSection config) {

        // 1. Hủy sự kiện gốc NGAY LẬP TỨC để tránh Minecraft tính toán sát thương mặc định
        event.setCancelled(true);

        // Tính toán sát thương gốc
        double rawDamage = calculateRawMobDamage(damager);
        UUID playerId = player.getUniqueId();

        // 2. Chuyển việc gây sát thương sang tick tiếp theo (Sync Task)
        // Điều này giúp tách biệt hoàn toàn 2 sự kiện chết, sửa lỗi double message
        new BukkitRunnable() {
            @Override
            public void run() {
                // Kiểm tra lại tính hợp lệ
                if (!player.isOnline() || player.isDead() || !damager.isValid()) return;

                // Đánh dấu đang xử lý để event mới sinh ra từ player.damage() bị chặn ở onPlayerDamaged
                processingPlayers.put(playerId, System.currentTimeMillis());

                PlayerInventory equipment = player.getInventory();
                EquipmentSnapshot snapshot = null;

                try {
                    // Logic tháo giáp
                    if (equipment != null) {
                        snapshot = new EquipmentSnapshot(equipment);
                        equipment.setHelmet(null);
                        equipment.setChestplate(null);
                        equipment.setLeggings(null);
                        equipment.setBoots(null);
                    }

                    // Xóa kháng cự
                    PotionEffect resistance = player.getPotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                    boolean hadResistance = resistance != null;
                    if (hadResistance) {
                        player.removePotionEffect(PotionEffectType.DAMAGE_RESISTANCE);
                    }

                    // Gây sát thương (Lúc này event gốc đã hủy xong hoàn toàn)
                    player.setNoDamageTicks(0);
                    player.damage(rawDamage, damager);

                    // Khôi phục giáp
                    if (snapshot != null) {
                        snapshot.restore(equipment);
                    }

                    // Khôi phục kháng cự
                    if (hadResistance) {
                        player.addPotionEffect(resistance);
                    }

                    // Hiệu ứng
                    applyEffects(player, config);

                } catch (Exception e) {
                    // Fallback an toàn: cố gắng trả lại đồ nếu có lỗi
                    if (snapshot != null && equipment != null) {
                        snapshot.restore(equipment);
                    }
                    e.printStackTrace();
                } finally {
                    // Xóa đánh dấu xử lý
                    processingPlayers.remove(playerId);
                }
            }
        }.runTask(configManager.getPlugin());
    }

    private void applyEffects(Player player, org.bukkit.configuration.ConfigurationSection config) {
        // ... (Giữ nguyên logic effect, không cần thay đổi vì nó chỉ là visual)
        if (!player.isOnline()) return;

        // Không cần check cooldown quá gắt gao ở đây vì logic chính đã chạy ở tick sau
        if (config.getBoolean("effects.particles", true)) {
            player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR,
                    player.getLocation().add(0, 1, 0), 5, 0.2, 0.3, 0.2, 0.1);
        }

        if (config.getBoolean("effects.sound", true)) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.8f);
        }
    }

    private void startAutoCleanup() {
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                // Chỉ cần cleanup processingPlayers nếu có lỗi kẹt task (rất hiếm khi dùng try-finally)
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
        // ... (Giữ nguyên class này)
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

    public void cleanup() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }
        processingPlayers.clear();
        lastEffectTime.clear();
    }
}