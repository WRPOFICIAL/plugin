package net.wrpnetwork.survivalcore;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SurvivalTabCompleter implements TabCompleter {

    private final SurvivalCore plugin;

    public SurvivalTabCompleter(SurvivalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player player)) return completions;

        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "survivalcore":
                if (args.length == 1) {
                    completions.addAll(Arrays.asList("help", "status", "reload", "giveupgrade", "setshopitem"));
                } else if (args.length == 2 && args[0].equalsIgnoreCase("giveupgrade")) {
                    completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                }
                break;
            case "tpa":
                if (args.length == 1) {
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList()));
                }
                break;
            case "home":
                if (args.length == 1) {
                    completions.addAll(Arrays.asList("trust", "untrust", "info"));
                    completions.addAll(plugin.getDatabaseManager().getHomes(player.getUniqueId()));
                } else if (args.length == 2 && Arrays.asList("trust", "untrust", "info").contains(args[0].toLowerCase())) {
                    completions.addAll(plugin.getDatabaseManager().getHomes(player.getUniqueId()));
                } else if (args.length == 3 && Arrays.asList("trust", "untrust").contains(args[0].toLowerCase())) {
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList()));
                }
                break;
            case "delhome":
                if (args.length == 1) {
                    completions.addAll(plugin.getDatabaseManager().getHomes(player.getUniqueId()));
                }
                break;
            case "money":
            case "balance":
                if (args.length == 1 && player.hasPermission("survivalcore.admin")) {
                    completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                }
                break;
            case "eco":
                if (player.hasPermission("survivalcore.admin")) {
                    if (args.length == 1) {
                        completions.addAll(Arrays.asList("give", "set", "reset"));
                    } else if (args.length == 2) {
                        completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                    }
                }
                break;
            case "msg":
                if (args.length == 1) {
                    completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                }
                break;
            case "tumba":
                if (args.length == 1 && player.hasPermission("survivalcore.admin")) {
                    completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                }
                break;
            case "fly":
            case "buildmode":
            case "repair":
                // No arguments needed, but online players for fly if admin
                if (args.length == 1 && player.hasPermission("survivalcore.admin")) {
                    completions.addAll(Bukkit.getOnlinePlayers().stream()
                            .map(Player::getName)
                            .collect(Collectors.toList()));
                }
                break;
            case "clan":
                if (args.length == 1) {
                    completions.addAll(Arrays.asList("create", "invite", "accept", "sethome", "home", "bank", "kick", "leave", "delete"));
                } else if (args.length == 2) {
                    if (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick")) {
                        completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                    } else if (args[0].equalsIgnoreCase("bank")) {
                        completions.addAll(Arrays.asList("deposit", "withdraw"));
                    }
                }
                break;
        }

        return completions.stream()
                .filter(s -> s.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                .collect(Collectors.toList());
    }
}
