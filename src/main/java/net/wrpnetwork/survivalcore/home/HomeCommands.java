package net.wrpnetwork.survivalcore.home;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.UUID;

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
            case "trust":
                handleTrust(player, args);
                break;
            case "untrust":
                handleUntrust(player, args);
                break;
            case "info":
                handleInfo(player, args);
                break;
        }

        return true;
    }

    private void handleSetHome(Player player, String[] args) {
        String name = args.length > 0 ? args[0].toLowerCase() : "home";
        List<String> currentHomes = plugin.getDatabaseManager().getHomes(player.getUniqueId());

        int currentLevel = 1;
        net.wrpnetwork.survivalcore.database.DatabaseManager.HomeInfo existing = plugin.getDatabaseManager().getHome(player.getUniqueId(), name);
        if (existing != null) {
            currentLevel = existing.level();
        } else {
            int limit = getHomeLimit(player);
            if (currentHomes.size() >= limit) {
                plugin.getMessageManager().sendMessage(player, "<red>✖ Has alcanzado tu límite de hogares (" + limit + ").</red>");
                return;
            }
        }

        // Place core block
        Material coreMat = Material.valueOf(plugin.getConfig().getString("homes.core-blocks.level-" + currentLevel, "COPPER_BLOCK"));
        player.getLocation().getBlock().setType(coreMat);

        plugin.getDatabaseManager().saveHome(player.getUniqueId(), name, player.getLocation(), currentLevel);
        plugin.getProtectionManager().updateHome(player.getUniqueId(), name, player.getLocation(), currentLevel);
        plugin.getMessageManager().sendMessage(player, plugin.getConfig().getString("messages.home-saved", "<green>✔ Hogar guardado correctamente. ¡Se ha colocado un núcleo de nivel " + currentLevel + "!</green>"));
    }

    private void handleHome(Player player, String[] args) {
        String name = args.length > 0 ? args[0].toLowerCase() : "home";
        net.wrpnetwork.survivalcore.database.DatabaseManager.HomeInfo info = plugin.getDatabaseManager().getHome(player.getUniqueId(), name);

        if (info == null) {
            plugin.getMessageManager().sendMessage(player, "<red>✖ No existe un hogar con ese nombre.</red>");
            return;
        }

        Location loc = info.location();
        // Use TeleportManager if available
        if (plugin.getConfig().getBoolean("modules.teleport", true)) {
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
        plugin.getProtectionManager().removeHome(player.getUniqueId(), name, player.getWorld().getName());
        plugin.getMessageManager().sendMessage(player, plugin.getConfig().getString("messages.home-deleted", "<green>✔ Hogar eliminado correctamente.</green>"));
    }

    private void handleTrust(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "<red>Uso: /home trust <hogar> <jugador></red>");
            return;
        }
        String homeName = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            plugin.getMessageManager().sendMessage(player, "<red>Jugador no encontrado.</red>");
            return;
        }
        if (plugin.getDatabaseManager().getHome(player.getUniqueId(), homeName) == null) {
            plugin.getMessageManager().sendMessage(player, "<red>Ese hogar no existe.</red>");
            return;
        }
        plugin.getDatabaseManager().addTrust(player.getUniqueId(), homeName, target.getUniqueId());
        plugin.getProtectionManager().updateTrust(player.getUniqueId(), homeName);
        plugin.getMessageManager().sendMessage(player, "<green>✔ Has dado permisos a " + target.getName() + " en " + homeName + ".</green>");
    }

    private void handleUntrust(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "<red>Uso: /home untrust <hogar> <jugador></red>");
            return;
        }
        String homeName = args[0].toLowerCase();
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        plugin.getDatabaseManager().removeTrust(player.getUniqueId(), homeName, target.getUniqueId());
        plugin.getProtectionManager().updateTrust(player.getUniqueId(), homeName);
        plugin.getMessageManager().sendMessage(player, "<yellow>✔ Has quitado permisos a " + target.getName() + " en " + homeName + ".</yellow>");
    }

    private void handleInfo(Player player, String[] args) {
        String name = args.length > 0 ? args[0].toLowerCase() : "home";
        net.wrpnetwork.survivalcore.database.DatabaseManager.HomeInfo info = plugin.getDatabaseManager().getHome(player.getUniqueId(), name);
        if (info == null) {
            plugin.getMessageManager().sendMessage(player, "<red>Ese hogar no existe.</red>");
            return;
        }
        int radius = info.level() * 10;
        plugin.getMessageManager().sendRawMessage(player, "<blue>🏠 Información de " + name + ":</blue>");
        plugin.getMessageManager().sendRawMessage(player, "<grey>Nivel:</grey> <green>" + info.level() + "</green>");
        plugin.getMessageManager().sendRawMessage(player, "<grey>Radio protección:</grey> <white>" + radius + " bloques</white>");
        List<UUID> trusted = plugin.getDatabaseManager().getTrusted(player.getUniqueId(), name);
        if (!trusted.isEmpty()) {
            StringBuilder sb = new StringBuilder("<grey>Amigos:</grey> <white>");
            for (UUID u : trusted) {
                String uName = Bukkit.getOfflinePlayer(u).getName();
                if (uName != null) sb.append(uName).append(" ");
            }
            plugin.getMessageManager().sendRawMessage(player, sb.toString().trim() + "</white>");
        }
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
