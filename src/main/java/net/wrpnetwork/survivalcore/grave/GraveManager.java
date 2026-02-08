package net.wrpnetwork.survivalcore.grave;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class GraveManager {

    private final SurvivalCore plugin;
    private final Map<Location, Grave> activeGraves = new HashMap<>();
    private final NamespacedKey graveKey;

    public GraveManager(SurvivalCore plugin) {
        this.plugin = plugin;
        this.graveKey = new NamespacedKey(plugin, "grave_owner");
        loadGraves();
    }

    private void loadGraves() {
        List<net.wrpnetwork.survivalcore.database.DatabaseManager.GraveData> data = plugin.getDatabaseManager().loadAllGraves();
        for (net.wrpnetwork.survivalcore.database.DatabaseManager.GraveData d : data) {
            org.bukkit.World world = Bukkit.getWorld(d.world());
            if (world == null) continue;
            Location loc = new Location(world, d.x(), d.y(), d.z());
            ItemStack[] items = net.wrpnetwork.survivalcore.util.InventoryUtils.itemStackArrayFromBase64(d.itemsBase64());

            // Recreate hologram
            ArmorStand hologram = (ArmorStand) loc.getWorld().spawnEntity(loc.clone().add(0.5, -0.5, 0.5), EntityType.ARMOR_STAND);
            hologram.setVisible(false);
            hologram.setGravity(false);
            hologram.setCustomNameVisible(true);
            String ownerName = Bukkit.getOfflinePlayer(d.owner()).getName();
            if (ownerName == null) ownerName = "Jugador";
            String holoText = plugin.getConfig().getString("messages.grave-hologram", "<red>☠ Tumba de <player></red>")
                    .replace("<player>", ownerName);
            hologram.customName(plugin.getMessageManager().parse(holoText));

            activeGraves.put(loc, new Grave(d.owner(), loc, items, hologram, d.createdAt()));

            // Re-schedule despawn
            long elapsed = System.currentTimeMillis() - d.createdAt();
            long remaining = (plugin.getConfig().getInt("graves.despawn-minutes", 15) * 60 * 1000L) - elapsed;
            if (remaining > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (activeGraves.containsKey(loc)) {
                        removeGrave(loc, true);
                    }
                }, remaining / 50L);

                startParticleTask(loc);
            } else {
                removeGrave(loc, true);
            }
        }
    }

    public void createGrave(Player player, ItemStack[] items) {
        Location loc = player.getLocation().getBlock().getLocation();

        // Find a suitable spot if the current one is occupied
        if (loc.getBlock().getType() != Material.AIR && loc.getBlock().getType() != Material.CAVE_AIR) {
            loc.add(0, 1, 0);
        }

        Block block = loc.getBlock();
        block.setType(Material.PLAYER_HEAD);
        if (block.getState() instanceof Skull skull) {
            skull.setOwningPlayer(player);
            skull.getPersistentDataContainer().set(graveKey, PersistentDataType.STRING, player.getUniqueId().toString());
            skull.update();
        }

        // Create Hologram
        ArmorStand hologram = (ArmorStand) loc.getWorld().spawnEntity(loc.clone().add(0.5, -0.5, 0.5), EntityType.ARMOR_STAND);
        hologram.setVisible(false);
        hologram.setGravity(false);
        hologram.setCustomNameVisible(true);
        String holoText = plugin.getConfig().getString("messages.grave-hologram", "<red>☠ Tumba de <player></red>")
                .replace("<player>", player.getName());
        hologram.customName(plugin.getMessageManager().parse(holoText));

        long now = System.currentTimeMillis();
        Grave grave = new Grave(player.getUniqueId(), loc, items, hologram, now);
        activeGraves.put(loc, grave);

        // Save to DB
        String itemsBase64 = net.wrpnetwork.survivalcore.util.InventoryUtils.itemStackArrayToBase64(items);
        plugin.getDatabaseManager().saveGrave(player.getUniqueId(), loc, itemsBase64, now);

        plugin.getMessageManager().sendMessage(player, plugin.getConfig().getString("messages.grave-created", "<red>☠ Tumba creada en tu posición de muerte.</red>"));

        if (plugin.getConfig().getBoolean("graves.give-compass", true)) {
            ItemStack compass = new ItemStack(Material.COMPASS);
            // In 1.20.1+ we can use CompassMeta to point to a location
            player.getInventory().addItem(compass);
            player.setCompassTarget(loc);
        }

        // Particles task
        if (plugin.getConfig().getBoolean("graves.particles", true)) {
            startParticleTask(loc);
        }

        // Auto-despawn task
        int minutes = plugin.getConfig().getInt("graves.despawn-minutes", 15);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (activeGraves.containsKey(loc)) {
                removeGrave(loc, true);
            }
        }, minutes * 60 * 20L);
    }

    public void removeGrave(Location loc, boolean dropItems) {
        Grave grave = activeGraves.remove(loc);
        if (grave == null) return;

        // Remove from DB
        plugin.getDatabaseManager().deleteGrave(loc);

        loc.getBlock().setType(Material.AIR);
        if (grave.hologram() != null) {
            grave.hologram().remove();
        }

        if (dropItems) {
            for (ItemStack item : grave.items()) {
                if (item != null && item.getType() != Material.AIR) {
                    loc.getWorld().dropItemNaturally(loc, item);
                }
            }
        }
    }

    public Grave getGrave(Location loc) {
        return activeGraves.get(loc);
    }

    public boolean isGraveBlock(Block block) {
        if (block.getType() != Material.PLAYER_HEAD) return false;
        if (block.getState() instanceof Skull skull) {
            return skull.getPersistentDataContainer().has(graveKey, PersistentDataType.STRING);
        }
        return false;
    }

    public UUID getGraveOwner(Block block) {
        if (block.getState() instanceof Skull skull) {
            String uuidStr = skull.getPersistentDataContainer().get(graveKey, PersistentDataType.STRING);
            if (uuidStr != null) return UUID.fromString(uuidStr);
        }
        return null;
    }

    private void startParticleTask(Location loc) {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!activeGraves.containsKey(loc)) {
                    this.cancel();
                    return;
                }
                loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_NORMAL, loc.clone().add(0.5, 0.5, 0.5), 5, 0.1, 0.1, 0.1, 0.05);
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public Map<Location, Grave> getActiveGraves() {
        return activeGraves;
    }

    public void cleanup() {
        for (Grave grave : activeGraves.values()) {
            if (grave.hologram() != null) {
                grave.hologram().remove();
            }
        }
    }

    public record Grave(UUID owner, Location location, ItemStack[] items, ArmorStand hologram, long createdAt) {}
}
