package net.wrpnetwork.survivalcore.clan;

import net.wrpnetwork.survivalcore.SurvivalCore;
import net.wrpnetwork.survivalcore.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ClanManager {

    private final SurvivalCore plugin;
    private final Map<UUID, Integer> invites = new HashMap<>(); // target -> clanId

    public ClanManager(SurvivalCore plugin) {
        this.plugin = plugin;
    }

    public void createClan(Player leader, String name, String tag) {
        if (plugin.getDatabaseManager().getClanByMember(leader.getUniqueId()) != null) {
            plugin.getMessageManager().sendMessage(leader, "<red>✖ Ya perteneces a un clan.</red>");
            return;
        }

        if (plugin.getDatabaseManager().getClan(name) != null) {
            plugin.getMessageManager().sendMessage(leader, "<red>✖ Ya existe un clan con ese nombre.</red>");
            return;
        }

        int id = plugin.getDatabaseManager().createClan(name, tag.toUpperCase(), leader.getUniqueId());
        if (id != -1) {
            plugin.getDatabaseManager().addClanMember(leader.getUniqueId(), id, "LEADER");
            plugin.getMessageManager().sendMessage(leader, "<green>✔ Clan <gold>" + name + "</gold> [<aqua>" + tag.toUpperCase() + "</aqua>] creado con éxito.</green>");
            updateNametag(leader);
        } else {
            plugin.getMessageManager().sendMessage(leader, "<red>✖ Error al crear el clan.</red>");
        }
    }

    public void sendInvite(Player sender, Player target) {
        DatabaseManager.ClanData clan = plugin.getDatabaseManager().getClanByMember(sender.getUniqueId());
        if (clan == null) {
            plugin.getMessageManager().sendMessage(sender, "<red>✖ No perteneces a un clan.</red>");
            return;
        }

        // Check rank (MODERATOR or LEADER)
        // For simplicity, let's assume getClanByMember also gives rank or check member table
        // I'll add a check in the command

        invites.put(target.getUniqueId(), clan.id());
        plugin.getMessageManager().sendMessage(sender, "<green>✔ Invitación enviada a " + target.getName() + ".</green>");
        plugin.getMessageManager().sendMessage(target, "<aqua>" + sender.getName() + "</aqua> <yellow>te ha invitado al clan</yellow> <gold>" + clan.name() + "</gold>.");
        plugin.getMessageManager().sendMessage(target, "<yellow>Escribe <green>/clan accept</green> para unirte.</yellow>");
    }

    public void acceptInvite(Player player) {
        Integer clanId = invites.remove(player.getUniqueId());
        if (clanId == null) {
            plugin.getMessageManager().sendMessage(player, "<red>✖ No tienes invitaciones pendientes.</red>");
            return;
        }

        if (plugin.getDatabaseManager().getClanByMember(player.getUniqueId()) != null) {
            plugin.getMessageManager().sendMessage(player, "<red>✖ Ya perteneces a un clan.</red>");
            return;
        }

        plugin.getDatabaseManager().addClanMember(player.getUniqueId(), clanId, "MEMBER");
        plugin.getMessageManager().sendMessage(player, "<green>✔ Te has unido al clan.</green>");
        updateNametag(player);
    }

    public void updateNametag(Player player) {
        DatabaseManager.ClanData clan = plugin.getDatabaseManager().getClanByMember(player.getUniqueId());
        Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();

        // Remove from old team
        Team oldTeam = sb.getEntryTeam(player.getName());
        if (oldTeam != null) oldTeam.removeEntry(player.getName());

        if (clan != null) {
            String teamName = "clan_" + clan.id();
            Team team = sb.getTeam(teamName);
            if (team == null) {
                team = sb.registerNewTeam(teamName);
            }
            team.prefix(plugin.getMessageManager().parse("<grey>[<aqua>" + clan.tag() + "</aqua>] </grey>"));
            team.addEntry(player.getName());
        }
    }
}
