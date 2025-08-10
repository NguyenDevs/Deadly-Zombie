package com.NguyenDevs.deadlyZombie.Listener;

import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import com.NguyenDevs.deadlyZombie.Utils.EquipmentUtils;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
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

    private boolean shouldEquipZombie(Entity entity) {
        ConfigurationSection equipmentSection = configManager.getConfig().getConfigurationSection("equipment");
        if (equipmentSection == null || !equipmentSection.getBoolean("enable", false)) {
            return false;
        }

        if (entity instanceof PigZombie) {
            return equipmentSection.getBoolean("zombified-piglin", false);
        } else if (entity instanceof Drowned) {
            return equipmentSection.getBoolean("drowned", false);
        } else if (entity instanceof Husk) {
            return equipmentSection.getBoolean("husk", false);
        } else if (entity instanceof ZombieVillager) {
            return equipmentSection.getBoolean("zombie-villager", false);
        } else if (entity instanceof Zombie) {
            return equipmentSection.getBoolean("zombie", true);
        }

        return false;
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Entity entity = event.getEntity();

        if (!(entity instanceof Zombie) && !(entity instanceof PigZombie)) {
            return;
        }
        if (isWorldDisabled(entity.getWorld())) return;

        if (!shouldEquipZombie(entity)) return;

        EntityEquipment equipment = ((LivingEntity) entity).getEquipment();
        if (equipment == null) return;

        equipmentUtils.equipRandomWeapon(equipment);
        equipmentUtils.equipRandomArmor(equipment);
    }
}