package net.wrpnetwork.hubstatus;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandHandler implements CommandExecutor, TabCompleter {

    private final WRPHubStatus plugin;
    private final StatusManager statusManager;

    public CommandHandler(WRPHubStatus plugin, StatusManager statusManager) {
        this.plugin = plugin;
        this.statusManager = statusManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.getMessage("usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "enable":
                if (!sender.hasPermission("wrphub.admin")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }
                plugin.getConfig().set("plugin.enabled", true);
                plugin.saveConfig();
                sender.sendMessage(plugin.getMessage("enabled"));
                break;

            case "disable":
                if (!sender.hasPermission("wrphub.admin")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }
                plugin.getConfig().set("plugin.enabled", false);
                plugin.saveConfig();
                sender.sendMessage(plugin.getMessage("disabled"));
                break;

            case "reload":
                if (!sender.hasPermission("wrphub.reload") && !sender.hasPermission("wrphub.admin")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }
                plugin.reloadConfig();
                plugin.reloadMessagesConfig();
                plugin.reloadSystems();
                sender.sendMessage(plugin.getMessage("reloaded"));
                break;

            case "status":
                sender.sendMessage(plugin.getMessage("status-display")
                        .replace("%state_color%", plugin.color(statusManager.getStateColor()))
                        .replace("%state%", statusManager.getCurrentState()));
                break;

            case "setstate":
                if (!sender.hasPermission("wrphub.setstate") && !sender.hasPermission("wrphub.admin")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.color("&cUsage: /wrphub setstate <ONLINE|OFFLINE|MANTENIMIENTO>"));
                    return true;
                }
                String newState = args[1].toUpperCase();
                if (newState.equals("ONLINE") || newState.equals("OFFLINE") || newState.equals("MANTENIMIENTO")) {
                    statusManager.setCurrentState(newState);
                    sender.sendMessage(plugin.getMessage("state-changed").replace("%state%", newState));
                } else {
                    sender.sendMessage(plugin.getMessage("invalid-state"));
                }
                break;

            default:
                sender.sendMessage(plugin.getMessage("usage"));
                break;
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("enable", "disable", "reload", "status", "setstate").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("setstate")) {
            return Arrays.asList("ONLINE", "OFFLINE", "MANTENIMIENTO").stream()
                    .filter(s -> s.startsWith(args[1].toUpperCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
