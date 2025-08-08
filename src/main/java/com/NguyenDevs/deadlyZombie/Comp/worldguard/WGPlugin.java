package com.NguyenDevs.deadlyZombie.Comp.worldguard;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface WGPlugin {
    boolean isPvpAllowed(Location location);
    boolean isFlagAllowed(Player player, String flag);
    boolean isFlagAllowedAtLocation(Location location, String flag);
}