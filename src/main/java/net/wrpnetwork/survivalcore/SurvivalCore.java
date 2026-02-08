package net.wrpnetwork.survivalcore;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.wrpnetwork.survivalcore.database.DatabaseManager;
import net.wrpnetwork.survivalcore.util.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class SurvivalCore extends JavaPlugin {

    private static SurvivalCore instance;
    private BukkitAudiences adventure;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private net.wrpnetwork.survivalcore.teleport.TeleportManager teleportManager;
    private net.wrpnetwork.survivalcore.grave.GraveManager graveManager;
    private net.wrpnetwork.survivalcore.build.BuildCommands buildCommands;
    private net.wrpnetwork.survivalcore.economy.EconomyManager economyManager;
    private net.wrpnetwork.survivalcore.economy.ShopGUI shopGUI;
    private net.wrpnetwork.survivalcore.economy.NPCManager npcManager;
    private net.wrpnetwork.survivalcore.protection.ProtectionManager protectionManager;
    private net.wrpnetwork.survivalcore.clan.ClanManager clanManager;

    @Override
    public void onEnable() {
        instance = this;
        this.adventure = BukkitAudiences.create(this);

        saveDefaultConfig();

        this.messageManager = new MessageManager(this);
        this.databaseManager = new DatabaseManager(this);

        if (!databaseManager.initialize()) {
            getLogger().log(Level.SEVERE, "No se pudo inicializar la base de datos. Desactivando plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.protectionManager = new net.wrpnetwork.survivalcore.protection.ProtectionManager(this);
        this.economyManager = new net.wrpnetwork.survivalcore.economy.EconomyManager(this);

        SurvivalTabCompleter tabCompleter = new SurvivalTabCompleter(this);
        getCommand("survivalcore").setExecutor(new CommandHandler(this));
        getCommand("survivalcore").setTabCompleter(tabCompleter);

        if (getConfig().getBoolean("modules.teleport", true)) {
            this.teleportManager = new net.wrpnetwork.survivalcore.teleport.TeleportManager(this);
            net.wrpnetwork.survivalcore.teleport.TeleportCommands tpCommands = new net.wrpnetwork.survivalcore.teleport.TeleportCommands(this, teleportManager);
            getCommand("spawn").setExecutor(tpCommands);
            getCommand("tpa").setExecutor(tpCommands);
            getCommand("tpaccept").setExecutor(tpCommands);
            getCommand("tpdeny").setExecutor(tpCommands);
            getCommand("back").setExecutor(tpCommands);

            getCommand("tpa").setTabCompleter(tabCompleter);

            getServer().getPluginManager().registerEvents(new net.wrpnetwork.survivalcore.teleport.TeleportListener(this, teleportManager), this);
            getServer().getPluginManager().registerEvents(new net.wrpnetwork.survivalcore.teleport.DeathBackListener(this), this);
        }

        if (getConfig().getBoolean("modules.homes", true)) {
            net.wrpnetwork.survivalcore.home.HomeCommands homeCommands = new net.wrpnetwork.survivalcore.home.HomeCommands(this);
            getCommand("sethome").setExecutor(homeCommands);
            getCommand("home").setExecutor(homeCommands);
            getCommand("homes").setExecutor(homeCommands);
            getCommand("delhome").setExecutor(homeCommands);

            getCommand("home").setTabCompleter(tabCompleter);
            getCommand("delhome").setTabCompleter(tabCompleter);

            getServer().getPluginManager().registerEvents(new net.wrpnetwork.survivalcore.home.ProtectionListener(this), this);
            getServer().getPluginManager().registerEvents(new net.wrpnetwork.survivalcore.home.UpgradeListener(this), this);
        }

        if (getConfig().getBoolean("modules.graves", true)) {
            this.graveManager = new net.wrpnetwork.survivalcore.grave.GraveManager(this);
            getServer().getPluginManager().registerEvents(new net.wrpnetwork.survivalcore.grave.GraveListener(this, graveManager), this);
            getCommand("tumba").setExecutor(new net.wrpnetwork.survivalcore.grave.GraveCommand(this, graveManager));
        }

        if (getConfig().getBoolean("modules.build", true)) {
            this.buildCommands = new net.wrpnetwork.survivalcore.build.BuildCommands(this);
            getCommand("fly").setExecutor(buildCommands);
            getCommand("repair").setExecutor(buildCommands);
            getCommand("buildmode").setExecutor(buildCommands);

            getCommand("fly").setTabCompleter(tabCompleter);
            getCommand("repair").setTabCompleter(tabCompleter);
            getCommand("buildmode").setTabCompleter(tabCompleter);

            getServer().getPluginManager().registerEvents(new net.wrpnetwork.survivalcore.build.BuildListener(this, buildCommands), this);
        }

        // Economy
        this.shopGUI = new net.wrpnetwork.survivalcore.economy.ShopGUI(this);
        this.npcManager = new net.wrpnetwork.survivalcore.economy.NPCManager(this, shopGUI);
        net.wrpnetwork.survivalcore.economy.EconomyCommands ecoCommands = new net.wrpnetwork.survivalcore.economy.EconomyCommands(this);
        getCommand("money").setExecutor(ecoCommands);
        getCommand("shop").setExecutor(ecoCommands);
        getCommand("eco").setExecutor(ecoCommands);
        getCommand("spawnnpc").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player p && p.isOp()) {
                npcManager.spawnShopNPC(p.getLocation(), args.length > 0 ? String.join(" ", args) : "<gold>Tienda WRP</gold>");
                return true;
            }
            return false;
        });
        getServer().getPluginManager().registerEvents(shopGUI, this);
        getServer().getPluginManager().registerEvents(npcManager, this);

        // Clans
        this.clanManager = new net.wrpnetwork.survivalcore.clan.ClanManager(this);
        getCommand("clan").setExecutor(new net.wrpnetwork.survivalcore.clan.ClanCommands(this));
        getCommand("clan").setTabCompleter(tabCompleter);
        getServer().getPluginManager().registerEvents(new net.wrpnetwork.survivalcore.clan.ClanListener(this), this);

        sendStartupMessage();
    }

    @Override
    public void onDisable() {
        if (this.graveManager != null) {
            this.graveManager.cleanup();
        }
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
        if (this.databaseManager != null) {
            this.databaseManager.close();
        }
    }

    private void sendStartupMessage() {
        getLogger().info("════════════════════════════════");
        getLogger().info(" SurvivalCore+ v" + getDescription().getVersion());
        getLogger().info(" Desarrollado por WRP");
        getLogger().info(" Autores: Wyl & Ari");
        getLogger().info(" Licencia: Uso privado");
        getLogger().info(" Todos los derechos reservados");
        getLogger().info("════════════════════════════════");
    }

    public static SurvivalCore getInstance() {
        return instance;
    }

    public BukkitAudiences getAdventure() {
        return adventure;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public net.wrpnetwork.survivalcore.teleport.TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public net.wrpnetwork.survivalcore.grave.GraveManager getGraveManager() {
        return graveManager;
    }

    public net.wrpnetwork.survivalcore.build.BuildCommands getBuildCommands() {
        return buildCommands;
    }

    public net.wrpnetwork.survivalcore.economy.EconomyManager getEconomyManager() {
        return economyManager;
    }

    public net.wrpnetwork.survivalcore.economy.ShopGUI getShopGUI() {
        return shopGUI;
    }

    public net.wrpnetwork.survivalcore.protection.ProtectionManager getProtectionManager() {
        return protectionManager;
    }

    public net.wrpnetwork.survivalcore.clan.ClanManager getClanManager() {
        return clanManager;
    }
}
