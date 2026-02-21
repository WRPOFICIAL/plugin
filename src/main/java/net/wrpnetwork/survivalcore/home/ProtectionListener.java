package net.wrpnetwork.survivalcore.home;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProtectionListener implements Listener {

    private final SurvivalCore plugin;

    public ProtectionListener(SurvivalCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent event) {
        Location loc = event.getBlock().getLocation();
        if (isCoreBlock(loc)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendMessage(event.getPlayer(), "<red>✖ No puedes romper el Núcleo del Hogar. Usa /delhome si quieres quitarlo.</red>");
            return;
        }

        if (isProtected(event.getPlayer(), loc)) {
            event.setCancelled(true);
            plugin.getMessageManager().sendActionBar(event.getPlayer(), "<red>✖ Esta zona está protegida.</red>");
        }
    }

    private boolean isCoreBlock(Location loc) {
        // Check if this location is a home location
        List<net.wrpnetwork.survivalcore.database.DatabaseManager.HomeData> homes = plugin.getProtectionManager().getWorldHomes(loc.getWorld().getName());
        if (homes == null) return false;
        for (net.wrpnetwork.survivalcore.database.DatabaseManager.HomeData home : homes) {
            // Compare block coordinates (int) to ensure it works even if the sethome was slightly offset
            if ((int)home.x() == loc.getBlockX() && (int)home.y() == loc.getBlockY() && (int)home.z() == loc.getBlockZ()) {
                return true;
            }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent event) {
        if (isProtected(event.getPlayer(), event.getBlock().getLocation())) {
            event.setCancelled(true);
            plugin.getMessageManager().sendActionBar(event.getPlayer(), "<red>✖ Esta zona está protegida.</red>");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (isProtected(event.getPlayer(), event.getClickedBlock().getLocation())) {
            // Allow some interactions if needed, but generally block containers
            Material type = event.getClickedBlock().getType();
            if (type.name().contains("CHEST") || type.name().contains("SHULKER") || type.name().contains("BARREL") || type.name().contains("FURNACE")) {
                event.setCancelled(true);
                plugin.getMessageManager().sendActionBar(event.getPlayer(), "<red>✖ No puedes abrir esto aquí.</red>");
            }
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        // Prevent creeper/tnt damage in ANY protected home
        event.blockList().removeIf(block -> isAnyProtected(block.getLocation()));
    }

    private boolean isProtected(Player player, Location loc) {
        if (player.hasPermission("survivalcore.protection.bypass")) return false;
        return !plugin.getProtectionManager().canBuildAt(player.getUniqueId(), loc);
    }

    private boolean isAnyProtected(Location loc) {
        return plugin.getProtectionManager().isLocationProtected(loc);
    }
}
