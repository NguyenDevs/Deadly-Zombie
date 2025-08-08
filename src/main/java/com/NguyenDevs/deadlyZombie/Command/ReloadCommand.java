package com.NguyenDevs.deadlyZombie.Command;

import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {
    private final ConfigManager configManager;

    public ReloadCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (sender.hasPermission("zd.admin")) {
                configManager.reloadConfigs();
                sender.sendMessage("§8[§aDeadly§2Zombie§8] §7Configurations reloaded successfully.");
                return true;
            } else {
                sender.sendMessage("§8[§aDeadly§2Zombie§8] §cYou do not have permission to use this command.");
                return true;
            }
        }

        sender.sendMessage("§8[§aDeadly§2Zombie§8] §eUsage: /deadlyzombie reload");
        return true;
    }
}
