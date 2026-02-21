package net.wrpnetwork.survivalcore.chat;

import net.wrpnetwork.survivalcore.SurvivalCore;
import net.wrpnetwork.survivalcore.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ChatCommands implements CommandExecutor {

    private final SurvivalCore plugin;

    public ChatCommands(SurvivalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) return true;

        switch (command.getName().toLowerCase()) {
            case "msg":
                handleMsg(player, args);
                break;
            case "reply":
                handleReply(player, args);
                break;
            case "clanchat":
                handleClanChat(player, args);
                break;
            case "togglechat":
                if (!player.hasPermission("survivalcore.admin")) return true;
                plugin.getChatManager().toggleGlobalChat();
                plugin.getMessageManager().sendMessage(player, "<yellow>Chat global:</yellow> " + (plugin.getChatManager().isGlobalChatEnabled() ? "<green>Activado</green>" : "<red>Desactivado</red>"));
                break;
        }

        return true;
    }

    private void handleMsg(Player player, String[] args) {
        if (args.length < 2) {
            plugin.getMessageManager().sendMessage(player, "<red>Uso: /msg <jugador> <mensaje></red>");
            return;
        }
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageManager().sendMessage(player, "<red>Jugador no encontrado.</red>");
            return;
        }
        String msg = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        sendPrivateMessage(player, target, msg);
    }

    private void handleReply(Player player, String[] args) {
        if (args.length < 1) {
            plugin.getMessageManager().sendMessage(player, "<red>Uso: /r <mensaje></red>");
            return;
        }
        UUID last = plugin.getChatManager().getLastMessaged(player.getUniqueId());
        if (last == null) {
            plugin.getMessageManager().sendMessage(player, "<red>No tienes a nadie a quien responder.</red>");
            return;
        }
        Player target = Bukkit.getPlayer(last);
        if (target == null) {
            plugin.getMessageManager().sendMessage(player, "<red>El jugador ya no está conectado.</red>");
            return;
        }
        String msg = String.join(" ", args);
        sendPrivateMessage(player, target, msg);
    }

    private void sendPrivateMessage(Player sender, Player target, String msg) {
        String formatTo = "<grey>[<aqua>Tú</aqua> -> <aqua>" + target.getName() + "</aqua>] </grey>" + msg;
        String formatFrom = "<grey>[<aqua>" + sender.getName() + "</aqua> -> <aqua>Tú</aqua>] </grey>" + msg;

        plugin.getMessageManager().sendRawMessage(sender, formatTo);
        plugin.getMessageManager().sendRawMessage(target, formatFrom);
        plugin.getChatManager().setLastMessaged(sender.getUniqueId(), target.getUniqueId());
    }

    private void handleClanChat(Player player, String[] args) {
        DatabaseManager.ClanData clan = plugin.getDatabaseManager().getClanByMember(player.getUniqueId());
        if (clan == null) {
            plugin.getMessageManager().sendMessage(player, "<red>✖ No perteneces a un clan.</red>");
            return;
        }
        if (args.length < 1) {
            plugin.getMessageManager().sendMessage(player, "<red>Uso: /c <mensaje></red>");
            return;
        }
        String msg = String.join(" ", args);
        String format = "<dark_grey>[<aqua>Clan Chat</aqua>] <white>" + player.getName() + ": </white><grey>" + msg + "</grey>";

        for (DatabaseManager.ClanMemberData member : plugin.getDatabaseManager().getClanMembers(clan.id())) {
            Player p = Bukkit.getPlayer(member.uuid());
            if (p != null) {
                plugin.getMessageManager().sendRawMessage(p, format);
            }
        }
    }
}
