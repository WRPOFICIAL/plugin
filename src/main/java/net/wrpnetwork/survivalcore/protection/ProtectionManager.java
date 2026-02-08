package net.wrpnetwork.survivalcore.protection;

import net.wrpnetwork.survivalcore.SurvivalCore;
import net.wrpnetwork.survivalcore.database.DatabaseManager;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ProtectionManager {

    private final SurvivalCore plugin;
    private final Map<String, List<DatabaseManager.HomeData>> worldHomeCache = new ConcurrentHashMap<>();
    private final Map<String, List<UUID>> trustCache = new ConcurrentHashMap<>(); // owner:homeName -> trusted

    public ProtectionManager(SurvivalCore plugin) {
        this.plugin = plugin;
        loadAll();
    }

    public void loadAll() {
        worldHomeCache.clear();
        trustCache.clear();
        List<DatabaseManager.HomeData> allHomes = plugin.getDatabaseManager().getAllHomes();
        for (DatabaseManager.HomeData home : allHomes) {
            worldHomeCache.computeIfAbsent(home.world(), k -> new ArrayList<>()).add(home);

            String key = home.owner().toString() + ":" + home.name();
            trustCache.put(key, plugin.getDatabaseManager().getTrusted(home.owner(), home.name()));
        }
    }

    public void updateHome(UUID owner, String name, Location loc, int level) {
        // Remove old if exists
        List<DatabaseManager.HomeData> homes = worldHomeCache.get(loc.getWorld().getName());
        if (homes != null) {
            homes.removeIf(h -> h.owner().equals(owner) && h.name().equalsIgnoreCase(name));
        }

        // Add new
        DatabaseManager.HomeData newData = new DatabaseManager.HomeData(owner, name, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), level);
        worldHomeCache.computeIfAbsent(loc.getWorld().getName(), k -> new ArrayList<>()).add(newData);
    }

    public void removeHome(UUID owner, String name, String world) {
        List<DatabaseManager.HomeData> homes = worldHomeCache.get(world);
        if (homes != null) {
            homes.removeIf(h -> h.owner().equals(owner) && h.name().equalsIgnoreCase(name));
        }
        trustCache.remove(owner.toString() + ":" + name);
    }

    public void updateTrust(UUID owner, String homeName) {
        String key = owner.toString() + ":" + homeName;
        trustCache.put(key, plugin.getDatabaseManager().getTrusted(owner, homeName));
    }

    public boolean canBuildAt(UUID playerUuid, Location loc) {
        List<DatabaseManager.HomeData> homes = worldHomeCache.get(loc.getWorld().getName());
        if (homes == null) return true;

        for (DatabaseManager.HomeData home : homes) {
            int radius = home.level() * 10;
            double distanceSq = Math.pow(loc.getX() - home.x(), 2) + Math.pow(loc.getZ() - home.z(), 2);
            if (distanceSq <= Math.pow(radius, 2)) {
                if (home.owner().equals(playerUuid)) return true;

                List<UUID> trusted = trustCache.get(home.owner().toString() + ":" + home.name());
                if (trusted != null && trusted.contains(playerUuid)) return true;

                return false; // Protected
            }
        }
        return true;
    }

    public boolean isLocationProtected(Location loc) {
        List<DatabaseManager.HomeData> homes = worldHomeCache.get(loc.getWorld().getName());
        if (homes == null) return false;

        for (DatabaseManager.HomeData home : homes) {
            int radius = home.level() * 10;
            double distanceSq = Math.pow(loc.getX() - home.x(), 2) + Math.pow(loc.getZ() - home.z(), 2);
            if (distanceSq <= Math.pow(radius, 2)) return true;
        }
        return false;
    }
}
