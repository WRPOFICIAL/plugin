package net.wrpnetwork.hubstatus;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemManager {

    private final WRPHubStatus plugin;
    private final StatusManager statusManager;

    public ItemManager(WRPHubStatus plugin, StatusManager statusManager) {
        this.plugin = plugin;
        this.statusManager = statusManager;
    }

    public ItemStack createServerItem(StatusManager.ServerData data) {
        ConfigurationSection serverSec = plugin.getConfig().getConfigurationSection("servers." + data.getId());
        ConfigurationSection itemSec = serverSec.getConfigurationSection("item");

        String materialName = plugin.getConfig().getString("item-defaults.material", "PLAYER_HEAD");
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = Material.PLAYER_HEAD;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Set Name and Lore with Placeholders
        String name = itemSec != null ? itemSec.getString("name") : null;
        if (name == null) name = "&6&l" + data.getDisplayName();
        meta.setDisplayName(plugin.getPlaceholderProvider().parse(null, name));

        updateLore(meta, data);

        // Enchantment Glow
        if (plugin.getConfig().getBoolean("item-defaults.enchanted", true)) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_PLACED_ON);

        // Set PersistentData
        NamespacedKey key = new NamespacedKey(plugin, "hub_item");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, data.getId());

        // Custom Texture
        if (material == Material.PLAYER_HEAD && meta instanceof SkullMeta && itemSec != null) {
            String texture = itemSec.getString("texture");
            if (texture != null && !texture.isEmpty()) {
                applyTexture((SkullMeta) meta, texture);
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    private void updateLore(ItemMeta meta, StatusManager.ServerData data) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(plugin.getPlaceholderProvider().parse(null, "&7Estado: %status_" + data.getId() + "%"));
        lore.add(plugin.getPlaceholderProvider().parse(null, "&7Jugadores: &f%players_" + data.getId() + "%/%max_" + data.getId() + "%"));
        lore.add("");
        lore.add(plugin.color("&eClick para entrar"));
        meta.setLore(lore);
    }

    public void updatePlayerItems(org.bukkit.entity.Player player) {
        if (!player.getWorld().getName().equalsIgnoreCase(plugin.getConfig().getString("item-defaults.world", "world"))) return;

        for (StatusManager.ServerData data : statusManager.getServerDataMap().values()) {
            ConfigurationSection itemSec = plugin.getConfig().getConfigurationSection("servers." + data.getId() + ".item");
            if (itemSec == null || !itemSec.getBoolean("enabled", true)) continue;

            int slot = itemSec.getInt("slot", 4);
            ItemStack currentItem = player.getInventory().getItem(slot);
            ItemStack newItem = createServerItem(data);

            if (currentItem == null || isHubItem(currentItem)) {
                if (currentItem != null && currentItem.isSimilar(newItem)) continue;
                player.getInventory().setItem(slot, newItem);
            }
        }
    }

    public boolean isHubItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey key = new NamespacedKey(plugin, "hub_item");
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    public String getServerId(ItemStack item) {
        if (!isHubItem(item)) return null;
        NamespacedKey key = new NamespacedKey(plugin, "hub_item");
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    private void applyTexture(SkullMeta meta, String base64) {
        try {
            GameProfile profile = new GameProfile(UUID.randomUUID(), null);
            profile.getProperties().put("textures", new Property("textures", base64));

            Field profileField = null;
            Class<?> clazz = meta.getClass();
            while (clazz != null) {
                try {
                    profileField = clazz.getDeclaredField("profile");
                    break;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }

            if (profileField != null) {
                profileField.setAccessible(true);
                profileField.set(meta, profile);
            }
        } catch (IllegalAccessException e) {
            plugin.getLogger().warning("Could not access 'profile' field in SkullMeta.");
        }
    }
}
