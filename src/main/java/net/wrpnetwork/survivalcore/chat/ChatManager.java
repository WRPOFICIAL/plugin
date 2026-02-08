package net.wrpnetwork.survivalcore.chat;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatManager {

    private final SurvivalCore plugin;
    private final Map<UUID, UUID> lastMessaged = new HashMap<>();
    private boolean globalChatEnabled = true;

    public ChatManager(SurvivalCore plugin) {
        this.plugin = plugin;
    }

    public void setLastMessaged(UUID player, UUID target) {
        lastMessaged.put(player, target);
        lastMessaged.put(target, player);
    }

    public UUID getLastMessaged(UUID player) {
        return lastMessaged.get(player);
    }

    public boolean isGlobalChatEnabled() {
        return globalChatEnabled;
    }

    public void toggleGlobalChat() {
        globalChatEnabled = !globalChatEnabled;
    }
}
