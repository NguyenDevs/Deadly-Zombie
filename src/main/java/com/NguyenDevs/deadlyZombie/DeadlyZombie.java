package com.NguyenDevs.deadlyZombie;

import com.NguyenDevs.deadlyZombie.Command.DeadlyZombieTabCompleter;
import com.NguyenDevs.deadlyZombie.Command.ReloadCommand;
import com.NguyenDevs.deadlyZombie.Comp.worldguard.WGPlugin;
import com.NguyenDevs.deadlyZombie.Comp.worldguard.WorldGuardOff;
import com.NguyenDevs.deadlyZombie.Comp.worldguard.WorldGuardOn;
import com.NguyenDevs.deadlyZombie.Listener.*;
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
    private boolean worldGuardReady = false;

    private ArmorPiercingListener armorPiercingListener;
    private TankyMonsterListener tankyMonsterListener;
    private ParasiteSummonListener parasiteSummonListener;
    private MobCriticalStrikeListener mobCriticalStrikeListener;
    private ZombieRageListener zombieRageListener;
    private ZombieEquipmentHandler zombieEquipmentHandler;
    private ZombieBreakBlockListener zombieBreakBlockListener;

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

        armorPiercingListener = new ArmorPiercingListener(configManager);
        tankyMonsterListener = new TankyMonsterListener(configManager);
        parasiteSummonListener = new ParasiteSummonListener(configManager);
        mobCriticalStrikeListener = new MobCriticalStrikeListener(configManager);
        zombieRageListener = new ZombieRageListener(configManager, this);
        zombieEquipmentHandler = new ZombieEquipmentHandler(configManager, this);
        zombieBreakBlockListener = new ZombieBreakBlockListener(configManager, this);

        getServer().getPluginManager().registerEvents(armorPiercingListener, this);
        getServer().getPluginManager().registerEvents(tankyMonsterListener, this);
        getServer().getPluginManager().registerEvents(parasiteSummonListener, this);
        getServer().getPluginManager().registerEvents(mobCriticalStrikeListener, this);
        getServer().getPluginManager().registerEvents(zombieRageListener, this);
        getServer().getPluginManager().registerEvents(zombieEquipmentHandler, this);
        getServer().getPluginManager().registerEvents(zombieBreakBlockListener, this);

        getCommand("deadlyzombie").setExecutor(new ReloadCommand(configManager));
        getCommand("deadlyzombie").setTabCompleter(new DeadlyZombieTabCompleter());

        UpdateChecker updateChecker = new UpdateChecker(127800, this);
        updateChecker.checkForUpdate();

        printLogo();
        Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&', "&8[&aDeadly&2Zombie&8] &aDeadlyZombie plugin enabled successfully!"));
    }

    @Override
    public void onDisable() {
        if (armorPiercingListener != null) {
            armorPiercingListener.cleanup();
        }
        if (tankyMonsterListener != null && tankyMonsterListener instanceof CleanupListener) {
            ((CleanupListener) tankyMonsterListener).cleanup();
        }
        if (parasiteSummonListener != null && parasiteSummonListener instanceof CleanupListener) {
            ((CleanupListener) parasiteSummonListener).cleanup();
        }
        if (mobCriticalStrikeListener != null && mobCriticalStrikeListener instanceof CleanupListener) {
            ((CleanupListener) mobCriticalStrikeListener).cleanup();
        }
        if (zombieRageListener != null && zombieRageListener instanceof CleanupListener) {
            ((CleanupListener) zombieRageListener).cleanup();
        }
        if (zombieEquipmentHandler != null && zombieEquipmentHandler instanceof CleanupListener) {
            ((CleanupListener) zombieEquipmentHandler).cleanup();
        }
        if (zombieBreakBlockListener != null && zombieBreakBlockListener instanceof CleanupListener) {
            ((CleanupListener) zombieBreakBlockListener).cleanup();
        }

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
                                            " custom flags."));
                        } else {
                            getServer().getScheduler().runTaskLater(this, () -> {
                                boolean delayedReady = wgOn.isReady();
                                int delayedFlagCount = wgOn.getRegisteredFlags().size();
                                if (delayedReady && delayedFlagCount > 0) {
                                    worldGuardReady = true;
                                    Bukkit.getConsoleSender().sendMessage(ChatColor.translateAlternateColorCodes('&',
                                            "&8[&aDeadly&2Zombie&8] &aWorldGuard integration ready with " +
                                                    delayedFlagCount + " custom flags."));
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