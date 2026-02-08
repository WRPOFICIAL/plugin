package net.wrpnetwork.survivalcore.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageManager {

    private final SurvivalCore plugin;
    private final MiniMessage miniMessage;
    private final String prefix;

    public MessageManager(SurvivalCore plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.prefix = plugin.getConfig().getString("messages.prefix", "<grey>[<green>Survival</green>]</grey> ");
    }

    public void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        plugin.getAdventure().sender(sender).sendMessage(miniMessage.deserialize(prefix + message));
    }

    public void sendRawMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        plugin.getAdventure().sender(sender).sendMessage(miniMessage.deserialize(message));
    }

    public void sendActionBar(Player player, String message) {
        if (message == null || message.isEmpty()) return;
        plugin.getAdventure().player(player).sendActionBar(miniMessage.deserialize(message));
    }

    public void sendTitle(Player player, String title, String subtitle) {
        Component t = miniMessage.deserialize(title);
        Component s = miniMessage.deserialize(subtitle);
        plugin.getAdventure().player(player).showTitle(Title.title(t, s));
    }

    public Component parse(String message) {
        return miniMessage.deserialize(message);
    }
}
