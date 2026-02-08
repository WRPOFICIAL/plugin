package net.wrpnetwork.survivalcore;

import net.wrpnetwork.survivalcore.util.MessageManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandHandler implements CommandExecutor {

    private final SurvivalCore plugin;
    private final MessageManager mm;

    public CommandHandler(SurvivalCore plugin) {
        this.plugin = plugin;
        this.mm = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelp(sender);
                break;
            case "reload":
                if (!sender.hasPermission("survivalcore.admin")) {
                    mm.sendMessage(sender, plugin.getConfig().getString("messages.no-permission"));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getProtectionManager().loadAll();
                mm.sendMessage(sender, "<green>✔ Configuración y cachés recargadas correctamente.</green>");
                break;
            case "giveupgrade":
                if (!sender.hasPermission("survivalcore.admin")) {
                    mm.sendMessage(sender, plugin.getConfig().getString("messages.no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    mm.sendMessage(sender, "<red>Uso: /sc giveupgrade <jugador></red>");
                    return true;
                }
                org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayer(args[1]);
                if (target == null) {
                    mm.sendMessage(sender, "<red>Jugador no encontrado.</red>");
                    return true;
                }
                target.getInventory().addItem(getUpgradeItem());
                mm.sendMessage(sender, "<green>✔ Ítem de mejora entregado a " + target.getName() + ".</green>");
                break;
            case "status":
                sendStatus(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        mm.sendRawMessage(sender, "<blue>📘 Tus comandos:</blue>");
        if (sender.hasPermission("survivalcore.home")) mm.sendRawMessage(sender, "<grey>-</grey> <green>/home</green>");
        if (sender.hasPermission("survivalcore.sethome")) mm.sendRawMessage(sender, "<grey>-</grey> <green>/sethome</green>");
        if (sender.hasPermission("survivalcore.tpa")) mm.sendRawMessage(sender, "<grey>-</grey> <green>/tpa</green>");
        if (sender.hasPermission("survivalcore.spawn")) mm.sendRawMessage(sender, "<grey>-</grey> <green>/spawn</green>");
        if (sender.hasPermission("survivalcore.back")) mm.sendRawMessage(sender, "<grey>-</grey> <green>/back</green>");
        if (sender.hasPermission("survivalcore.fly")) mm.sendRawMessage(sender, "<grey>-</grey> <green>/fly</green>");
        if (sender.hasPermission("survivalcore.status")) mm.sendRawMessage(sender, "<grey>-</grey> <green>/sc status</green>");
    }

    private void sendStatus(CommandSender sender) {
        mm.sendRawMessage(sender, "<dark_aqua>═════ [ <aqua>SurvivalCore+ Status</aqua> ] ═════</dark_aqua>");
        mm.sendRawMessage(sender, "<grey>Versión:</grey> <white>" + plugin.getDescription().getVersion() + "</white>");
        mm.sendRawMessage(sender, "<grey>Módulos:</grey> " + getEnabledModules());

        if (sender instanceof Player player) {
            int homes = plugin.getDatabaseManager().getHomes(player.getUniqueId()).size();
            String rank = getPlayerRank(player);
            mm.sendRawMessage(sender, "<grey>Rango detectado:</grey> <white>" + rank + "</white>");
            mm.sendRawMessage(sender, "<grey>Hogares usados:</grey> <white>" + homes + "</white>");
        }
        mm.sendRawMessage(sender, "<dark_aqua>════════════════════════════════</dark_aqua>");
    }

    private String getEnabledModules() {
        StringBuilder sb = new StringBuilder();
        if (plugin.getConfig().getBoolean("modules.teleport", true)) sb.append("<green>TP</green> ");
        if (plugin.getConfig().getBoolean("modules.homes", true)) sb.append("<green>Homes</green> ");
        if (plugin.getConfig().getBoolean("modules.graves", true)) sb.append("<green>Graves</green> ");
        if (plugin.getConfig().getBoolean("modules.build", true)) sb.append("<green>Build</green> ");
        return sb.toString().trim();
    }

    private org.bukkit.inventory.ItemStack getUpgradeItem() {
        org.bukkit.Material mat = org.bukkit.Material.valueOf(plugin.getConfig().getString("homes.upgrade-item.material", "NETHER_STAR"));
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.getMessageManager().parse(plugin.getConfig().getString("homes.upgrade-item.name", "<gold>✨ Núcleo de Mejora ✨</gold>")));
        java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
        for (String line : plugin.getConfig().getStringList("homes.upgrade-item.lore")) {
            lore.add(plugin.getMessageManager().parse(line));
        }
        meta.lore(lore);
        meta.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "home_upgrade"), org.bukkit.persistence.PersistentDataType.INTEGER, 1);
        item.setItemMeta(meta);
        return item;
    }

    private String getPlayerRank(Player player) {
        if (player.isOp()) return "<red>Admin</red>";
        if (player.hasPermission("survivalcore.rank.staff")) return "<aqua>Staff</aqua>";
        if (player.hasPermission("survivalcore.rank.vip")) return "<gold>VIP</gold>";
        return "<white>Usuario</white>";
    }
}
