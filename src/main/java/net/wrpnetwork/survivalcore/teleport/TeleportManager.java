package net.wrpnetwork.survivalcore.teleport;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager {

    private final SurvivalCore plugin;
    private final Map<UUID, BukkitTask> pendingTeleports = new HashMap<>();
    private final Map<UUID, UUID> tpaRequests = new HashMap<>(); // target -> requester

    public TeleportManager(SurvivalCore plugin) {
        this.plugin = plugin;
    }

    public void teleport(Player player, Location location) {
        if (plugin.getConfig().getBoolean("teleport.combat-block", true) &&
            plugin.getCombatManager() != null &&
            plugin.getCombatManager().isTagged(player)) {
            plugin.getMessageManager().sendMessage(player, "<red>✖ No puedes teletransportarte mientras estás en combate.</red>");
            return;
        }

        int delay = plugin.getConfig().getInt("teleport.delay", 3);

        if (delay <= 0 || player.hasPermission("survivalcore.teleport.bypass")) {
            performTeleport(player, location);
            return;
        }

        cancelPending(player);

        String startMsg = plugin.getConfig().getString("messages.teleport-start", "<yellow>Teletransportando en <count> segundos...</yellow>")
                .replace("<count>", String.valueOf(delay));
        plugin.getMessageManager().sendActionBar(player, startMsg);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);

        BukkitTask task = new BukkitRunnable() {
            int count = delay;

            @Override
            public void run() {
                if (count <= 1) {
                    performTeleport(player, location);
                    pendingTeleports.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }
                count--;
                String tickMsg = plugin.getConfig().getString("messages.teleport-start", "<yellow>Teletransportando en <count> segundos...</yellow>")
                        .replace("<count>", String.valueOf(count));
                plugin.getMessageManager().sendActionBar(player, tickMsg);
            }
        }.runTaskTimer(plugin, 20L, 20L);

        pendingTeleports.put(player.getUniqueId(), task);
    }

    private void performTeleport(Player player, Location location) {
        // Save back location before teleporting
        plugin.getDatabaseManager().saveBackLocation(player.getUniqueId(), player.getLocation());

        player.teleport(location);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
        plugin.getMessageManager().sendMessage(player, plugin.getConfig().getString("messages.teleport-success", "<green>✔ Teletransportado con éxito.</green>"));
    }

    public void cancelPending(Player player) {
        if (pendingTeleports.containsKey(player.getUniqueId())) {
            pendingTeleports.get(player.getUniqueId()).cancel();
            pendingTeleports.remove(player.getUniqueId());
        }
    }

    public boolean isPending(Player player) {
        return pendingTeleports.containsKey(player.getUniqueId());
    }

    public void addTpaRequest(Player requester, Player target) {
        tpaRequests.put(target.getUniqueId(), requester.getUniqueId());
        plugin.getMessageManager().sendMessage(requester, "<yellow>Solicitud enviada a " + target.getName() + ".</yellow>");
        plugin.getMessageManager().sendMessage(target, "<aqua>" + requester.getName() + " <yellow>quiere teletransportarse a ti.</yellow>");
        plugin.getMessageManager().sendMessage(target, "<yellow>Escribe <green>/tpaccept</green> para aceptar o <red>/tpdeny</red> para rechazar.</yellow>");
    }

    public void acceptTpa(Player target) {
        UUID requesterUUID = tpaRequests.remove(target.getUniqueId());
        if (requesterUUID == null) {
            plugin.getMessageManager().sendMessage(target, "<red>No tienes solicitudes pendientes.</red>");
            return;
        }

        Player requester = Bukkit.getPlayer(requesterUUID);
        if (requester == null) {
            plugin.getMessageManager().sendMessage(target, "<red>El jugador ya no está conectado.</red>");
            return;
        }

        plugin.getMessageManager().sendMessage(target, "<green>Has aceptado la solicitud de " + requester.getName() + ".</green>");
        teleport(requester, target.getLocation());
    }

    public void denyTpa(Player target) {
        UUID requesterUUID = tpaRequests.remove(target.getUniqueId());
        if (requesterUUID == null) {
            plugin.getMessageManager().sendMessage(target, "<red>No tienes solicitudes pendientes.</red>");
            return;
        }

        Player requester = Bukkit.getPlayer(requesterUUID);
        if (requester != null) {
            plugin.getMessageManager().sendMessage(requester, "<red>" + target.getName() + " ha rechazado tu solicitud.</red>");
        }
        plugin.getMessageManager().sendMessage(target, "<yellow>Has rechazado la solicitud.</yellow>");
    }
}
