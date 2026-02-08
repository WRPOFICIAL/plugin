package net.wrpnetwork.hubstatus;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatusGUI {

    private final WRPHubStatus plugin;
    private final StatusManager statusManager;

    public StatusGUI(WRPHubStatus plugin, StatusManager statusManager) {
        this.plugin = plugin;
        this.statusManager = statusManager;
    }

    public void open(Player player) {
        Map<String, StatusManager.ServerData> servers = statusManager.getServerDataMap();
        int size = ((servers.size() / 9) + 1) * 9;
        Inventory inv = Bukkit.createInventory(null, size, plugin.color("&8Administrar Estados"));

        int slot = 0;
        for (StatusManager.ServerData data : servers.values()) {
            inv.setItem(slot++, createServerItem(data));
        }

        player.openInventory(inv);
    }

    private ItemStack createServerItem(StatusManager.ServerData data) {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.color("&e" + data.getDisplayName()));
            List<String> lore = new ArrayList<>();
            lore.add(plugin.color("&7ID: &f" + data.getId()));
            lore.add(plugin.color("&7Estado Actual: " + statusManager.getStateColor(data.getCurrentState()) + data.getCurrentState()));
            lore.add(plugin.color("&7Modo: &f" + (data.isAutoStatus() ? "Automático" : "Manual")));
            lore.add("");
            lore.add(plugin.color("&eClick Izquierdo: &aONLINE"));
            lore.add(plugin.color("&eClick Derecho: &cOFFLINE"));
            lore.add(plugin.color("&eClick Central/Shift: &6MANTENIMIENTO"));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
