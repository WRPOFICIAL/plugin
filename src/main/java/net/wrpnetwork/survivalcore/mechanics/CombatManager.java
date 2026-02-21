package net.wrpnetwork.survivalcore.mechanics;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatManager implements Listener {

    private final SurvivalCore plugin;
    private final Map<UUID, Long> tagged = new HashMap<>();
    private final int tagDuration = 15; // 15 seconds

    public CombatManager(SurvivalCore plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCombat(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player victim && event.getDamager() instanceof Player attacker) {
            tag(victim);
            tag(attacker);
        }
    }

    private void tag(Player player) {
        if (!tagged.containsKey(player.getUniqueId())) {
            plugin.getMessageManager().sendActionBar(player, "<red>⚔ ¡Estás en combate! No te desconectes.</red>");
        }
        tagged.put(player.getUniqueId(), System.currentTimeMillis() + (tagDuration * 1000L));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isTagged(player)) {
            player.setHealth(0); // Kill player for combat logging
            Bukkit.broadcast(plugin.getMessageManager().parse("<red>☠ " + player.getName() + " se desconectó en combate y ha sido castigado.</red>"));
            tagged.remove(player.getUniqueId());
        }
    }

    public boolean isTagged(Player player) {
        return tagged.containsKey(player.getUniqueId()) && tagged.get(player.getUniqueId()) > System.currentTimeMillis();
    }

    private void startCleanupTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            tagged.entrySet().removeIf(entry -> entry.getValue() < System.currentTimeMillis());
        }, 20L, 20L);
    }
}
