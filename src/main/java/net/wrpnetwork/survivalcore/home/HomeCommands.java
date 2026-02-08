package net.wrpnetwork.survivalcore.home;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class HomeCommands implements CommandExecutor {

    private final SurvivalCore plugin;

    public HomeCommands(SurvivalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "sethome":
                handleSetHome(player, args);
                break;
            case "home":
                handleHome(player, args);
                break;
            case "homes":
                handleListHomes(player);
                break;
            case "delhome":
                handleDelHome(player, args);
                break;
        }

        return true;
    }

    private void handleSetHome(Player player, String[] args) {
        String name = args.length > 0 ? args[0].toLowerCase() : "home";
        List<String> currentHomes = plugin.getDatabaseManager().getHomes(player.getUniqueId());

        if (!currentHomes.contains(name)) {
            int limit = getHomeLimit(player);
            if (currentHomes.size() >= limit) {
                plugin.getMessageManager().sendMessage(player, "<red>✖ Has alcanzado tu límite de hogares (" + limit + ").</red>");
                return;
            }
        }

        plugin.getDatabaseManager().saveHome(player.getUniqueId(), name, player.getLocation());
        plugin.getMessageManager().sendMessage(player, plugin.getConfig().getString("messages.home-saved", "<green>✔ Hogar guardado correctamente.</green>"));
    }

    private void handleHome(Player player, String[] args) {
        String name = args.length > 0 ? args[0].toLowerCase() : "home";
        Location loc = plugin.getDatabaseManager().getHome(player.getUniqueId(), name);

        if (loc == null) {
            plugin.getMessageManager().sendMessage(player, "<red>✖ No existe un hogar con ese nombre.</red>");
            return;
        }

        // Use TeleportManager if available
        if (plugin.getConfig().getBoolean("modules.teleport", true)) {
            // Need to expose teleportManager or find it
            // For now, I'll use a direct teleport or get it from instance if I add a getter
            plugin.getTeleportManager().teleport(player, loc);
        } else {
            player.teleport(loc);
            plugin.getMessageManager().sendMessage(player, "<green>✔ Teletransportado a tu hogar.</green>");
        }
    }

    private void handleListHomes(Player player) {
        List<String> homes = plugin.getDatabaseManager().getHomes(player.getUniqueId());
        if (homes.isEmpty()) {
            plugin.getMessageManager().sendMessage(player, "<yellow>No tienes hogares guardados.</yellow>");
            return;
        }

        StringBuilder sb = new StringBuilder("<blue>🏠 Tus hogares:</blue> <grey>");
        for (int i = 0; i < homes.size(); i++) {
            sb.append("<green>").append(homes.get(i)).append("</green>");
            if (i < homes.size() - 1) sb.append(", ");
        }
        sb.append("</grey>");
        plugin.getMessageManager().sendRawMessage(player, sb.toString());
    }

    private void handleDelHome(Player player, String[] args) {
        String name = args.length > 0 ? args[0].toLowerCase() : "home";
        if (plugin.getDatabaseManager().getHome(player.getUniqueId(), name) == null) {
            plugin.getMessageManager().sendMessage(player, "<red>✖ No existe un hogar con ese nombre.</red>");
            return;
        }

        plugin.getDatabaseManager().deleteHome(player.getUniqueId(), name);
        plugin.getMessageManager().sendMessage(player, plugin.getConfig().getString("messages.home-deleted", "<green>✔ Hogar eliminado correctamente.</green>"));
    }

    private int getHomeLimit(Player player) {
        if (player.hasPermission("survivalcore.homes.unlimited")) return 1000;

        int max = plugin.getConfig().getInt("homes.default-limit", 1);

        if (plugin.getConfig().getConfigurationSection("homes.limits") != null) {
            Set<String> keys = plugin.getConfig().getConfigurationSection("homes.limits").getKeys(false);
            for (String key : keys) {
                if (player.hasPermission("survivalcore.homes." + key)) {
                    int limit = plugin.getConfig().getInt("homes.limits." + key);
                    if (limit > max) max = limit;
                }
            }
        }

        return max;
    }
}
