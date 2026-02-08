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

}
