package net.wrpnetwork.hubstatus;

import org.bukkit.ChatColor;

public class StatusManager {

    private final WRPHubStatus plugin;
    private String currentState;

    public StatusManager(WRPHubStatus plugin) {
        this.plugin = plugin;
        this.currentState = plugin.getConfig().getString("server.default-state", "OFFLINE").toUpperCase();
    }

    public String getCurrentState() {
        return currentState;
    }

    public void setCurrentState(String state) {
        this.currentState = state.toUpperCase();
    }

    public String getStateColor() {
        switch (currentState) {
            case "ONLINE":
                return "&a";
            case "MANTENIMIENTO":
                return "&e";
            case "OFFLINE":
            default:
                return "&c";
        }
    }

    public String getFormattedState() {
        return plugin.color(getStateColor() + currentState);
    }
}
