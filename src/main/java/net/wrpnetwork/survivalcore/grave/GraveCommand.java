package net.wrpnetwork.survivalcore.grave;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class GraveCommand implements CommandExecutor {

    private final SurvivalCore plugin;
    private final GraveManager graveManager;

    public GraveCommand(SurvivalCore plugin, GraveManager graveManager) {
        this.plugin = plugin;
        this.graveManager = graveManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }

        GraveManager.Grave playerGrave = null;
        for (GraveManager.Grave g : graveManager.getActiveGraves().values()) {
            if (g.owner().equals(player.getUniqueId())) {
                playerGrave = g;
                break;
            }
        }

        if (playerGrave == null) {
            plugin.getMessageManager().sendMessage(player, "<yellow>No tienes ninguna tumba activa.</yellow>");
            return true;
        }

        Location loc = playerGrave.location();
        long elapsed = System.currentTimeMillis() - playerGrave.createdAt();
        long total = plugin.getConfig().getInt("graves.despawn-minutes", 15) * 60 * 1000L;
        long remaining = (total - elapsed) / 1000 / 60;

        plugin.getMessageManager().sendRawMessage(player, "<dark_red>☠ Información de tu tumba:</dark_red>");
        plugin.getMessageManager().sendRawMessage(player, "<grey>Ubicación:</grey> <white>" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + " (" + loc.getWorld().getName() + ")</white>");
        plugin.getMessageManager().sendRawMessage(player, "<grey>Tiempo restante:</grey> <white>" + remaining + " minutos</white>");
        plugin.getMessageManager().sendRawMessage(player, "<yellow>Usa tu brújula para encontrarla.</yellow>");

        return true;
    }
}
