package com.NguyenDevs.deadlyZombie.Comp.worldguard;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardOff implements WGPlugin {

    @Override
    public boolean isPvpAllowed(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
        // Khi WorldGuard tắt, PvP được phép ở mọi nơi
        return true;
    }

    @Override
    public boolean isFlagAllowed(Player player, String flag) {
        if (player == null) {
            throw new IllegalArgumentException("Player cannot be null");
        }
        // Khi WorldGuard tắt, tất cả custom flags đều được phép
        // Trả về true để cho phép zombie break blocks
        return true;
    }

    @Override
    public boolean isFlagAllowedAtLocation(Location location, String flag) {
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null");
        }
        // Khi WorldGuard tắt, tất cả custom flags đều được phép tại mọi location
        // Trả về true để cho phép zombie break blocks
        return true;
    }
}