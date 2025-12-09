package com.NguyenDevs.deadlyZombie.Utils;

import com.NguyenDevs.deadlyZombie.Manager.ConfigManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    public static String colorize(String message) {
        if (message == null) return "";

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of("#" + matcher.group(1)).toString());
        }
        matcher.appendTail(buffer);

        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static String getMessage(ConfigManager configManager, String path) {
        String prefix = configManager.getLanguageConfig().getString("prefix", "");
        String message = configManager.getLanguageConfig().getString(path, "");
        return colorize(prefix + message);
    }

    public static String getRawMessage(ConfigManager configManager, String path) {
        return configManager.getLanguageConfig().getString(path, "");
    }

    public static void send(CommandSender sender, ConfigManager configManager, String path) {
        sender.sendMessage(getMessage(configManager, path));
    }
}