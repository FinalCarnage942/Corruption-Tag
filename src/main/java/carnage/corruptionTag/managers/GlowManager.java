package carnage.corruptionTag.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class GlowManager {

    public static void setGlow(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam("corrupted");

        if (team == null) {
            team = scoreboard.registerNewTeam("corrupted");
            team.setColor(org.bukkit.ChatColor.RED);
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
        }

        team.addEntry(player.getName());
        player.setGlowing(true);
    }

    public static void removeGlow(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam("corrupted");

        if (team != null) {
            team.removeEntry(player.getName());
        }

        player.setGlowing(false);
    }
}
