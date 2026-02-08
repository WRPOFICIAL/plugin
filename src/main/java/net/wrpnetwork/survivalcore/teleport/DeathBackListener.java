package net.wrpnetwork.survivalcore.teleport;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathBackListener implements Listener {

    private final SurvivalCore plugin;

    public DeathBackListener(SurvivalCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        plugin.getDatabaseManager().saveBackLocation(event.getEntity().getUniqueId(), event.getEntity().getLocation());
    }
}
