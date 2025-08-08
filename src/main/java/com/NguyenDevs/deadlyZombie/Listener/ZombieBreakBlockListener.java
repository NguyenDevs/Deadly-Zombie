package com.NguyenDevs.deadlyZombie.Listener;

import com.NguyenDevs.deadlyZombie.DeadlyZombie;
import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class ZombieBreakBlockListener implements Listener {
    private final ConfigManager configManager;
    private final JavaPlugin plugin;
    private final Map<UUID, BukkitRunnable> activeBreakingTasks = new HashMap<>();

    public ZombieBreakBlockListener(ConfigManager configManager, JavaPlugin plugin) {
        this.configManager = configManager;
        this.plugin = plugin;

        // Zombie Break Block loop
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    for (World world : plugin.getServer().getWorlds()) {
                        for (Zombie zombie : world.getEntitiesByClass(Zombie.class)) {
                            if (isWorldDisabled(zombie.getWorld())) continue;

                            if (configManager.getConfig().getBoolean("zombie-break-block.enabled", true)
                                    && zombie.getTarget() instanceof Player
                                    && DeadlyZombie.getInstance().getWorldGuard().isFlagAllowedAtLocation(zombie.getLocation(), "zd-break")) {
                                applyZombieBreakBlock(zombie);
                            }
                        }

                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error in Zombie Break Block loop task", e);
                }
            }
        }.runTaskTimer(plugin, 0, 20); // Kiểm tra mỗi 20 tick (1 giây)
    }
    private boolean isWorldDisabled(World world) {
        return configManager.getConfig().getStringList("disable-worlds").contains(world.getName());
    }
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Zombie) {
            Zombie zombie = (Zombie) event.getEntity();
            cleanupZombieBreaking(zombie);
        }
    }

    private void applyZombieBreakBlock(Zombie zombie) {
        try {
            if (zombie == null || zombie.getHealth() <= 0) {
                return;
            }
            if (isWorldDisabled(zombie.getWorld())) return;


            UUID zombieUUID = zombie.getUniqueId();
            if (activeBreakingTasks.containsKey(zombieUUID)) {
                return;
            }

            Player target = null;
            if (zombie.getTarget() instanceof Player) {
                target = (Player) zombie.getTarget();
            } else {
                for (Player player : zombie.getWorld().getPlayers()) {
                    if (player.isOnline() && zombie.getLocation().distanceSquared(player.getLocation()) <= 1600) {
                        target = player;
                        zombie.setTarget(target);
                        break;
                    }
                }
            }
            if (target == null || !target.getWorld().equals(zombie.getWorld()) ||
                    zombie.getLocation().distanceSquared(target.getLocation()) > 1600) {
                return;
            }

            ItemStack itemInHand = zombie.getEquipment().getItemInMainHand();
            Material toolType = itemInHand != null ? itemInHand.getType() : Material.AIR;
            String toolCategory = getToolCategory(toolType);
            if (toolCategory == null) {
                return;
            }

            Location zombieEyeLoc = zombie.getEyeLocation();
            Vector direction = target.getLocation().toVector().subtract(zombieEyeLoc.toVector()).normalize();
            Block targetBlock = null;

            BlockIterator iterator = new BlockIterator(zombie.getWorld(), zombieEyeLoc.toVector(), direction, 0, 3);
            while (iterator.hasNext()) {
                Block block = iterator.next();
                if (block.getType().isSolid() && canBreakBlock(toolCategory, block.getType())) {
                    targetBlock = block;
                    break;
                }
                if (!block.getType().isSolid()) {
                    Block blockBelow = block.getRelative(0, -1, 0);
                    if (blockBelow.getType().isSolid() && canBreakBlock(toolCategory, blockBelow.getType())) {
                        targetBlock = blockBelow;
                        break;
                    }
                }
            }
            if (targetBlock == null) {
                zombie.setTarget(target);
                return;
            }

            double breakTimeTicks = calculateBreakTime(toolType, targetBlock.getType());
            if (breakTimeTicks <= 0) {
                return;
            }

            Block finalTargetBlock = targetBlock;
            Player finalTarget = target;

            int randomSwingOffset = (int) (Math.random() * 10);
            int randomSoundOffset = (int) (Math.random() * 8);
            int randomParticleOffset = (int) (Math.random() * 12);

            BukkitRunnable breakTask = new BukkitRunnable() {
                int ticksElapsed = 0;
                ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
                int entityId = (int) (Math.random() * Integer.MAX_VALUE);

                int swingInterval = Math.max(8, (int) (breakTimeTicks / 8)) + (int) (Math.random() * 4 - 2);
                int hitSoundInterval = Math.max(10, (int) (breakTimeTicks / 6)) + (int) (Math.random() * 4 - 2);
                int particleInterval = Math.max(15, (int) (breakTimeTicks / 5)) + (int) (Math.random() * 6 - 3);

                @Override
                public void run() {
                    try {
                        if (!zombie.isValid() || !finalTarget.isOnline() ||
                                zombie.getLocation().distanceSquared(finalTarget.getLocation()) > 1600 ||
                                !finalTargetBlock.getType().isSolid() || !canBreakBlock(toolCategory, finalTargetBlock.getType())) {
                            activeBreakingTasks.remove(zombieUUID);
                            sendBreakAnimationPacket(finalTarget, entityId, finalTargetBlock, -1);
                            cancel();
                            return;
                        }
                        zombie.setTarget(finalTarget);

                        ticksElapsed++;
                        if (ticksElapsed < breakTimeTicks) {
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    try {
                                        int destroyStage = Math.min(9, (int) ((ticksElapsed / breakTimeTicks) * 10));
                                        sendBreakAnimationPacket(finalTarget, entityId, finalTargetBlock, destroyStage);

                                        if ((ticksElapsed + randomSwingOffset) % swingInterval == 0) {
                                            zombie.swingMainHand();
                                        }

                                        if ((ticksElapsed + randomSoundOffset) % hitSoundInterval == 0) {
                                            Sound hitSound = finalTargetBlock.getType().createBlockData().getSoundGroup().getHitSound();
                                            zombie.getWorld().playSound(finalTargetBlock.getLocation(), hitSound, 0.4f,
                                                    0.8f + (float) (Math.random() * 0.4));
                                        }

                                        if ((ticksElapsed + randomParticleOffset) % particleInterval == 0 && destroyStage >= 2) {
                                            zombie.getWorld().spawnParticle(Particle.BLOCK_CRACK,
                                                    finalTargetBlock.getLocation().add(0.5, 0.5, 0.5),
                                                    2, 0.1, 0.1, 0.1, 0.02, finalTargetBlock.getBlockData());
                                        }
                                    } catch (Exception e) {
                                        plugin.getLogger().log(Level.WARNING,
                                                "Lỗi trong animation phá block cho zombie: " + zombie.getUniqueId(), e);
                                    }
                                }
                            }.runTask(plugin);
                            return;
                        }

                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                try {
                                    if (finalTargetBlock.getType().isSolid()) {
                                        zombie.swingMainHand();

                                        Sound breakSound = finalTargetBlock.getType().createBlockData().getSoundGroup().getBreakSound();
                                        zombie.getWorld().playSound(finalTargetBlock.getLocation(), breakSound, 0.8f, 1.0f);

                                        sendBreakAnimationPacket(finalTarget, entityId, finalTargetBlock, -1);

                                        zombie.getWorld().spawnParticle(Particle.BLOCK_CRACK,
                                                finalTargetBlock.getLocation().add(0.5, 0.5, 0.5),
                                                25, 0.3, 0.3, 0.3, 0.1, finalTargetBlock.getBlockData());

                                        finalTargetBlock.breakNaturally(itemInHand);
                                    }

                                    activeBreakingTasks.remove(zombieUUID);
                                } catch (Exception e) {
                                    plugin.getLogger().log(Level.WARNING,
                                            "Lỗi khi phá block cho zombie: " + zombie.getUniqueId(), e);
                                    activeBreakingTasks.remove(zombieUUID);
                                }
                            }
                        }.runTaskLater(plugin, 0);
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING,
                                "Lỗi trong tác vụ Zombie Break Block cho zombie: " + zombie.getUniqueId(), e);
                        activeBreakingTasks.remove(zombieUUID);
                        sendBreakAnimationPacket(finalTarget, entityId, finalTargetBlock, -1);
                        cancel();
                    }
                }

                private void sendBreakAnimationPacket(Player targetPlayer, int entityId, Block block, int destroyStage) {
                    try {
                        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.BLOCK_BREAK_ANIMATION);
                        packet.getIntegers().write(0, entityId);
                        packet.getBlockPositionModifier().write(0, new BlockPosition(block.getX(), block.getY(), block.getZ()));
                        packet.getIntegers().write(1, destroyStage);
                        for (Player nearbyPlayer : block.getWorld().getPlayers()) {
                            if (nearbyPlayer.isOnline() && nearbyPlayer.getLocation().distanceSquared(block.getLocation()) <= 1024) {
                                protocolManager.sendServerPacket(nearbyPlayer, packet);
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.WARNING,
                                "Lỗi khi gửi packet animation phá block cho zombie: " + zombie.getUniqueId(), e);
                    }
                }
            };
            activeBreakingTasks.put(zombieUUID, breakTask);
            breakTask.runTaskTimerAsynchronously(plugin, 0, 1);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING,
                    "Lỗi khi áp dụng Zombie Break Block cho zombie: " + zombie.getUniqueId(), e);
        }
    }

    public void cleanupZombieBreaking(Zombie zombie) {
        UUID zombieUUID = zombie.getUniqueId();
        BukkitRunnable task = activeBreakingTasks.remove(zombieUUID);
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    private String getToolCategory(Material toolType) {
        String materialName = toolType.name().toLowerCase();
        if (materialName.contains("_axe")) {
            return "axe";
        } else if (materialName.contains("_pickaxe")) {
            return "pickaxe";
        } else if (materialName.contains("_shovel")) {
            return "shovel";
        }
        return null;
    }

    private boolean canBreakBlock(String toolCategory, Material blockType) {
        ConfigurationSection blocksSection = configManager.getBlocksConfig().getConfigurationSection("blocks");
        if (blocksSection == null) return false;

        List<String> breakableBlocks = blocksSection.getStringList(toolCategory);
        return breakableBlocks != null && breakableBlocks.contains(blockType.name());
    }

    private double calculateBreakTime(Material toolType, Material blockType) {
        double baseTicks = 20.0;
        if (toolType.name().contains("NETHERITE")) {
            baseTicks *= 0.6;
        } else if (toolType.name().contains("DIAMOND")) {
            baseTicks *= 0.7;
        } else if (toolType.name().contains("IRON")) {
            baseTicks *= 0.8;
        } else if (toolType.name().contains("GOLD")) {
            baseTicks *= 0.9;
        } else if (toolType.name().contains("WOOD")) {
            baseTicks *= 1.0;
        }

        if (blockType == Material.OBSIDIAN) {
            baseTicks *= 3.0;
        } else if (blockType.name().contains("DEEPSLATE")) {
            baseTicks *= 1.5;
        } else if (blockType.name().contains("STONE") || blockType.name().contains("ORE")) {
            baseTicks *= 1.2;
        }

        return baseTicks;
    }
}