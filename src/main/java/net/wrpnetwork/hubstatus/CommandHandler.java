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

            case "menu":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("Only players can open the menu.");
                    return true;
                }
                if (!sender.hasPermission("wrphub.admin")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }
                new StatusGUI(plugin, statusManager).open((Player) sender);
                break;

            case "status":
                if (args.length < 2) {
                    for (StatusManager.ServerData data : statusManager.getServerDataMap().values()) {
                        sender.sendMessage(plugin.color("&7Server &e" + data.getDisplayName() + "&7: " + statusManager.getStateColor(data.getCurrentState()) + data.getCurrentState()));
                    }
                    return true;
                }
                StatusManager.ServerData data = statusManager.getServerData(args[1].toLowerCase());
                if (data == null) {
                    sender.sendMessage(plugin.color("&cServidor no encontrado."));
                    return true;
                }
                sender.sendMessage(plugin.getMessage("status-display")
                        .replace("%state_color%", plugin.color(statusManager.getStateColor(data.getCurrentState())))
                        .replace("%state%", data.getCurrentState()));
                break;

            case "setstate":
                if (!sender.hasPermission("wrphub.setstate") && !sender.hasPermission("wrphub.admin")) {
                    sender.sendMessage(plugin.getMessage("no-permission"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(plugin.color("&cUsage: /wrphub setstate <server> <ONLINE|OFFLINE|MANTENIMIENTO>"));
                    return true;
                }
                StatusManager.ServerData serverData = statusManager.getServerData(args[1].toLowerCase());
                if (serverData == null) {
                    sender.sendMessage(plugin.color("&cServidor no encontrado."));
                    return true;
                }
                String newState = args[2].toUpperCase();
                if (newState.equals("ONLINE") || newState.equals("OFFLINE") || newState.equals("MANTENIMIENTO")) {
                    serverData.setCurrentState(newState);
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
            return Arrays.asList("enable", "disable", "reload", "status", "setstate", "menu").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("setstate") || args[0].equalsIgnoreCase("status"))) {
            return new ArrayList<>(plugin.getStatusManager().getServerDataMap().keySet());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("setstate")) {
            return Arrays.asList("ONLINE", "OFFLINE", "MANTENIMIENTO").stream()
                    .filter(s -> s.startsWith(args[2].toUpperCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
