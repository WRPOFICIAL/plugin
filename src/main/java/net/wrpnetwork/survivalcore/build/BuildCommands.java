package net.wrpnetwork.survivalcore.build;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BuildCommands implements CommandExecutor {

    private final SurvivalCore plugin;
    private final Set<UUID> flyingPlayers = new HashSet<>();
    private final Set<UUID> buildModePlayers = new HashSet<>();

    public BuildCommands(SurvivalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }

        switch (command.getName().toLowerCase()) {
            case "fly":
                if (!player.hasPermission("survivalcore.fly")) {
                    plugin.getMessageManager().sendMessage(player, plugin.getConfig().getString("messages.no-permission"));
                    return true;
                }
                toggleFly(player);
                break;
            case "repair":
                if (!player.hasPermission("survivalcore.repair")) {
                    plugin.getMessageManager().sendMessage(player, plugin.getConfig().getString("messages.no-permission"));
                    return true;
                }
                repairItem(player);
                break;
            case "buildmode":
                if (!player.hasPermission("survivalcore.buildmode")) {
                    plugin.getMessageManager().sendMessage(player, plugin.getConfig().getString("messages.no-permission"));
                    return true;
                }
                toggleBuildMode(player);
                break;
        }

        return true;
    }

    private void toggleFly(Player player) {
        if (player.getAllowFlight()) {
            player.setAllowFlight(false);
            player.setFlying(false);
            flyingPlayers.remove(player.getUniqueId());
            plugin.getMessageManager().sendMessage(player, "<yellow>Modo vuelo desactivado.</yellow>");
        } else {
            player.setAllowFlight(true);
            flyingPlayers.add(player.getUniqueId());
            plugin.getMessageManager().sendMessage(player, "<green>Modo vuelo activado.</green>");
        }
    }

    private void repairItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            plugin.getMessageManager().sendMessage(player, "<red>No tienes ningún objeto en la mano.</red>");
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable damageable) {
            if (damageable.getDamage() == 0) {
                plugin.getMessageManager().sendMessage(player, "<yellow>El objeto ya está reparado.</yellow>");
                return;
            }
            damageable.setDamage(0);
            item.setItemMeta(meta);
            plugin.getMessageManager().sendMessage(player, "<green>✔ Objeto reparado correctamente.</green>");
        } else {
            plugin.getMessageManager().sendMessage(player, "<red>Este objeto no se puede reparar.</red>");
        }
    }

    private void toggleBuildMode(Player player) {
        if (buildModePlayers.contains(player.getUniqueId())) {
            buildModePlayers.remove(player.getUniqueId());
            player.setAllowFlight(false);
            player.setFlying(false);
            plugin.getMessageManager().sendMessage(player, "<yellow>BuildMode desactivado.</yellow>");
        } else {
            buildModePlayers.add(player.getUniqueId());
            player.setAllowFlight(true);
            plugin.getMessageManager().sendMessage(player, "<green>BuildMode activado (Fly + GodMode).</green>");
        }
    }

    public Set<UUID> getBuildModePlayers() {
        return buildModePlayers;
    }
}
