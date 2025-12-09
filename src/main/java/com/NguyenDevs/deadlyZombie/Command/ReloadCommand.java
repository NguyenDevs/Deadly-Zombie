package com.NguyenDevs.deadlyZombie.Command;

import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import com.NguyenDevs.deadlyZombie.Utils.MessageUtils;
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
                MessageUtils.send(sender, configManager, "admin.reload-success");
                return true;
            } else {
                MessageUtils.send(sender, configManager, "admin.no-permission");
                return true;
            }
        }

        MessageUtils.send(sender, configManager, "admin.usage");
        return true;
    }
}