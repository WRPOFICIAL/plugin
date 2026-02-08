package net.wrpnetwork.survivalcore.clan;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.wrpnetwork.survivalcore.SurvivalCore;
import net.wrpnetwork.survivalcore.database.DatabaseManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class ClanListener implements Listener {

    private final SurvivalCore plugin;

    public ClanListener(SurvivalCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getClanManager().updateNametag(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getChatManager().isGlobalChatEnabled() && !event.getPlayer().hasPermission("survivalcore.admin")) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(event.getPlayer(), "<red>El chat global está desactivado actualmente.</red>");
            return;
        }

        DatabaseManager.ClanData clan = plugin.getDatabaseManager().getClanByMember(event.getPlayer().getUniqueId());

        if (clan != null) {
            // [TAG] Player: Message
            String tagPrefix = "<grey>[<aqua>" + clan.tag() + "</aqua>] </grey>";
            Component tagComp = plugin.getMessageManager().parse(tagPrefix);

            event.renderer((source, sourceDisplayName, message, viewer) ->
                tagComp.append(sourceDisplayName)
                       .append(Component.text(": "))
                       .append(message)
            );
        }
    }
}
