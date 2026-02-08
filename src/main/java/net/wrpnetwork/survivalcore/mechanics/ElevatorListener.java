package net.wrpnetwork.survivalcore.mechanics;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class ElevatorListener implements Listener {

    private final SurvivalCore plugin;
    private final Material elevatorMat = Material.DAYLIGHT_DETECTOR;

    public ElevatorListener(SurvivalCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJump(PlayerMoveEvent event) {
        if (event.getFrom().getY() < event.getTo().getY()) {
            Block b = event.getFrom().getBlock().getRelative(0, -1, 0);
            if (b.getType() == elevatorMat) {
                Location up = findElevator(b.getLocation(), true);
                if (up != null) {
                    event.getPlayer().teleport(up.add(0.5, 1, 0.5));
                    event.getPlayer().playSound(event.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_SHULKER_TELEPORT, 1f, 1f);
                }
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            Block b = event.getPlayer().getLocation().getBlock().getRelative(0, -1, 0);
            if (b.getType() == elevatorMat) {
                Location down = findElevator(b.getLocation(), false);
                if (down != null) {
                    event.getPlayer().teleport(down.add(0.5, 1, 0.5));
                    event.getPlayer().playSound(event.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_SHULKER_TELEPORT, 1f, 1f);
                }
            }
        }
    }

    private Location findElevator(Location start, boolean up) {
        int y = start.getBlockY();
        for (int i = 1; i < 64; i++) {
            int nextY = up ? y + i : y - i;
            if (nextY < 0 || nextY > 255) break;

            Block b = start.getWorld().getBlockAt(start.getBlockX(), nextY, start.getBlockZ());
            if (b.getType() == elevatorMat) {
                return b.getLocation();
            }
        }
        return null;
    }
}
