package net.wrpnetwork.survivalcore.build;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class BuildListener implements Listener {

    private final SurvivalCore plugin;
    private final BuildCommands buildCommands;

    public BuildListener(SurvivalCore plugin, BuildCommands buildCommands) {
        this.plugin = plugin;
        this.buildCommands = buildCommands;
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // GodMode for BuildMode
        if (buildCommands.getBuildModePlayers().contains(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            // Protection if they had fly enabled recently or have the permission
            if (player.getAllowFlight() || player.hasPermission("survivalcore.fly.safefall")) {
                event.setCancelled(true);
            }
        }
    }
}
