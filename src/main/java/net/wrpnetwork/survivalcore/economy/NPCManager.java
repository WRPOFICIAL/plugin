package net.wrpnetwork.survivalcore.economy;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.persistence.PersistentDataType;

public class NPCManager implements Listener {

    private final SurvivalCore plugin;
    private final ShopGUI shopGUI;
    private final NamespacedKey npcKey;

    public NPCManager(SurvivalCore plugin, ShopGUI shopGUI) {
        this.plugin = plugin;
        this.shopGUI = shopGUI;
        this.npcKey = new NamespacedKey(plugin, "shop_npc");
    }

    public void spawnShopNPC(Location loc, String name) {
        Villager villager = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
        villager.setCustomNameVisible(true);
        villager.customName(plugin.getMessageManager().parse(name));
        villager.setAI(false);
        villager.setInvulnerable(true);
        villager.getPersistentDataContainer().set(npcKey, PersistentDataType.INTEGER, 1);
        villager.setProfession(Villager.Profession.LIBRARIAN);
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getPersistentDataContainer().has(npcKey, PersistentDataType.INTEGER)) {
            event.setCancelled(true);
            shopGUI.openShop(event.getPlayer());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (event.getEntity().getPersistentDataContainer().has(npcKey, PersistentDataType.INTEGER)) {
            event.setCancelled(true);
            if (event.getDamager() instanceof Player player && player.isOp()) {
                // Allow shift+left click to remove? No, better a specific command
            }
        }
    }
}
