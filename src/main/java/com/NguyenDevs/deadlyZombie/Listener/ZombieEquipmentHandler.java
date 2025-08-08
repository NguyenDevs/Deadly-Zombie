package com.NguyenDevs.deadlyZombie.Listener;

import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import com.NguyenDevs.deadlyZombie.Utils.EquipmentUtils;
import org.bukkit.World;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.plugin.java.JavaPlugin;

public class ZombieEquipmentHandler implements Listener {
    private final ConfigManager configManager;
    private final EquipmentUtils equipmentUtils;

    public ZombieEquipmentHandler(ConfigManager configManager, JavaPlugin plugin) {
        this.configManager = configManager;
        this.equipmentUtils = new EquipmentUtils(configManager, plugin);
    }
    private boolean isWorldDisabled(World world) {
        return configManager.getConfig().getStringList("disable-worlds").contains(world.getName());
    }
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Zombie zombie)) return;
        if (isWorldDisabled(zombie.getWorld())) return;

        zombie = (Zombie) event.getEntity();
        EntityEquipment equipment = zombie.getEquipment();

        // Equip weapon
        equipmentUtils.equipRandomWeapon(equipment);

        // Equip armor
        equipmentUtils.equipRandomArmor(equipment);
    }
}