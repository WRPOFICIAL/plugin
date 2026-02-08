package net.wrpnetwork.hubstatus;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.List;

public class ScoreboardManager {

    private final WRPHubStatus plugin;
    private final StatusManager statusManager;

    public ScoreboardManager(WRPHubStatus plugin, StatusManager statusManager) {
        this.plugin = plugin;
        this.statusManager = statusManager;
    }

    public void updateScoreboard(Player player) {
        if (!plugin.getConfig().getBoolean("scoreboard.enabled", true)) {
            if (player.getScoreboard() != Bukkit.getScoreboardManager().getMainScoreboard()) {
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
            return;
        }

        Scoreboard scoreboard = player.getScoreboard();
        if (scoreboard == null || scoreboard == Bukkit.getScoreboardManager().getMainScoreboard()) {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(scoreboard);
        }

        String title = plugin.color(plugin.getConfig().getString("scoreboard.title", "&6&lWRP NETWORK"));
        Objective objective = scoreboard.getObjective("hubstatus");
        if (objective == null) {
            objective = scoreboard.registerNewObjective("hubstatus", "dummy", title);
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        } else {
            objective.setDisplayName(title);
        }

        List<String> lines = plugin.getConfig().getStringList("scoreboard.lines");
        int scoreCount = lines.size();

        for (int i = 0; i < lines.size(); i++) {
            String teamName = "line_" + i;
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
                String entry = getEntryForLine(i);
                team.addEntry(entry);
                objective.getScore(entry).setScore(scoreCount - i);
            }

            String line = lines.get(i);
            String formattedLine = formatLine(line);

            // In 1.13+, teams can have prefix and suffix each up to 64 chars,
            // but for simplicity and compatibility, we use prefix for the whole line if < 64.
            // In 1.20, it's even more flexible.
            team.setPrefix(formattedLine);
        }

        // Remove extra teams if lines decreased
        int i = lines.size();
        while (true) {
            Team team = scoreboard.getTeam("line_" + i);
            if (team == null) break;
            for (String entry : team.getEntries()) {
                scoreboard.resetScores(entry);
            }
            team.unregister();
            i++;
        }
    }

    private String getEntryForLine(int i) {
        return ChatColor.values()[i].toString() + ChatColor.RESET;
    }

    private String formatLine(String line) {
        return plugin.color(line
                .replace("%state%", statusManager.getCurrentState())
                .replace("%state_color%", statusManager.getStateColor())
                .replace("%players%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max_players%", String.valueOf(plugin.getConfig().getInt("server.max-players", 100))));
    }

    public void removeScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }
}
