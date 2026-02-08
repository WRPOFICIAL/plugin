package net.wrpnetwork.survivalcore.teleport;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class TeleportListener implements Listener {

    private final SurvivalCore plugin;
    private final TeleportManager tm;

    public TeleportListener(SurvivalCore plugin, TeleportManager tm) {
        this.plugin = plugin;
        this.tm = tm;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfig().getBoolean("teleport.cancel-on-move", true)) return;

        Player player = event.getPlayer();
        if (!tm.isPending(player)) return;

        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            tm.cancelPending(player);
            plugin.getMessageManager().sendMessage(player, plugin.getConfig().getString("messages.teleport-cancelled-move", "<red>✖ Teletransporte cancelado por movimiento.</red>"));
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!plugin.getConfig().getBoolean("teleport.cancel-on-damage", true)) return;
        if (!(event.getEntity() instanceof Player player)) return;

        if (tm.isPending(player)) {
            tm.cancelPending(player);
            plugin.getMessageManager().sendMessage(player, plugin.getConfig().getString("messages.teleport-cancelled-damage", "<red>✖ Teletransporte cancelado por daño.</red>"));
        }
    }
}
