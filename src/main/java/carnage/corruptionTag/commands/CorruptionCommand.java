package carnage.corruptionTag.commands;

import carnage.corruptionTag.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CorruptionCommand implements CommandExecutor {

    private final GameManager gameManager;

    public CorruptionCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Only players can run this command.", TextColor.color(0xFF5555)));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: /" + label + " <create|start|join|status|endgame>", TextColor.color(0xFF5555)));
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "create":
                handleCreateCommand(player);
                break;
            case "start":
                handleStartCommand(player);
                break;
            case "join":
                handleJoinCommand(player, args);
                break;
            case "status":
                handleStatusCommand(player);
                break;
            case "endgame":
                handleEndGameCommand(player);
                break;
            default:
                player.sendMessage(Component.text("Unknown subcommand.", TextColor.color(0xFF5555)));
        }

        return true;
    }

    private void handleCreateCommand(Player player) {
        if (gameManager.isGameHost(player)) {
            int size = gameManager.getLobbySize(player);
            player.sendMessage(Component.text("You already have a lobby with " + size + " players.", TextColor.color(0xFFAA00)));
        } else {
            if (gameManager.createGame(player)) {
                player.sendMessage(Component.text("Lobby created! (Players: 1/" + GameManager.MAX_PLAYERS + ")", TextColor.color(0x55FF55)));
            } else {
                player.sendMessage(Component.text("Failed to create lobby.", TextColor.color(0xFF5555)));
            }
        }
    }

    private void handleStartCommand(Player player) {
        if (!gameManager.isGameHost(player)) {
            player.sendMessage(Component.text("You are not hosting a lobby.", TextColor.color(0xFF5555)));
            return;
        }

        int currentPlayers = gameManager.getLobbySize(player);
        if (currentPlayers < 2) {
            player.sendMessage(Component.text("You need at least 2 players to start the game.", TextColor.color(0xFF5555)));
            return;
        }

        gameManager.startGame(player);
    }

    private void handleJoinCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /ct join <hostPlayerName>", TextColor.color(0xFF5555)));
            return;
        }

        Player host = Bukkit.getPlayerExact(args[1]);
        if (host == null || !host.isOnline()) {
            player.sendMessage(Component.text("Host player not found.", TextColor.color(0xFF5555)));
            return;
        }

        if (gameManager.isInLobby(host, player)) {
            player.sendMessage(Component.text("You are already in this lobby.", TextColor.color(0xFFAA00)));
            return;
        }

        if (gameManager.joinGame(host, player)) {
            int current = gameManager.getLobbySize(host);
            player.sendMessage(Component.text("You joined " + host.getName() + "'s game! (Players: " + current + "/" + GameManager.MAX_PLAYERS + ")", TextColor.color(0x55FF55)));
        } else {
            player.sendMessage(Component.text("Could not join the game. Lobby full or unavailable.", TextColor.color(0xFF5555)));
        }
    }

    private void handleStatusCommand(Player player) {
        if (gameManager.isGameHost(player)) {
            int size = gameManager.getLobbySize(player);
            player.sendMessage(Component.text("You are hosting a game with " + size + "/" + GameManager.MAX_PLAYERS + " players.", TextColor.color(0x55FF55)));
        } else {
            player.sendMessage(Component.text("You are not hosting any game.", TextColor.color(0xFFAA00)));
        }
    }

    private void handleEndGameCommand(Player player) {
        if (!gameManager.isGameHost(player)) {
            player.sendMessage(Component.text("You are not hosting a game.", TextColor.color(0xFF5555)));
            return;
        }

        gameManager.endGame(player);
    }
}
