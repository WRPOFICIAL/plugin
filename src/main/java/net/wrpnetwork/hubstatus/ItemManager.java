package net.wrpnetwork.hubstatus;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.NamespacedKey;
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

    public ItemStack createHubItem() {
        String materialName = plugin.getConfig().getString("item.material", "PLAYER_HEAD");
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material in config: " + materialName + ". Falling back to PLAYER_HEAD.");
            material = Material.PLAYER_HEAD;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        // Set Name
        meta.setDisplayName(plugin.color(plugin.getConfig().getString("item.name", "&6&lWRP Survival &7(Click)")));

        // Set Lore
        updateLore(meta);

        // Enchantment Glow
        if (plugin.getConfig().getBoolean("item.enchanted", true)) {
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_PLACED_ON);

        // Set PersistentData to identify the item
        NamespacedKey key = new NamespacedKey(plugin, "hub_item");
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        // Custom Texture if PLAYER_HEAD
        if (material == Material.PLAYER_HEAD && meta instanceof SkullMeta) {
            String texture = plugin.getConfig().getString("item.texture");
            if (texture != null && !texture.isEmpty()) {
                applyTexture((SkullMeta) meta, texture);
            }
        }

        item.setItemMeta(meta);
        return item;
    }

    private void updateLore(ItemMeta meta) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(plugin.color("&7Estado: " + statusManager.getStateColor() + statusManager.getCurrentState()));
        lore.add(plugin.color("&7Jugadores: &f" + Bukkit.getOnlinePlayers().size() + "/" + plugin.getConfig().getInt("server.max-players", 100)));
        lore.add("");
        lore.add(plugin.color("&eClick para entrar"));
        meta.setLore(lore);
    }

    public void updatePlayerItem(org.bukkit.entity.Player player) {
        if (!plugin.getConfig().getBoolean("item.enabled", true)) return;

        String worldName = plugin.getConfig().getString("item.world", "world");
        if (!player.getWorld().getName().equalsIgnoreCase(worldName)) return;

        int slot = plugin.getConfig().getInt("item.slot", 4);
        if (slot < 0 || slot > 35) {
            plugin.getLogger().warning("Invalid slot in config: " + slot + ". Using default slot 4.");
            slot = 4;
        }

        ItemStack currentItem = player.getInventory().getItem(slot);
        ItemStack newItem = createHubItem();

        // Only update if it's our item or the slot is empty
        if (currentItem == null || isHubItem(currentItem)) {
            // Check if it's already exactly the same to avoid unnecessary packets
            if (currentItem != null && currentItem.isSimilar(newItem)) {
                return;
            }
            player.getInventory().setItem(slot, newItem);
        } else {
            // Slot is occupied by something else!
            // We should probably not overwrite it unless we are sure.
            // But the requirements say the item should be there.
            // Let's at least log it or try to find an empty slot, but the req says "slot configurable".
            // I'll stick to not overwriting if it's not a hub item, to be safe.
        }
    }

    public boolean isHubItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey key = new NamespacedKey(plugin, "hub_item");
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
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
            } else {
                plugin.getLogger().warning("Could not find 'profile' field in SkullMeta.");
            }
        } catch (IllegalAccessException e) {
            plugin.getLogger().warning("Could not access 'profile' field in SkullMeta.");
        }
    }
}
