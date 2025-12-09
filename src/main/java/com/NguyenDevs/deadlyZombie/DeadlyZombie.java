package com.NguyenDevs.deadlyZombie;

import com.NguyenDevs.deadlyZombie.Command.DeadlyZombieTabCompleter;
import com.NguyenDevs.deadlyZombie.Command.ReloadCommand;
import com.NguyenDevs.deadlyZombie.Comp.worldguard.WGPlugin;
import com.NguyenDevs.deadlyZombie.Comp.worldguard.WorldGuardOff;
import com.NguyenDevs.deadlyZombie.Comp.worldguard.WorldGuardOn;
import com.NguyenDevs.deadlyZombie.Listener.*;
import com.NguyenDevs.deadlyZombie.Listener.ZombieEquipmentHandler;
import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import com.NguyenDevs.deadlyZombie.Utils.UpdateChecker;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagConflictException;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class DeadlyZombie extends JavaPlugin {
    private static DeadlyZombie instance;
    private ConfigManager configManager;
    private WGPlugin wgPlugin;

    @Override
    public void onLoad() {
        instance = this;
        registerWorldGuardFlags();
    }

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.loadConfigs();
        initializeWorldGuard();

        getServer().getPluginManager().registerEvents(new ArmorPiercingListener(this), this);
        getServer().getPluginManager().registerEvents(new TankyMonsterListener(this), this);
        getServer().getPluginManager().registerEvents(new ParasiteSummonListener(this), this);
        getServer().getPluginManager().registerEvents(new MobCriticalStrikeListener(this), this);
        getServer().getPluginManager().registerEvents(new ZombieRageListener(this), this);
        getServer().getPluginManager().registerEvents(new ZombieEquipmentHandler(this), this);
        getServer().getPluginManager().registerEvents(new ZombieBreakBlockListener(this), this);

        getCommand("deadlyzombie").setExecutor(new ReloadCommand(configManager));
        getCommand("deadlyzombie").setTabCompleter(new DeadlyZombieTabCompleter());

        UpdateChecker updateChecker = new UpdateChecker(127800, this);
        updateChecker.checkForUpdate();

        printLogo();
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&aDeadly&2Zombie&8] &aDeadlyZombie plugin enabled successfully!"));
    }

    @Override
    public void onDisable() {
        configManager.cleanup();
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&aDeadly&2Zombie&8] &cDeadlyZombie plugin disabled!"));
    }

    private void registerWorldGuardFlags() {
        if (getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            return;
        }
        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
            String breakFlagPath = "zd-break";
            if (registry.get(breakFlagPath) == null) {
                StateFlag breakFlag = new StateFlag(breakFlagPath, true);
                registry.register(breakFlag);
            }
            String rageFlagPath = "zd-rage";
            if (registry.get(rageFlagPath) == null) {
                StateFlag rageFlag = new StateFlag(rageFlagPath, true);
                registry.register(rageFlag);
            }
        } catch (FlagConflictException e) {
            getLogger().warning("Flag conflict while registering WorldGuard flags: " + e.getMessage());
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Unexpected error while registering WorldGuard flags", e);
        }
    }

    private void initializeWorldGuard() {
        try {
            if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                this.wgPlugin = new WorldGuardOn();
            } else {
                this.wgPlugin = new WorldGuardOff();
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize WorldGuard integration", e);
            this.wgPlugin = new WorldGuardOff();
        }
    }

    public static DeadlyZombie getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public WGPlugin getWorldGuard() { return wgPlugin; }

    public void printLogo() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&a   ██████╗ ███████╗ █████╗ ██████╗ ██╗     ██╗   ██╗"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&a   ██╔══██╗██╔════╝██╔══██╗██╔══██╗██║     ╚██╗ ██╔╝"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&a   ██║  ██║█████╗  ███████║██║  ██║██║      ╚████╔╝ "));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&a   ██║  ██║██╔══╝  ██╔══██║██║  ██║██║       ╚██╔╝  "));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&a   ██████╔╝███████╗██║  ██║██████╔╝███████╗   ██║   "));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&a   ╚═════╝ ╚══════╝╚═╝  ╚═╝╚═════╝ ╚══════╝   ╚═╝   "));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&2   ███████╗ ██████╗ ███╗   ███╗██████╗ ██╗███████╗"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&2   ╚══███╔╝██╔═══██╗████╗ ████║██╔══██╗██║██╔════╝"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&2     ███╔╝ ██║   ██║██╔████╔██║██████╔╝██║█████╗  "));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&2    ███╔╝  ██║   ██║██║╚██╔╝██║██╔══██╗██║██╔══╝  "));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&2   ███████╗╚██████╔╝██║ ╚═╝ ██║██████╔╝██║███████╗"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&2   ╚══════╝ ╚═════╝ ╚═╝     ╚═╝╚═════╝ ╚═╝╚══════╝"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&2         Deadly Zombie"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&6         Version " + getDescription().getVersion()));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&b         Development by NguyenDevs"));
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', ""));
    }
}