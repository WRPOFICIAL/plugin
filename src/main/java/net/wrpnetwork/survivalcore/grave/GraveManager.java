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
    private final Map<String, Grave> activeGraves = new HashMap<>();
    private final NamespacedKey graveKey;

    public GraveManager(SurvivalCore plugin) {
        this.plugin = plugin;
        this.graveKey = new NamespacedKey(plugin, "grave_owner");
        loadGraves();
    }

    private String getLocKey(Location loc) {
        if (loc == null || loc.getWorld() == null) return "unknown";
        return loc.getWorld().getName() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private void loadGraves() {
        List<net.wrpnetwork.survivalcore.database.DatabaseManager.GraveData> data = plugin.getDatabaseManager().loadAllGraves();
        for (net.wrpnetwork.survivalcore.database.DatabaseManager.GraveData d : data) {
            org.bukkit.World world = Bukkit.getWorld(d.world());
            if (world == null) continue;
            Location loc = new Location(world, d.x(), d.y(), d.z());
            ItemStack[] items = net.wrpnetwork.survivalcore.util.InventoryUtils.itemStackArrayFromBase64(d.itemsBase64());

            String ownerName = Bukkit.getOfflinePlayer(d.owner()).getName();
            if (ownerName == null) ownerName = "Jugador";
            String finalOwnerName = ownerName;
            String holoText = plugin.getConfig().getString("messages.grave-hologram", "<red>☠ Tumba de <player></red>")
                    .replace("<player>", finalOwnerName);

            // Recreate hologram using consumer for better reliability
            ArmorStand hologram = world.spawn(loc.clone().add(0.5, 0.0, 0.5), ArmorStand.class, armorStand -> {
                armorStand.setVisible(false);
                armorStand.setGravity(false);
                armorStand.setMarker(true);
                armorStand.setCustomNameVisible(true);
                armorStand.customName(plugin.getMessageManager().parse(holoText));
            });

            String key = getLocKey(loc);
            activeGraves.put(key, new Grave(d.owner(), loc, items, hologram, d.createdAt()));

            // Re-schedule despawn
            long elapsed = System.currentTimeMillis() - d.createdAt();
            long remaining = (plugin.getConfig().getInt("graves.despawn-minutes", 15) * 60 * 1000L) - elapsed;
            if (remaining > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (activeGraves.containsKey(key)) {
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

        String holoText = plugin.getConfig().getString("messages.grave-hologram", "<red>☠ Tumba de <player></red>")
                .replace("<player>", player.getName());

        // Create Hologram using consumer
        ArmorStand hologram = loc.getWorld().spawn(loc.clone().add(0.5, 0.0, 0.5), ArmorStand.class, armorStand -> {
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setMarker(true);
            armorStand.setCustomNameVisible(true);
            armorStand.customName(plugin.getMessageManager().parse(holoText));
        });

        long now = System.currentTimeMillis();
        Grave grave = new Grave(player.getUniqueId(), loc, items, hologram, now);
        String key = getLocKey(loc);
        activeGraves.put(key, grave);

        // Save to DB
        String itemsBase64 = net.wrpnetwork.survivalcore.util.InventoryUtils.itemStackArrayToBase64(items);
        plugin.getDatabaseManager().saveGrave(player.getUniqueId(), loc, itemsBase64, now);

        plugin.getMessageManager().sendMessage(player, plugin.getConfig().getString("messages.grave-created", "<red>☠ Tumba creada en tu posición de muerte.</red>"));

        if (plugin.getConfig().getBoolean("graves.give-compass", true)) {
            ItemStack compass = new ItemStack(Material.COMPASS);
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
            if (activeGraves.containsKey(key)) {
                removeGrave(loc, true);
            }
        }, minutes * 60 * 20L);
    }

    public void removeGrave(Location loc, boolean dropItems) {
        String key = getLocKey(loc);
        Grave grave = activeGraves.remove(key);
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
        return activeGraves.get(getLocKey(loc));
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
        String key = getLocKey(loc);
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                if (!activeGraves.containsKey(key)) {
                    this.cancel();
                    return;
                }
                loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE_NORMAL, loc.clone().add(0.5, 0.5, 0.5), 5, 0.1, 0.1, 0.1, 0.05);
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public Map<String, Grave> getActiveGraves() {
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
