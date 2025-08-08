package com.NguyenDevs.deadlyZombie.Listener;

import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class TankyMonsterListener implements Listener {
    private final ConfigManager configManager;

    public TankyMonsterListener(ConfigManager configManager) {
        this.configManager = configManager;
    }
    private boolean isWorldDisabled(World world) {
        return configManager.getConfig().getStringList("disable-worlds").contains(world.getName());
    }
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity instanceof Player) return;
        if (isWorldDisabled(entity.getWorld())) return;

        var config = configManager.getConfig().getConfigurationSection("tanky-monsters");
        if (config == null || !config.getBoolean("enabled", true)) return;

        String path = "dmg-reduction-percent." + entity.getType().name();
        if (!config.contains(path)) return;

        double percent = config.getDouble(path);
        if (percent <= 0 || percent >= 100) return;

        double reduction = percent / 100.0;
        event.setDamage(event.getDamage() * (1 - reduction));
    }
}
