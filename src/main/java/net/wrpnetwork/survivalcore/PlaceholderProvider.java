package net.wrpnetwork.survivalcore;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.wrpnetwork.survivalcore.database.DatabaseManager;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PlaceholderProvider extends PlaceholderExpansion {

    private final SurvivalCore plugin;

    public PlaceholderProvider(SurvivalCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "survivalcore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Wyl & Ari";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        switch (params.toLowerCase()) {
            case "balance":
                return String.format("%.2f", plugin.getEconomyManager().getBalance(player.getUniqueId()));
            case "clan_tag":
                DatabaseManager.ClanData clan = plugin.getDatabaseManager().getClanByMember(player.getUniqueId());
                return clan != null ? clan.tag() : "";
            case "clan_name":
                DatabaseManager.ClanData clanName = plugin.getDatabaseManager().getClanByMember(player.getUniqueId());
                return clanName != null ? clanName.name() : "Sin Clan";
            case "homes_count":
                return String.valueOf(plugin.getDatabaseManager().getHomes(player.getUniqueId()).size());
        }

        return null;
    }
}
