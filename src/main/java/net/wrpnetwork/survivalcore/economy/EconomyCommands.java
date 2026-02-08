package net.wrpnetwork.survivalcore.economy;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EconomyCommands implements CommandExecutor {

    private final SurvivalCore plugin;

    public EconomyCommands(SurvivalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores...");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "balance":
            case "money":
                plugin.getEconomyManager().sendBalanceMessage(player);
                break;
            case "shop":
                new ShopGUI(plugin).openShop(player); // Use the one from SurvivalCore ideally
                break;
            case "eco":
                if (!player.hasPermission("survivalcore.admin")) return true;
                if (args.length < 3) {
                    player.sendMessage("/eco <give/set> <player> <amount>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) return true;
                double amount = Double.parseDouble(args[2]);
                if (args[0].equalsIgnoreCase("give")) {
                    plugin.getEconomyManager().addBalance(target.getUniqueId(), amount);
                } else {
                    plugin.getEconomyManager().setBalance(target.getUniqueId(), amount);
                }
                player.sendMessage("Balance actualizado.");
                break;
        }

        return true;
    }
}
