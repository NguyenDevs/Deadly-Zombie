package com.NguyenDevs.deadlyZombie.Listener;

import com.NguyenDevs.deadlyZombie.DeadlyZombie;
import com.NguyenDevs.deadlyZombie.Feature.DeadlyFeature;
import com.NguyenDevs.deadlyZombie.Utils.EquipmentUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.List;

public class ZombieEquipmentHandler extends DeadlyFeature {
    private final EquipmentUtils equipmentUtils;

    public ZombieEquipmentHandler(DeadlyZombie plugin) {
        super(plugin, "equipment");
        this.equipmentUtils = new EquipmentUtils(plugin.getConfigManager());
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (!shouldRun(entity.getWorld())) return;

        List<String> targets = getFeatureConfig().getStringList("targets");
        if (targets.contains(entity.getType().name())) {
            if (entity.getEquipment() != null) {
                equipmentUtils.equipWeapon(entity.getEquipment());
                equipmentUtils.equipArmor(entity.getEquipment());
            }
        }
    }
}