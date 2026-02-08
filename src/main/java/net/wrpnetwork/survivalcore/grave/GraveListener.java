package net.wrpnetwork.survivalcore.grave;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class GraveListener implements Listener {

    private final SurvivalCore plugin;
    private final GraveManager graveManager;

    public GraveListener(SurvivalCore plugin, GraveManager graveManager) {
        this.plugin = plugin;
        this.graveManager = graveManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("modules.graves", true)) return;

        Player player = event.getEntity();
        if (event.getDrops().isEmpty()) return;

        ItemStack[] drops = event.getDrops().toArray(new ItemStack[0]);
        event.getDrops().clear(); // Prevent items from dropping on ground

        graveManager.createGrave(player, drops);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.PLAYER_HEAD) return;

        if (graveManager.isGraveBlock(event.getClickedBlock())) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            UUID owner = graveManager.getGraveOwner(event.getClickedBlock());

            if (owner.equals(player.getUniqueId()) || player.hasPermission("survivalcore.graves.admin")) {
                graveManager.removeGrave(event.getClickedBlock().getLocation(), true);
                plugin.getMessageManager().sendMessage(player, "<green>✔ Has recuperado tus pertenencias.</green>");
            } else {
                plugin.getMessageManager().sendMessage(player, "<red>✖ Esta tumba no te pertenece.</red>");
            }
        }
    }
}
