package net.wrpnetwork.hubstatus;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class StatusManager {

    private final WRPHubStatus plugin;
    private final Map<String, ServerData> serverDataMap = new HashMap<>();

    public StatusManager(WRPHubStatus plugin) {
        this.plugin = plugin;
        loadServers();
    }

    public void loadServers() {
        serverDataMap.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("servers");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection serverSection = section.getConfigurationSection(key);
            if (serverSection == null) continue;

            ServerData data = new ServerData(
                    key,
                    serverSection.getString("display-name", key),
                    serverSection.getString("address", "127.0.0.1:25565"),
                    serverSection.getInt("max-players", 100),
                    serverSection.getBoolean("auto-status", true),
                    serverSection.getString("default-state", "OFFLINE")
            );
            serverDataMap.put(key, data);
        }
    }

    public ServerData getServerData(String serverId) {
        return serverDataMap.get(serverId);
    }

    public Map<String, ServerData> getServerDataMap() {
        return serverDataMap;
    }

    public String getStateColor(String state) {
        if (state == null) return "&c";
        switch (state.toUpperCase()) {
            case "ONLINE":
                return "&a";
            case "MANTENIMIENTO":
                return "&e";
            case "OFFLINE":
            default:
                return "&c";
        }
    }

    public static class ServerData {
        private final String id;
        private final String displayName;
        private final String address;
        private int maxPlayers;
        private final boolean autoStatus;
        private String currentState;
        private int onlinePlayers = 0;
        private boolean maintenance = false;

        public ServerData(String id, String displayName, String address, int maxPlayers, boolean autoStatus, String defaultState) {
            this.id = id;
            this.displayName = displayName;
            this.address = address;
            this.maxPlayers = maxPlayers;
            this.autoStatus = autoStatus;
            this.currentState = defaultState.toUpperCase();
            if (this.currentState.equals("MANTENIMIENTO")) {
                this.maintenance = true;
            }
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getAddress() { return address; }
        public int getMaxPlayers() { return maxPlayers; }
        public boolean isAutoStatus() { return autoStatus; }
        public String getCurrentState() { return currentState; }
        public void setCurrentState(String currentState) {
            this.currentState = currentState.toUpperCase();
            this.maintenance = this.currentState.equals("MANTENIMIENTO");
        }
        public int getOnlinePlayers() { return onlinePlayers; }
        public void setOnlinePlayers(int onlinePlayers) { this.onlinePlayers = onlinePlayers; }
        public void setMaxPlayers(int maxPlayers) { this.maxPlayers = maxPlayers; }
        public boolean isMaintenance() { return maintenance; }
        public void setMaintenance(boolean maintenance) {
            this.maintenance = maintenance;
            if (maintenance) this.currentState = "MANTENIMIENTO";
        }
    }
}
