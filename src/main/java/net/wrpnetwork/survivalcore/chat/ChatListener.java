package net.wrpnetwork.survivalcore.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.wrpnetwork.survivalcore.SurvivalCore;
import net.wrpnetwork.survivalcore.database.DatabaseManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final SurvivalCore plugin;

    public ChatListener(SurvivalCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (!plugin.getChatManager().isGlobalChatEnabled() && !event.getPlayer().hasPermission("survivalcore.chat.bypass")) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(event.getPlayer(), "<red>El chat global está desactivado.</red>");
            return;
        }

        DatabaseManager.ClanData clan = plugin.getDatabaseManager().getClanByMember(event.getPlayer().getUniqueId());
        String format = plugin.getConfig().getString("clans.chat-format", "<grey>[%tag%] </grey>%player%: %message%");

        String tag = (clan != null) ? clan.tag() : "";
        String playerName = event.getPlayer().getName();
        String messageStr = LegacyComponentSerializer.legacyAmpersand().serialize(event.originalMessage());

        String formatted;
        if (clan != null) {
            formatted = format
                    .replace("%tag%", tag)
                    .replace("%player%", playerName)
                    .replace("%message%", messageStr);
        } else {
            // Default clean format: Player: Message
            formatted = "<grey>" + playerName + ": </grey>" + messageStr;
        }

        event.renderer((source, sourceDisplayName, message, viewer) ->
            plugin.getMessageManager().parse(formatted)
        );
    }
}
