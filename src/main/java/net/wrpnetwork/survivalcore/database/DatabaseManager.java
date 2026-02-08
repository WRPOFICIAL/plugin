package net.wrpnetwork.survivalcore.database;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private final SurvivalCore plugin;
    private Connection connection;

    public DatabaseManager(SurvivalCore plugin) {
        this.plugin = plugin;
    }

    public boolean initialize() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "database.db");
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            createTables();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            // Homes table
            statement.execute("CREATE TABLE IF NOT EXISTS homes (" +
                    "uuid TEXT, " +
                    "name TEXT, " +
                    "world TEXT, " +
                    "x REAL, " +
                    "y REAL, " +
                    "z REAL, " +
                    "yaw REAL, " +
                    "pitch REAL, " +
                    "level INTEGER DEFAULT 1, " +
                    "PRIMARY KEY (uuid, name))");

            // Back locations table
            statement.execute("CREATE TABLE IF NOT EXISTS back_locations (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "world TEXT, " +
                    "x REAL, " +
                    "y REAL, " +
                    "z REAL, " +
                    "yaw REAL, " +
                    "pitch REAL)");

            // Graves table
            statement.execute("CREATE TABLE IF NOT EXISTS graves (" +
                    "uuid TEXT, " +
                    "world TEXT, " +
                    "x INTEGER, " +
                    "y INTEGER, " +
                    "z INTEGER, " +
                    "items TEXT, " +
                    "created_at INTEGER, " +
                    "PRIMARY KEY (world, x, y, z))");

            // Players economy table
            statement.execute("CREATE TABLE IF NOT EXISTS economy (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "balance REAL DEFAULT 0)");

            // Trust table
            statement.execute("CREATE TABLE IF NOT EXISTS home_trust (" +
                    "owner_uuid TEXT, " +
                    "home_name TEXT, " +
                    "trusted_uuid TEXT, " +
                    "PRIMARY KEY (owner_uuid, home_name, trusted_uuid))");
        }
    }

    public void saveHome(UUID uuid, String name, Location loc, int level) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO homes (uuid, name, world, x, y, z, yaw, pitch, level) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name.toLowerCase());
            ps.setString(3, loc.getWorld().getName());
            ps.setDouble(4, loc.getX());
            ps.setDouble(5, loc.getY());
            ps.setDouble(6, loc.getZ());
            ps.setFloat(7, loc.getYaw());
            ps.setFloat(8, loc.getPitch());
            ps.setInt(9, level);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public HomeInfo getHome(UUID uuid, String name) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM homes WHERE uuid = ? AND name = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name.toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Location loc = new Location(
                        Bukkit.getWorld(rs.getString("world")),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                );
                return new HomeInfo(loc, rs.getInt("level"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public record HomeInfo(Location location, int level) {}

    public List<HomeData> getAllHomes() {
        List<HomeData> homes = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT * FROM homes");
            while (rs.next()) {
                homes.add(new HomeData(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("name"),
                        rs.getString("world"),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getInt("level")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return homes;
    }

    public record HomeData(UUID owner, String name, String world, double x, double y, double z, int level) {}

    public List<String> getHomes(UUID uuid) {
        List<String> homes = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT name FROM homes WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                homes.add(rs.getString("name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return homes;
    }

    public void deleteHome(UUID uuid, String name) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM homes WHERE uuid = ? AND name = ?")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name.toLowerCase());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveBackLocation(UUID uuid, Location loc) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO back_locations (uuid, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, loc.getWorld().getName());
            ps.setDouble(3, loc.getX());
            ps.setDouble(4, loc.getY());
            ps.setDouble(5, loc.getZ());
            ps.setFloat(6, loc.getYaw());
            ps.setFloat(7, loc.getPitch());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Location getBackLocation(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM back_locations WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Location(
                        Bukkit.getWorld(rs.getString("world")),
                        rs.getDouble("x"),
                        rs.getDouble("y"),
                        rs.getDouble("z"),
                        rs.getFloat("yaw"),
                        rs.getFloat("pitch")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveGrave(UUID uuid, Location loc, String itemsBase64, long createdAt) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO graves (uuid, world, x, y, z, items, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, loc.getWorld().getName());
            ps.setInt(3, loc.getBlockX());
            ps.setInt(4, loc.getBlockY());
            ps.setInt(5, loc.getBlockZ());
            ps.setString(6, itemsBase64);
            ps.setLong(7, createdAt);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteGrave(Location loc) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM graves WHERE world = ? AND x = ? AND y = ? AND z = ?")) {
            ps.setString(1, loc.getWorld().getName());
            ps.setInt(2, loc.getBlockX());
            ps.setInt(3, loc.getBlockY());
            ps.setInt(4, loc.getBlockZ());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<GraveData> loadAllGraves() {
        List<GraveData> graves = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery("SELECT * FROM graves");
            while (rs.next()) {
                graves.add(new GraveData(
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("world"),
                        rs.getInt("x"),
                        rs.getInt("y"),
                        rs.getInt("z"),
                        rs.getString("items"),
                        rs.getLong("created_at")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return graves;
    }

    public double getBalance(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT balance FROM economy WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble("balance");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void setBalance(UUID uuid, double balance) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO economy (uuid, balance) VALUES (?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setDouble(2, balance);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addTrust(UUID owner, String homeName, UUID trusted) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR IGNORE INTO home_trust (owner_uuid, home_name, trusted_uuid) VALUES (?, ?, ?)")) {
            ps.setString(1, owner.toString());
            ps.setString(2, homeName.toLowerCase());
            ps.setString(3, trusted.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeTrust(UUID owner, String homeName, UUID trusted) {
        try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM home_trust WHERE owner_uuid = ? AND home_name = ? AND trusted_uuid = ?")) {
            ps.setString(1, owner.toString());
            ps.setString(2, homeName.toLowerCase());
            ps.setString(3, trusted.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<UUID> getTrusted(UUID owner, String homeName) {
        List<UUID> trusted = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT trusted_uuid FROM home_trust WHERE owner_uuid = ? AND home_name = ?")) {
            ps.setString(1, owner.toString());
            ps.setString(2, homeName.toLowerCase());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                trusted.add(UUID.fromString(rs.getString("trusted_uuid")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return trusted;
    }

    public boolean canPlayerBuildAt(UUID playerUuid, Location loc) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT uuid, name, x, y, z, level FROM homes WHERE world = ?")) {
            ps.setString(1, loc.getWorld().getName());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                UUID owner = UUID.fromString(rs.getString("uuid"));
                String name = rs.getString("name");
                double hX = rs.getDouble("x");
                double hY = rs.getDouble("y");
                double hZ = rs.getDouble("z");
                int level = rs.getInt("level");
                int radius = level * 10;

                double distance = Math.sqrt(Math.pow(loc.getX() - hX, 2) + Math.pow(loc.getZ() - hZ, 2));
                if (distance <= radius) {
                    if (owner.equals(playerUuid)) return true;
                    if (getTrusted(owner, name).contains(playerUuid)) return true;
                    return false; // Protected by someone else
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true; // Not in any protection zone
    }

    public boolean isLocationProtected(Location loc) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT x, z, level FROM homes WHERE world = ?")) {
            ps.setString(1, loc.getWorld().getName());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double hX = rs.getDouble("x");
                double hZ = rs.getDouble("z");
                int level = rs.getInt("level");
                int radius = level * 10;

                double distance = Math.sqrt(Math.pow(loc.getX() - hX, 2) + Math.pow(loc.getZ() - hZ, 2));
                if (distance <= radius) return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public record GraveData(UUID owner, String world, int x, int y, int z, String itemsBase64, long createdAt) {}

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
