package net.wrpnetwork.hubstatus;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class PlaceholderProvider {

    private final WRPHubStatus plugin;
    private final boolean papiEnabled;

    public PlaceholderProvider(WRPHubStatus plugin) {
        this.plugin = plugin;
        this.papiEnabled = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
    }

    public String parse(OfflinePlayer player, String text) {
        if (text == null) return "";

        // Internal Placeholders
        text = text.replace("%players%", String.valueOf(Bukkit.getOnlinePlayers().size()));

        for (StatusManager.ServerData data : plugin.getStatusManager().getServerDataMap().values()) {
            String id = data.getId();
            text = text.replace("%status_" + id + "%", plugin.color(plugin.getStatusManager().getStateColor(data.getCurrentState()) + data.getCurrentState()));
            text = text.replace("%players_" + id + "%", String.valueOf(data.getOnlinePlayers()));
            text = text.replace("%max_" + id + "%", String.valueOf(data.getMaxPlayers()));
        }

        // Global placeholders (optional)
        int totalMax = 0;
        int totalOnline = 0;
        for (StatusManager.ServerData data : plugin.getStatusManager().getServerDataMap().values()) {
            totalMax += data.getMaxPlayers();
            totalOnline += data.getOnlinePlayers();
        }
        text = text.replace("%max_players%", String.valueOf(totalMax));
        text = text.replace("%network_players%", String.valueOf(totalOnline));

        // PAPI support
        if (papiEnabled && player != null) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }

        return plugin.color(text);
    }
}
