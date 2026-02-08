package net.wrpnetwork.hubstatus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class WRPHubStatus extends JavaPlugin {

    private FileConfiguration messagesConfig;
    private File messagesFile;
    private FileConfiguration licenseConfig;

    private StatusManager statusManager;
    private ItemManager itemManager;
    private ScoreboardManager scoreboardManager;
    private PlaceholderProvider placeholderProvider;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        createMessagesConfig();
        loadLicenseConfig();

        if (getConfig().getBoolean("plugin.show-license")) {
            showLicense();
        }

        if (!getConfig().getBoolean("plugin.enabled")) {
            getLogger().info("Plugin disabled via config.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize managers
        statusManager = new StatusManager(this);
        placeholderProvider = new PlaceholderProvider(this);
        itemManager = new ItemManager(this, statusManager);
        scoreboardManager = new ScoreboardManager(this, statusManager);

        // Register commands
        getCommand("wrphub").setExecutor(new CommandHandler(this, statusManager));
        getCommand("wrphub").setTabCompleter(new CommandHandler(this, statusManager));

        // Register events
        getServer().getPluginManager().registerEvents(new HubListener(this, itemManager), this);

        // Update task for items
        startTasks();

        getLogger().info("WRP-HubStatus enabled successfully.");
    }

    private void startTasks() {
        // Status ping task
        long updateInterval = getConfig().getLong("server-settings.update-interval-seconds", 10);
        if (updateInterval < 1) updateInterval = 1;
        updateInterval *= 20L;

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            for (StatusManager.ServerData data : statusManager.getServerDataMap().values()) {
                if (data.isMaintenance()) continue;
                if (data.isAutoStatus()) {
                    ServerPinger.PingResult result = ServerPinger.ping(data.getAddress());
                    if (result.online) {
                        data.setCurrentState("ONLINE");
                        data.setOnlinePlayers(result.playersOnline);
                        data.setMaxPlayers(result.playersMax);
                    } else {
                        data.setCurrentState("OFFLINE");
                        data.setOnlinePlayers(0);
                    }
                }
            }
        }, 20L, updateInterval);

        // Item update task
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                itemManager.updatePlayerItems(player);
            }
        }, updateInterval, updateInterval);

        // Scoreboard update task
        long sbInterval = getConfig().getLong("scoreboard.update-interval-seconds", 5);
        if (sbInterval < 1) sbInterval = 1;
        sbInterval *= 20L;

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (getConfig().getBoolean("scoreboard.enabled", true)) {
                for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                    scoreboardManager.updateScoreboard(player);
                }
            }
        }, 20L, sbInterval);
    }

    public StatusManager getStatusManager() {
        return statusManager;
    }

    public ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public PlaceholderProvider getPlaceholderProvider() {
        return placeholderProvider;
    }

    @Override
    public void onDisable() {
        getLogger().info("WRP-HubStatus disabled.");
    }

    public void reloadSystems() {
        // Stop existing tasks
        Bukkit.getScheduler().cancelTasks(this);

        // Reload StatusManager data
        statusManager.loadServers();

        // Re-start tasks
        startTasks();

        // Update all players immediately
        for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
            itemManager.updatePlayerItems(player);

            if (getConfig().getBoolean("scoreboard.enabled", true)) {
                scoreboardManager.updateScoreboard(player);
            } else {
                scoreboardManager.removeScoreboard(player);
            }
        }
    }

    private void loadLicenseConfig() {
        File licenseFile = new File(getDataFolder(), "license.yml");
        if (!licenseFile.exists()) {
            saveResource("license.yml", false);
        }
        licenseConfig = YamlConfiguration.loadConfiguration(licenseFile);
    }

    private void showLicense() {
        String version = licenseConfig.getString("version", "1.0");
        String author = licenseConfig.getString("author", "EGG PRODUCCIONES OFICIAL");
        String copyright = licenseConfig.getString("copyright", "WYLRP© – Todos los derechos reservados");

        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "WRP-HubStatus v" + version);
        Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + author);
        Bukkit.getConsoleSender().sendMessage(ChatColor.GRAY + copyright);
    }

    public void createMessagesConfig() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public FileConfiguration getMessagesConfig() {
        if (messagesConfig == null) {
            createMessagesConfig();
        }
        return messagesConfig;
    }

    public void reloadMessagesConfig() {
        if (messagesFile == null) {
            messagesFile = new File(getDataFolder(), "messages.yml");
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defaultStream = getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            messagesConfig.setDefaults(defaultConfig);
        }
    }

    public String getMessage(String path) {
        String message = getMessagesConfig().getString(path);
        if (message == null) return "Missing message: " + path;
        String prefix = getConfig().getString("messages.prefix", "&8[&6WRP&8]");
        return ChatColor.translateAlternateColorCodes('&', message.replace("%prefix%", prefix));
    }

    public String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
