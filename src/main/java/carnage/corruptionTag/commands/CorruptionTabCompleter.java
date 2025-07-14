package carnage.corruptionTag.commands;

import carnage.corruptionTag.CorruptionTag;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CorruptionTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("start");
            suggestions.add("join");
            suggestions.add("status");
            suggestions.add("create");
            suggestions.add("endgame");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            String partial = args[1].toLowerCase();
            for (UUID hostId : CorruptionTag.getInstance().getGameManager().getAllHosts()) {
                Player host = Bukkit.getPlayer(hostId);
                if (host != null && host.getName().toLowerCase().startsWith(partial)) {
                    suggestions.add(host.getName());
                }
            }
        }

        return suggestions;
    }
}
