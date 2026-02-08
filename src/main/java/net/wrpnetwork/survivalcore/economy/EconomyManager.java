package net.wrpnetwork.survivalcore.economy;

import net.wrpnetwork.survivalcore.SurvivalCore;
import org.bukkit.entity.Player;

import java.util.UUID;

public class EconomyManager {

    private final SurvivalCore plugin;

    public EconomyManager(SurvivalCore plugin) {
        this.plugin = plugin;
    }

    public double getBalance(UUID uuid) {
        return plugin.getDatabaseManager().getBalance(uuid);
    }

    public void setBalance(UUID uuid, double amount) {
        plugin.getDatabaseManager().setBalance(uuid, Math.max(0, amount));
    }

    public void addBalance(UUID uuid, double amount) {
        setBalance(uuid, getBalance(uuid) + amount);
    }

    public boolean withdraw(UUID uuid, double amount) {
        double current = getBalance(uuid);
        if (current < amount) return false;
        setBalance(uuid, current - amount);
        return true;
    }

    public void sendBalanceMessage(Player player) {
        double balance = getBalance(player.getUniqueId());
        plugin.getMessageManager().sendMessage(player, "<grey>Tu saldo actual es: <green>$" + String.format("%.2f", balance) + "</green></grey>");
    }
}
