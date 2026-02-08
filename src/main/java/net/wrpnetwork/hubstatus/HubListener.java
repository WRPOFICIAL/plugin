package net.wrpnetwork.hubstatus;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class HubListener implements Listener {

    private final WRPHubStatus plugin;
    private final ItemManager itemManager;

    public HubListener(WRPHubStatus plugin, ItemManager itemManager) {
        this.plugin = plugin;
        this.itemManager = itemManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        giveItemsAndScoreboard(event.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        giveItemsAndScoreboard(event.getPlayer());
    }

    private void giveItemsAndScoreboard(Player player) {
        itemManager.updatePlayerItems(player);

        // Initial scoreboard set
        if (plugin.getConfig().getBoolean("scoreboard.enabled", true)) {
            plugin.getScoreboardManager().updateScoreboard(player);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("item.enabled", true)) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;

        if (isHubItem(item)) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            String serverId = itemManager.getServerId(item);
            String command = plugin.getConfig().getString("item-defaults.command", "server %server%")
                    .replace("%server%", serverId);
            player.performCommand(command);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent event) {
        if (plugin.getConfig().getBoolean("item.unmovable", true) && isHubItem(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(plugin.color("&8Administrar Estados"))) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player) {
                Player player = (Player) event.getWhoClicked();
                handleGUIClick(player, event);
            }
            return;
        }

        if (!plugin.getConfig().getBoolean("item.unmovable", true)) return;

        ItemStack item = event.getCurrentItem();
        if (item != null && isHubItem(item)) {
            event.setCancelled(true);
        }

        // Also check hotbar swap
        if (event.getClick().isKeyboardClick()) {
            ItemStack hotbarItem = event.getWhoClicked().getInventory().getItem(event.getHotbarButton());
            if (hotbarItem != null && isHubItem(hotbarItem)) {
                event.setCancelled(true);
            }
        }
    }

    private void handleGUIClick(Player player, InventoryClickEvent event) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() != Material.BEACON) return;

        String displayName = org.bukkit.ChatColor.stripColor(item.getItemMeta().getDisplayName());
        StatusManager.ServerData data = null;
        for (StatusManager.ServerData sd : plugin.getStatusManager().getServerDataMap().values()) {
            if (sd.getDisplayName().equalsIgnoreCase(displayName)) {
                data = sd;
                break;
            }
        }

        if (data == null) return;

        if (event.getClick() == ClickType.LEFT) {
            data.setCurrentState("ONLINE");
        } else if (event.getClick() == ClickType.RIGHT) {
            data.setCurrentState("OFFLINE");
        } else {
            data.setCurrentState("MANTENIMIENTO");
        }

        player.sendMessage(plugin.getMessage("state-changed").replace("%state%", data.getCurrentState()));
        new StatusGUI(plugin, plugin.getStatusManager()).open(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        if (plugin.getConfig().getBoolean("item.unmovable", true)) {
            if (isHubItem(event.getMainHandItem()) || isHubItem(event.getOffHandItem())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent event) {
        if (plugin.getConfig().getBoolean("item.unmovable", true)) {
            if (isHubItem(event.getOldCursor()) || isHubItem(event.getCursor())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        if (plugin.getConfig().getBoolean("item.unmovable", true)) {
            event.getDrops().removeIf(this::isHubItem);
        }
    }

    private boolean isHubItem(ItemStack item) {
        return itemManager.isHubItem(item);
    }
}
