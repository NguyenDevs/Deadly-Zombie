package com.NguyenDevs.deadlyZombie;

import com.NguyenDevs.deadlyZombie.Command.DeadlyZombieTabCompleter;
import com.NguyenDevs.deadlyZombie.Command.ReloadCommand;
import com.NguyenDevs.deadlyZombie.Comp.worldguard.WGPlugin;
import com.NguyenDevs.deadlyZombie.Comp.worldguard.WorldGuardOff;
import com.NguyenDevs.deadlyZombie.Comp.worldguard.WorldGuardOn;
import com.NguyenDevs.deadlyZombie.Listener.*;
import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
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
    private boolean worldGuardReady = false;

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

        // Register event listeners

        getServer().getPluginManager().registerEvents(new ArmorPiercingListener(configManager), this);
        getServer().getPluginManager().registerEvents(new TankyMonsterListener(configManager), this);
        getServer().getPluginManager().registerEvents(new ParasiteSummonListener(configManager), this);
        getServer().getPluginManager().registerEvents(new MobCriticalStrikeListener(configManager), this);

        getServer().getPluginManager().registerEvents(new ZombieRageListener(configManager, this), this);
        getServer().getPluginManager().registerEvents(new ZombieEquipmentHandler(configManager, this), this);
        getServer().getPluginManager().registerEvents(new ZombieBreakBlockListener(configManager, this), this);

        // Register command
        getCommand("deadlyzombie").setExecutor(new ReloadCommand(configManager));
        getCommand("deadlyzombie").setTabCompleter(new DeadlyZombieTabCompleter());

        printLogo();
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&aDeadly&2Zombie&8] &aDeadlyZombie plugin enabled successfully!"));
    }

    @Override
    public void onDisable() {
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&aDeadly&2Zombie&8] &cDeadlyZombie plugin disabled!"));
    }

    private void registerWorldGuardFlags() {
        if (getServer().getPluginManager().getPlugin("WorldGuard") == null) {
            return;
        }
        try {
            FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();

            // Register zd-break flag
            String breakFlagPath = "zd-break";
            if (registry.get(breakFlagPath) == null) {
                StateFlag breakFlag = new StateFlag(breakFlagPath, true); // Default to ALLOW
                registry.register(breakFlag);
                getLogger().info("Registered WorldGuard flag: " + breakFlagPath);
            }

            // Register zd-rage flag
            String rageFlagPath = "zd-rage";
            if (registry.get(rageFlagPath) == null) {
                StateFlag rageFlag = new StateFlag(rageFlagPath, true); // Default to ALLOW
                registry.register(rageFlag);
                getLogger().info("Registered WorldGuard flag: " + rageFlagPath);
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
                org.bukkit.plugin.Plugin wgPlugin = getServer().getPluginManager().getPlugin("WorldGuard");
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&8[&aDeadly&2Zombie&8] &aWorldGuard version: " + wgPlugin.getDescription().getVersion()));
                try {
                    this.wgPlugin = new WorldGuardOn();
                    if (this.wgPlugin instanceof WorldGuardOn) {
                        WorldGuardOn wgOn = (WorldGuardOn) this.wgPlugin;
                        boolean isReady = wgOn.isReady();
                        int flagCount = wgOn.getRegisteredFlags().size();
                        if (isReady && flagCount > 0) {
                            worldGuardReady = true;
                            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&8[&aDeadly&2Zombie&8] &aWorldGuard integration ready with " + flagCount +
                                            " custom flags: zd-break, zd-rage"));
                        } else {
                            getServer().getScheduler().runTaskLater(this, () -> {
                                boolean delayedReady = wgOn.isReady();
                                int delayedFlagCount = wgOn.getRegisteredFlags().size();
                                if (delayedReady && delayedFlagCount > 0) {
                                    worldGuardReady = true;
                                    Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                                            "&8[&aDeadly&2Zombie&8] &aWorldGuard integration ready with " +
                                                    delayedFlagCount + " custom flags: zd-break, zd-rage"));
                                } else {
                                    getLogger().severe("WorldGuard integration failed - flags not loaded properly");
                                    getLogger().severe("Ready: " + delayedReady + ", Flag count: " + delayedFlagCount);
                                    worldGuardReady = false;
                                }
                            }, 40L);
                            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                                    "&8[&aDeadly&2Zombie&8] &6WorldGuard integration created, waiting for flags to load..."));
                        }
                    } else {
                        throw new IllegalStateException("WorldGuardOn instance creation failed");
                    }
                } catch (IllegalStateException e) {
                    getLogger().severe("Failed to initialize WorldGuardOn - " + e.getMessage());
                    Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&8[&aDeadly&2Zombie&8] &cWorldGuardOn failed: " + e.getMessage()));
                    Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&8[&aDeadly&2Zombie&8] &6Falling back to WorldGuardOff mode"));
                    this.wgPlugin = new WorldGuardOff();
                    worldGuardReady = true;
                } catch (NoClassDefFoundError e) {
                    getLogger().severe("WorldGuard classes not found - " + e.getMessage());
                    Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&8[&aDeadly&2Zombie&8] &cMissing WorldGuard dependencies"));
                    Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&8[&aDeadly&2Zombie&8] &6Falling back to WorldGuardOff mode"));
                    this.wgPlugin = new WorldGuardOff();
                    worldGuardReady = true;
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Unexpected error initializing WorldGuardOn", e);
                    Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&8[&aDeadly&2Zombie&8] &cUnexpected error: " + e.getClass().getSimpleName()));
                    Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&8[&aDeadly&2Zombie&8] &6Falling back to WorldGuardOff mode"));
                    this.wgPlugin = new WorldGuardOff();
                    worldGuardReady = true;
                }
            } else {
                Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&8[&aDeadly&2Zombie&8] &6WorldGuard not found, using fallback mode"));
                this.wgPlugin = new WorldGuardOff();
                worldGuardReady = true;
            }
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize WorldGuard integration", e);
            this.wgPlugin = new WorldGuardOff();
            worldGuardReady = true;
            Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&8[&aDeadly&2Zombie&8] &cForced fallback to WorldGuardOff due to initialization error"));
        }
    }

    public boolean isWorldGuardReady() {
        return worldGuardReady && wgPlugin != null;
    }

    public WGPlugin getWorldGuard() {
        if (wgPlugin == null) {
            getLogger().warning("WorldGuard plugin requested but not initialized, returning fallback");
            return new WorldGuardOff();
        }
        return wgPlugin;
    }

    public static DeadlyZombie getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

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