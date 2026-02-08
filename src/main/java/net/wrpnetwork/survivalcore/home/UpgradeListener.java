package net.wrpnetwork.survivalcore.home;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class UpgradeListener implements Listener {

    private final SurvivalCore plugin;
    private final NamespacedKey upgradeKey;

    public UpgradeListener(SurvivalCore plugin) {
        this.plugin = plugin;
        this.upgradeKey = new NamespacedKey(plugin, "home_upgrade");
    }

    @EventHandler
    public void onUpgrade(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        if (!item.getItemMeta().getPersistentDataContainer().has(upgradeKey, PersistentDataType.INTEGER)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();

        // Find if player is standing in one of their homes
        List<String> homes = plugin.getDatabaseManager().getHomes(player.getUniqueId());
        String currentHome = null;
        net.wrpnetwork.survivalcore.database.DatabaseManager.HomeInfo currentInfo = null;

        for (String name : homes) {
            net.wrpnetwork.survivalcore.database.DatabaseManager.HomeInfo info = plugin.getDatabaseManager().getHome(player.getUniqueId(), name);
            if (info.location().getWorld().equals(player.getWorld())) {
                double dist = Math.sqrt(Math.pow(player.getLocation().getX() - info.location().getX(), 2) +
                                       Math.pow(player.getLocation().getZ() - info.location().getZ(), 2));
                if (dist <= info.level() * 10) {
                    currentHome = name;
                    currentInfo = info;
                    break;
                }
            }
        }

        if (currentHome == null) {
            plugin.getMessageManager().sendMessage(player, "<red>✖ Debes estar dentro de tu zona protegida para mejorarla.</red>");
            return;
        }

        if (currentInfo.level() >= 3) {
            plugin.getMessageManager().sendMessage(player, "<yellow>Tu hogar ya está al nivel máximo (3).</yellow>");
            return;
        }

        int nextLevel = currentInfo.level() + 1;
        plugin.getDatabaseManager().saveHome(player.getUniqueId(), currentHome, currentInfo.location(), nextLevel);
        plugin.getProtectionManager().updateHome(player.getUniqueId(), currentHome, currentInfo.location(), nextLevel);

        item.setAmount(item.getAmount() - 1);
        plugin.getMessageManager().sendMessage(player, "<green>✔ ¡Hogar mejorado al Nivel " + nextLevel + "!</green>");
        plugin.getMessageManager().sendMessage(player, "<aqua>Nuevo radio: " + (nextLevel * 10) + " bloques.</aqua>");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
    }
}
