package net.wrpnetwork.survivalcore.ui;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.wrpnetwork.survivalcore.SurvivalCore;
import net.wrpnetwork.survivalcore.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private final SurvivalCore plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public ScoreboardManager(SurvivalCore plugin) {
        this.plugin = plugin;
        startUpdateTask();
    }

    public void setup(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("survival", "dummy", plugin.getMessageManager().parse("<gradient:#55ff55:#aaffaa><b>SurvivalCore+</b></gradient>"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        boards.put(player.getUniqueId(), board);
        player.setScoreboard(board);
        update(player);
    }

    public void update(Player player) {
        Scoreboard board = boards.get(player.getUniqueId());
        if (board == null) return;
        Objective obj = board.getObjective("survival");

        // Remove old scores
        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        DatabaseManager.ClanData clan = plugin.getDatabaseManager().getClanByMember(player.getUniqueId());
        String clanName = clan != null ? clan.tag() : "Ninguno";
        double bal = plugin.getEconomyManager().getBalance(player.getUniqueId());

        LegacyComponentSerializer lcs = LegacyComponentSerializer.legacySection();

        int i = 10;
        obj.getScore(" ").setScore(i--);
        obj.getScore(lcs.serialize(plugin.getMessageManager().parse("<grey>Jugador:</grey> <green>" + player.getName() + "</green>"))).setScore(i--);
        obj.getScore(lcs.serialize(plugin.getMessageManager().parse("<grey>Dinero:</grey> <gold>$" + String.format("%.2f", bal) + "</gold>"))).setScore(i--);
        obj.getScore(lcs.serialize(plugin.getMessageManager().parse("<grey>Clan:</grey> <aqua>" + clanName + "</aqua>"))).setScore(i--);
        obj.getScore("  ").setScore(i--);
        obj.getScore(lcs.serialize(plugin.getMessageManager().parse("<grey>Online:</grey> <white>" + Bukkit.getOnlinePlayers().size() + "</white>"))).setScore(i--);
        obj.getScore("   ").setScore(i--);
        obj.getScore(lcs.serialize(plugin.getMessageManager().parse("<gradient:#aaffaa:#55ff55>wrpnetwork.net</gradient>"))).setScore(i--);

        // Tablist
        player.sendPlayerListHeaderAndFooter(
                plugin.getMessageManager().parse("\n<gradient:#55ff55:#aaffaa><b>WRP NETWORK</b></gradient>\n"),
                plugin.getMessageManager().parse("\n<grey>¡Disfruta de tu aventura!</grey>\n<green>play.wrpnetwork.net</green>\n")
        );
    }

    private void startUpdateTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                update(player);
            }
        }, 100L, 100L);
    }

    public void remove(Player player) {
        boards.remove(player.getUniqueId());
    }
}
