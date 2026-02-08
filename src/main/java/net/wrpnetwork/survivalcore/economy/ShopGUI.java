package net.wrpnetwork.survivalcore.economy;

import net.kyori.adventure.text.Component;
import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ShopGUI implements Listener {

    private final SurvivalCore plugin;
    private final NamespacedKey itemKey;

    public ShopGUI(SurvivalCore plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "shop_item_id");
    }

    public void openCategories(Player player, boolean isBuy) {
        String title = plugin.getConfig().getString("shop.category-title", "<dark_aqua>📂 Categorías de Tienda</dark_aqua>");
        Inventory inv = Bukkit.createInventory(new ShopHolder(isBuy, null, true), 27, plugin.getMessageManager().parse(title));

        if (plugin.getConfig().getConfigurationSection("shop.categories") != null) {
            for (String key : plugin.getConfig().getConfigurationSection("shop.categories").getKeys(false)) {
                String path = "shop.categories." + key + ".";
                Material mat = Material.valueOf(plugin.getConfig().getString(path + "material", "PAPER"));
                String name = plugin.getConfig().getString(path + "name", key);
                int slot = plugin.getConfig().getInt(path + "slot", 0);

                ItemStack item = new ItemStack(mat);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(plugin.getMessageManager().parse(name));
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "category_id"), PersistentDataType.STRING, key);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "is_buy"), PersistentDataType.INTEGER, isBuy ? 1 : 0);
                item.setItemMeta(meta);
                inv.setItem(slot, item);
            }
        }
        player.openInventory(inv);
    }

    public void openShop(Player player, boolean isBuy, String category) {
        String title = isBuy ? plugin.getConfig().getString("shop.title", "<dark_aqua>🛒 Tienda WRP · Compra</dark_aqua>")
                            : plugin.getConfig().getString("shop.sell-title", "<dark_green>💰 Tienda WRP · Venta</dark_green>");
        Inventory inv = Bukkit.createInventory(new ShopHolder(isBuy, category, false), 54, plugin.getMessageManager().parse(title));

        if (plugin.getConfig().getConfigurationSection("shop.items") != null) {
            for (String key : plugin.getConfig().getConfigurationSection("shop.items").getKeys(false)) {
                String path = "shop.items." + key + ".";
                String itemCategory = plugin.getConfig().getString(path + "category", "none");
                if (!itemCategory.equalsIgnoreCase(category)) continue;

                Material mat = Material.valueOf(plugin.getConfig().getString(path + "material", "STONE"));
                String name = plugin.getConfig().getString(path + "name", key);
                double buy = isBuy ? plugin.getConfig().getDouble(path + "buy-price", 0) : 0;
                double sell = !isBuy ? plugin.getConfig().getDouble(path + "sell-price", 0) : 0;
                int slot = plugin.getConfig().getInt(path + "slot", 0);
                String special = plugin.getConfig().getString(path + "special", "");

                if (isBuy && buy <= 0) continue;
                if (!isBuy && sell <= 0) continue;

                addItem(inv, slot, mat, name, buy, sell, special, isBuy);
            }
        }

        // Back button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.displayName(plugin.getMessageManager().parse("<red>Volver</red>"));
        backMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "back_to_cats"), PersistentDataType.INTEGER, isBuy ? 1 : 0);
        back.setItemMeta(backMeta);
        inv.setItem(49, back);

        player.openInventory(inv);
    }

    private void addItem(Inventory inv, int slot, Material mat, String name, double buyPrice, double sellPrice, String special, boolean isBuy) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.getMessageManager().parse(name));
        List<Component> lore = new ArrayList<>();
        if (buyPrice > 0) {
            lore.add(plugin.getMessageManager().parse("<grey>Precio Compra: <green>$" + buyPrice + "</green></grey>"));
            lore.add(plugin.getMessageManager().parse("<yellow>Clic Izquierdo para Comprar</yellow>"));
        }
        if (sellPrice > 0) {
            lore.add(plugin.getMessageManager().parse("<grey>Precio Venta: <red>$" + sellPrice + "</red></grey>"));
            lore.add(plugin.getMessageManager().parse("<yellow>Clic Izquierdo para Vender</yellow>"));
        }
        meta.lore(lore);

        // Tag for price info
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "buy_price"), PersistentDataType.DOUBLE, buyPrice);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "sell_price"), PersistentDataType.DOUBLE, sellPrice);
        if (!special.isEmpty()) {
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "special_type"), PersistentDataType.STRING, special);
        }

        item.setItemMeta(meta);
        inv.setItem(slot, item);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopHolder holder)) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();
        ItemMeta meta = item.getItemMeta();

        if (holder.isCategoryView()) {
            String category = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "category_id"), PersistentDataType.STRING);
            if (category != null) {
                openShop(player, holder.isBuy(), category);
            }
            return;
        }

        if (meta.getPersistentDataContainer().has(new NamespacedKey(plugin, "back_to_cats"), PersistentDataType.INTEGER)) {
            openCategories(player, holder.isBuy());
            return;
        }

        Double buyPrice = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "buy_price"), PersistentDataType.DOUBLE);
        Double sellPrice = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "sell_price"), PersistentDataType.DOUBLE);

        if (holder.isBuy() && event.isLeftClick() && buyPrice != null && buyPrice > 0) {
            if (plugin.getEconomyManager().withdraw(player.getUniqueId(), buyPrice)) {
                ItemStack bought = new ItemStack(item.getType());
                String special = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "special_type"), PersistentDataType.STRING);
                if ("upgrade_core".equals(special)) {
                    // It's an upgrade core
                    ItemMeta coreMeta = bought.getItemMeta();
                    coreMeta.displayName(plugin.getMessageManager().parse("<gold>Núcleo de Mejora de Hogar</gold>"));
                    List<Component> coreLore = new ArrayList<>();
                    coreLore.add(plugin.getMessageManager().parse("<grey>Usa este núcleo en tu hogar para subir de nivel.</grey>"));
                    coreLore.add(plugin.getMessageManager().parse("<yellow>Radio:</yellow> <white>+10 bloques</white>"));
                    coreMeta.lore(coreLore);
                    coreMeta.getPersistentDataContainer().set(new NamespacedKey(plugin, "home_upgrade"), PersistentDataType.INTEGER, 1);
                    bought.setItemMeta(coreMeta);
                }
                player.getInventory().addItem(bought);
                plugin.getMessageManager().sendMessage(player, "<green>✔ Has comprado " + item.getType().name() + " por $" + buyPrice + ".</green>");
            } else {
                plugin.getMessageManager().sendMessage(player, "<red>✖ No tienes suficiente dinero.</red>");
            }
        } else if (!holder.isBuy() && event.isLeftClick() && sellPrice != null && sellPrice > 0) {
            if (player.getInventory().contains(item.getType())) {
                removeItem(player, item.getType(), 1);
                plugin.getEconomyManager().addBalance(player.getUniqueId(), sellPrice);
                plugin.getMessageManager().sendMessage(player, "<green>✔ Has vendido " + item.getType().name() + " por $" + sellPrice + ".</green>");
            } else {
                plugin.getMessageManager().sendMessage(player, "<red>✖ No tienes este objeto en tu inventario.</red>");
            }
        }
    }

    private void removeItem(Player player, Material mat, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is != null && is.getType() == mat) {
                if (is.getAmount() > remaining) {
                    is.setAmount(is.getAmount() - remaining);
                    player.getInventory().setItem(i, is);
                    remaining = 0;
                } else {
                    remaining -= is.getAmount();
                    player.getInventory().setItem(i, null);
                }
                if (remaining <= 0) break;
            }
        }
    }
}
