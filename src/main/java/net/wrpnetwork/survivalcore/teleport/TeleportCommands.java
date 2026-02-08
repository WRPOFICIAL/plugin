package net.wrpnetwork.survivalcore.teleport;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TeleportCommands implements CommandExecutor {

    private final SurvivalCore plugin;
    private final TeleportManager tm;

    public TeleportCommands(SurvivalCore plugin, TeleportManager tm) {
        this.plugin = plugin;
        this.tm = tm;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "spawn":
                tm.teleport(player, player.getWorld().getSpawnLocation());
                break;
            case "tpa":
                if (args.length == 0) {
                    plugin.getMessageManager().sendMessage(player, "<red>Uso: /tpa <jugador></red>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    plugin.getMessageManager().sendMessage(player, "<red>Jugador no encontrado.</red>");
                    return true;
                }
                tm.addTpaRequest(player, target);
                break;
            case "tpaccept":
                tm.acceptTpa(player);
                break;
            case "tpdeny":
                tm.denyTpa(player);
                break;
            case "back":
                Location backLoc = plugin.getDatabaseManager().getBackLocation(player.getUniqueId());
                if (backLoc == null) {
                    plugin.getMessageManager().sendMessage(player, "<red>No tienes una ubicación anterior registrada.</red>");
                    return true;
                }
                tm.teleport(player, backLoc);
                break;
        }

        return true;
    }
}
