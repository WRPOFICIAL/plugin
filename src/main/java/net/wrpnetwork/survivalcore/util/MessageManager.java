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
        plugin.getAdventure().sender(sender).sendMessage(parse(prefix + message));
    }

    public void sendRawMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        plugin.getAdventure().sender(sender).sendMessage(parse(message));
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
        if (message == null) return Component.empty();
        // Handle legacy & colors by converting them to MiniMessage-like tags for consistent parsing
        String mmMessage = message
                .replace("&0", "<black>").replace("&1", "<dark_blue>").replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>").replace("&4", "<dark_red>").replace("&5", "<dark_purple>")
                .replace("&6", "<gold>").replace("&7", "<gray>").replace("&8", "<dark_gray>")
                .replace("&9", "<blue>").replace("&a", "<green>").replace("&b", "<aqua>")
                .replace("&c", "<red>").replace("&d", "<light_purple>").replace("&e", "<yellow>")
                .replace("&f", "<white>").replace("&l", "<bold>").replace("&m", "<strikethrough>")
                .replace("&n", "<underlined>").replace("&o", "<italic>").replace("&r", "<reset>");

        // Also support standard & if not followed by a color code (optional)
        mmMessage = mmMessage.replace("&", "§"); // Fallback for other uses

        try {
            return miniMessage.deserialize(mmMessage);
        } catch (Exception e) {
            // Fallback to legacy if MiniMessage fails
            return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(mmMessage);
        }
    }
}
