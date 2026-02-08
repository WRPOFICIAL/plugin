package net.wrpnetwork.survivalcore.clan;

import net.wrpnetwork.survivalcore.SurvivalCore;
import net.wrpnetwork.survivalcore.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ClanCommands implements CommandExecutor {

    private final SurvivalCore plugin;

    public ClanCommands(SurvivalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create":
                if (!player.hasPermission("survivalcore.clan.create")) {
                    plugin.getMessageManager().sendMessage(player, plugin.getConfig().getString("messages.no-permission"));
                    return true;
                }
                if (args.length < 3) {
                    plugin.getMessageManager().sendMessage(player, "<red>Uso: /clan create <nombre> <lema/tag></red>");
                    plugin.getMessageManager().sendMessage(player, "<grey>Ejemplo: /clan create Zombies Z</grey>");
                    return true;
                }
                plugin.getClanManager().createClan(player, args[1], args[2]);
                break;
            case "invite":
                if (args.length < 2) {
                    plugin.getMessageManager().sendMessage(player, "<red>Uso: /clan invite <jugador></red>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    plugin.getMessageManager().sendMessage(player, "<red>Jugador no encontrado.</red>");
                    return true;
                }
                plugin.getClanManager().sendInvite(player, target);
                break;
            case "accept":
                plugin.getClanManager().acceptInvite(player);
                break;
            case "delete":
                handleDelete(player);
                break;
            case "sethome":
                handleSetHome(player);
                break;
            case "home":
                handleHome(player);
                break;
            case "bank":
                handleBank(player, args);
                break;
            case "kick":
                handleKick(player, args);
                break;
            case "leave":
                handleLeave(player);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void sendHelp(Player player) {
        plugin.getMessageManager().sendRawMessage(player, "<dark_aqua>═════ [ <aqua>Clan Comandos</aqua> ] ═════</dark_aqua>");
        plugin.getMessageManager().sendRawMessage(player, "<grey>-</grey> <green>/clan create <nombre> <tag></green>");
        plugin.getMessageManager().sendRawMessage(player, "<grey>-</grey> <green>/clan invite <jugador></green>");
        plugin.getMessageManager().sendRawMessage(player, "<grey>-</grey> <green>/clan accept</green>");
        plugin.getMessageManager().sendRawMessage(player, "<grey>-</grey> <green>/clan sethome</green>");
        plugin.getMessageManager().sendRawMessage(player, "<grey>-</grey> <green>/clan home</green>");
        plugin.getMessageManager().sendRawMessage(player, "<grey>-</grey> <green>/clan bank <deposit/withdraw> <monto></green>");
        plugin.getMessageManager().sendRawMessage(player, "<grey>-</grey> <green>/clan kick <jugador></green>");
        plugin.getMessageManager().sendRawMessage(player, "<grey>-</grey> <green>/clan leave</green>");
        plugin.getMessageManager().sendRawMessage(player, "<dark_aqua>════════════════════════════════</dark_aqua>");
    }

    private void handleSetHome(Player player) {
        DatabaseManager.ClanData clan = plugin.getDatabaseManager().getClanByMember(player.getUniqueId());
        if (clan == null) {
            plugin.getMessageManager().sendMessage(player, "<red>✖ No tienes clan.</red>");
            return;
        }
        // Only Leader or Moderator? Let's check leader for now
        if (!clan.leader().equals(player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "<red>✖ Solo el líder puede poner el hogar del clan.</red>");
            return;
        }
        plugin.getDatabaseManager().updateClan(clan.id(), clan.balance(), clan.level(), player.getLocation());
        plugin.getMessageManager().sendMessage(player, "<green>✔ Hogar del clan establecido.</green>");
    }

    private void handleHome(Player player) {
        DatabaseManager.ClanData clan = plugin.getDatabaseManager().getClanByMember(player.getUniqueId());
        if (clan == null || clan.home() == null) {
            plugin.getMessageManager().sendMessage(player, "<red>✖ El clan no tiene hogar establecido.</red>");
            return;
        }
        plugin.getTeleportManager().teleport(player, clan.home());
    }

    private void handleBank(Player player, String[] args) {
        if (args.length < 3) {
            plugin.getMessageManager().sendMessage(player, "<red>Uso: /clan bank <deposit/withdraw> <monto></red>");
            return;
        }
        DatabaseManager.ClanData clan = plugin.getDatabaseManager().getClanByMember(player.getUniqueId());
        if (clan == null) return;

        double amount = Double.parseDouble(args[2]);
        if (args[1].equalsIgnoreCase("deposit")) {
            if (plugin.getEconomyManager().withdraw(player.getUniqueId(), amount)) {
                plugin.getDatabaseManager().updateClan(clan.id(), clan.balance() + amount, clan.level(), clan.home());
                plugin.getMessageManager().sendMessage(player, "<green>✔ Has depositado $" + amount + " al banco del clan.</green>");
            } else {
                plugin.getMessageManager().sendMessage(player, "<red>✖ No tienes suficiente dinero.</red>");
            }
        } else if (args[1].equalsIgnoreCase("withdraw")) {
            if (!clan.leader().equals(player.getUniqueId())) {
                plugin.getMessageManager().sendMessage(player, "<red>✖ Solo el líder puede retirar dinero.</red>");
                return;
            }
            if (clan.balance() >= amount) {
                plugin.getDatabaseManager().updateClan(clan.id(), clan.balance() - amount, clan.level(), clan.home());
                plugin.getEconomyManager().addBalance(player.getUniqueId(), amount);
                plugin.getMessageManager().sendMessage(player, "<green>✔ Has retirado $" + amount + " del banco del clan.</green>");
            } else {
                plugin.getMessageManager().sendMessage(player, "<red>✖ El clan no tiene suficiente dinero.</red>");
            }
        }
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) return;
        DatabaseManager.ClanData clan = plugin.getDatabaseManager().getClanByMember(player.getUniqueId());
        if (clan == null || !clan.leader().equals(player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "<red>✖ No tienes permiso.</red>");
            return;
        }
        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        plugin.getDatabaseManager().removeClanMember(target.getUniqueId());
        plugin.getMessageManager().sendMessage(player, "<green>✔ Has expulsado a " + target.getName() + ".</green>");
        if (target.isOnline()) {
            plugin.getClanManager().updateNametag(target.getPlayer());
            plugin.getMessageManager().sendMessage(target.getPlayer(), "<red>Has sido expulsado del clan.</red>");
        }
    }

    private void handleLeave(Player player) {
        DatabaseManager.ClanData clan = plugin.getDatabaseManager().getClanByMember(player.getUniqueId());
        if (clan == null) return;
        if (clan.leader().equals(player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "<red>✖ Como líder no puedes salirte. Usa /clan delete para disolverlo.</red>");
            return;
        }
        plugin.getDatabaseManager().removeClanMember(player.getUniqueId());
        plugin.getMessageManager().sendMessage(player, "<yellow>Has salido del clan.</yellow>");
        plugin.getClanManager().updateNametag(player);
    }

    private void handleDelete(Player player) {
        DatabaseManager.ClanData clan = plugin.getDatabaseManager().getClanByMember(player.getUniqueId());
        if (clan == null || !clan.leader().equals(player.getUniqueId())) {
            plugin.getMessageManager().sendMessage(player, "<red>✖ Solo el líder puede disolver el clan.</red>");
            return;
        }

        // Update all members nametags
        List<DatabaseManager.ClanMemberData> members = plugin.getDatabaseManager().getClanMembers(clan.id());
        plugin.getDatabaseManager().deleteClan(clan.id());

        plugin.getMessageManager().sendMessage(player, "<red>El clan ha sido disuelto.</red>");
        for (DatabaseManager.ClanMemberData m : members) {
            Player p = Bukkit.getPlayer(m.uuid());
            if (p != null) {
                plugin.getClanManager().updateNametag(p);
                if (!p.equals(player)) {
                    plugin.getMessageManager().sendMessage(p, "<red>Tu clan ha sido disuelto por el líder.</red>");
                }
            }
        }
    }
}
