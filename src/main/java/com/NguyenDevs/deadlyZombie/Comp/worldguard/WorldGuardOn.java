package com.NguyenDevs.deadlyZombie.Comp.worldguard;

import com.NguyenDevs.deadlyZombie.DeadlyZombie;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class WorldGuardOn implements WGPlugin {
    private final WorldGuard worldGuard;
    private final WorldGuardPlugin worldGuardPlugin;
    private final Map<String, StateFlag> customFlags;
    private final Set<String> failedFlags;
    private volatile boolean flagsRegistered = false;
    private static final String ZD_BREAK = "zd-break";
    private static final String ZD_RAGE = "zd-rage";

    public WorldGuardOn() {
        this.customFlags = new HashMap<>();
        this.failedFlags = new HashSet<>();
        this.worldGuard = WorldGuard.getInstance();

        WorldGuardPlugin plugin = (WorldGuardPlugin) DeadlyZombie.getInstance().getServer()
                .getPluginManager().getPlugin("WorldGuard");
        if (plugin == null) {
            throw new IllegalStateException("WorldGuard plugin not found. Disabling WorldGuard integration.");
        }
        this.worldGuardPlugin = plugin;
        DeadlyZombie.getInstance().getServer().getScheduler().runTaskLater(
                DeadlyZombie.getInstance(), this::registerCustomFlags, 1L);
    }

    private void registerCustomFlags() {
        FlagRegistry registry = worldGuard.getFlagRegistry();
        try {
            StateFlag breakFlag = (StateFlag) registry.get(ZD_BREAK);
            StateFlag rageFlag = (StateFlag) registry.get(ZD_RAGE);

            if (breakFlag != null) {
                customFlags.put(ZD_BREAK, breakFlag);
            } else {
                failedFlags.add(ZD_BREAK);
                DeadlyZombie.getInstance().getLogger().log(Level.WARNING, "Custom flag not found: " + ZD_BREAK);
            }

            if (rageFlag != null) {
                customFlags.put(ZD_RAGE, rageFlag);
            } else {
                failedFlags.add(ZD_RAGE);
                DeadlyZombie.getInstance().getLogger().log(Level.WARNING, "Custom flag not found: " + ZD_RAGE);
            }
        } catch (Exception e) {
            failedFlags.add(ZD_BREAK);
            failedFlags.add(ZD_RAGE);
            DeadlyZombie.getInstance().getLogger().log(Level.SEVERE,
                    "Error loading WorldGuard custom flags", e);
        }
        flagsRegistered = true;
    }


    @Override
    public boolean isPvpAllowed(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
        try {
            ApplicableRegionSet regions = getApplicableRegion(location);
            return regions.queryState(null, com.sk89q.worldguard.protection.flags.Flags.PVP) != StateFlag.State.DENY;
        } catch (Exception e) {
            DeadlyZombie.getInstance().getLogger().log(Level.WARNING,
                    "Error checking PvP state at location: " + location, e);
            return true;
        }
    }

    @Override
    public boolean isFlagAllowed(Player player, String flag) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        if (!flag.equals(ZD_BREAK) && !flag.equals(ZD_RAGE)) {
            throw new IllegalArgumentException("Only ZD_BREAK and ZD_RAGE flags are supported");
        }
        if (!flagsRegistered) {
            return true; // Default to allow if flags not registered
        }
        if (failedFlags.contains(flag)) {
            return true; // Default to allow if flag failed to load
        }
        try {
            ApplicableRegionSet regions = getApplicableRegion(player.getLocation());
            StateFlag stateFlag = customFlags.get(flag);
            if (stateFlag == null) {
                if (!failedFlags.contains(flag)) {
                    failedFlags.add(flag);
                    DeadlyZombie.getInstance().getLogger().log(Level.WARNING,
                            "Custom flag not found after registration: " + flag + " - Using default behavior");
                }
                return true;
            }
            StateFlag.State state = regions.queryValue(worldGuardPlugin.wrapPlayer(player), stateFlag);
            return state == null || state == StateFlag.State.ALLOW;
        } catch (Exception e) {
            DeadlyZombie.getInstance().getLogger().log(Level.WARNING,
                    "Error checking flag " + flag + " for player: " + player.getName(), e);
            return true;
        }
    }

    @Override
    public boolean isFlagAllowedAtLocation(Location location, String flag) {
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
        if (!flag.equals(ZD_BREAK) && !flag.equals(ZD_RAGE)) {
            throw new IllegalArgumentException("Only ZD_BREAK and ZD_RAGE flags are supported");
        }
        if (!flagsRegistered) {
            return true;
        }
        if (failedFlags.contains(flag)) {
            return true;
        }
        try {
            ApplicableRegionSet regions = getApplicableRegion(location);
            StateFlag stateFlag = customFlags.get(flag);
            if (stateFlag == null) {
                if (!failedFlags.contains(flag)) {
                    failedFlags.add(flag);
                    DeadlyZombie.getInstance().getLogger().log(Level.WARNING,
                            "Custom flag not found after registration: " + flag + " - Using default behavior");
                }
                return true;
            }
            StateFlag.State state = regions.queryState(null, stateFlag);
            return state == null || state == StateFlag.State.ALLOW;
        } catch (Exception e) {
            DeadlyZombie.getInstance().getLogger().log(Level.WARNING,
                    "Error checking flag " + flag + " at location: " + location, e);
            return true;
        }
    }


    private ApplicableRegionSet getApplicableRegion(Location location) {
        try {
            return worldGuard.getPlatform().getRegionContainer()
                    .createQuery()
                    .getApplicableRegions(BukkitAdapter.adapt(location));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to query WorldGuard regions at location: " + location, e);
        }
    }

    public boolean isReady() {
        return flagsRegistered;
    }

    public Map<String, StateFlag> getRegisteredFlags() {
        return new HashMap<>(customFlags);
    }
}