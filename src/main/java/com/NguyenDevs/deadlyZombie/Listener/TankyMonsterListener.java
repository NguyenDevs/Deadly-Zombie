package com.NguyenDevs.deadlyZombie.Listener;

import com.NguyenDevs.deadlyZombie.DeadlyZombie;
import com.NguyenDevs.deadlyZombie.Feature.DeadlyFeature;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

public class TankyMonsterListener extends DeadlyFeature {

    public TankyMonsterListener(DeadlyZombie plugin) {
        super(plugin, "tanky");
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity instanceof Player) return;
        if (!shouldRun(entity.getWorld())) return;

        ConfigurationSection config = getFeatureConfig();
        String path = "reduction-percentage." + entity.getType().name();
        if (!config.contains(path)) return;

        double percent = config.getDouble(path);
        if (percent <= 0 || percent >= 100) return;

        double reduction = percent / 100.0;
        event.setDamage(event.getDamage() * (1 - reduction));
    }
}