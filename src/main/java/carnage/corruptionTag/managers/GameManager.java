package carnage.corruptionTag.managers;

import carnage.corruptionTag.CorruptionTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GameManager {

    public static final int MAX_PLAYERS = 5;
    private final Map<UUID, Set<UUID>> games = new HashMap<>();
    private final Map<UUID, World> activeGameWorlds = new HashMap<>();
    private final Map<UUID, Location> originalLocations = new HashMap<>();
    private final Map<UUID, Location> spawnPoints = new HashMap<>();
    public final Map<UUID, Integer> corruptionLevels = new HashMap<>();
    private final Map<UUID, Integer> hitsLeft = new HashMap<>();
    private final Map<UUID, UUID> playerToHost = new HashMap<>();
    private UUID currentCorruptedPlayer = null;

    public boolean createGame(Player host) {
        if (games.containsKey(host.getUniqueId())) return false;
        Set<UUID> lobby = new HashSet<>();
        lobby.add(host.getUniqueId());
        games.put(host.getUniqueId(), lobby);
        originalLocations.put(host.getUniqueId(), host.getLocation());
        playerToHost.put(host.getUniqueId(), host.getUniqueId());
        return true;
    }

    public boolean joinGame(Player host, Player player) {
        Set<UUID> lobby = games.get(host.getUniqueId());
        if (lobby == null || lobby.contains(player.getUniqueId()) || lobby.size() >= MAX_PLAYERS) return false;
        lobby.add(player.getUniqueId());
        originalLocations.put(player.getUniqueId(), player.getLocation());
        playerToHost.put(player.getUniqueId(), host.getUniqueId());
        Bukkit.broadcast(Component.text(player.getName() + " has joined the lobby (" + lobby.size() + "/" + MAX_PLAYERS + ")", TextColor.color(0x55FFFF)));
        if (lobby.size() >= MAX_PLAYERS) {
            startGame(host);
        }
        return true;
    }

    public void startGame(Player host) {
        Set<UUID> uuids = games.get(host.getUniqueId());
        if (uuids == null || uuids.isEmpty()) return;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy_HH-mm-ss");
        String timestamp = LocalDateTime.now().format(formatter);
        String worldName = "ct_game_" + timestamp;
        World gameWorld = WorldManager.createGameWorld(worldName);
        activeGameWorlds.put(host.getUniqueId(), gameWorld);
        gameWorld.setGameRule(GameRule.NATURAL_REGENERATION, false);
        gameWorld.setDifficulty(Difficulty.PEACEFUL);
        WorldManager.buildStonePlatform(gameWorld, 0, 64, 0);
        Location spawn = new Location(gameWorld, 0.5, 65, 0.5);
        List<Player> players = new ArrayList<>();
        for (UUID u : uuids) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && p.isOnline()) {
                p.getAttribute(Attribute.MAX_HEALTH).setBaseValue(8.0);
                p.setHealth(8.0);
                p.teleport(spawn);
                p.setGameMode(GameMode.SURVIVAL);
                p.sendMessage(Component.text("The game has started!", TextColor.color(0xFF55FF)));
                players.add(p);
                corruptionLevels.put(u, 0);
                hitsLeft.put(u, 4); // Initialize hits left
                spawnPoints.put(u, spawn);
            }
        }
        if (players.isEmpty()) return;
        Player corrupted = players.get(new Random().nextInt(players.size()));
        corruptPlayer(corrupted);
        saveGame(host);
        Bukkit.getScheduler().runTaskTimer(CorruptionTag.getInstance(), task -> {
            int aliveCount = 0;
            for (Player p : players) {
                if (p.isOnline() && p.getGameMode() == GameMode.SURVIVAL) {
                    aliveCount++;
                }
            }
            if (aliveCount <= 1) {
                endGame(host);
                task.cancel();
            }
        }, 20L, 60L);
    }

    public void increaseCorruption(Player p, int amount) {
        UUID u = p.getUniqueId();
        int newLevel = Math.min(100, corruptionLevels.getOrDefault(u, 0) + amount);
        corruptionLevels.put(u, newLevel);
        if (newLevel >= 100) {
            handleDeath(p);
        }
    }

    public void reduceHitsLeft(Player player) {
        UUID uuid = player.getUniqueId();
        if (!hitsLeft.containsKey(uuid)) return;
        int hits = hitsLeft.get(uuid) - 1;
        hitsLeft.put(uuid, hits);
        if (hits <= 0) {
            handlePlayerCorrupted(player);
        }
    }

    public void handleDeath(Player p) {
        UUID u = p.getUniqueId();
        UUID hostU = playerToHost.get(u);
        if (hostU == null) return;
        Set<UUID> uuids = games.get(hostU);
        if (uuids == null) return;
        List<UUID> remainingPlayers = new ArrayList<>(uuids);
        remainingPlayers.remove(u);
        boolean wasTwo = remainingPlayers.size() == 1;
        CorruptionTag.getInstance().stopCorruptionTask(p);
        p.setGameMode(GameMode.SPECTATOR);
        corruptionLevels.remove(u);
        Bukkit.broadcast(Component.text(p.getName() + " died of corruption!", TextColor.color(0xFF5555)));
        if (u.equals(currentCorruptedPlayer)) {
            BossBarManager.removeBossBar(p);
            currentCorruptedPlayer = null;
        }
        if (wasTwo) {
            Player other = Bukkit.getPlayer(remainingPlayers.get(0));
            if (other != null && other.isOnline()) {
                endGame(Bukkit.getPlayer(hostU));
            }
            return;
        }
        if (!remainingPlayers.isEmpty()) {
            UUID next = remainingPlayers.stream()
                    .min(Comparator.comparingInt(x -> corruptionLevels.getOrDefault(x, 0)))
                    .orElse(null);
            if (next != null) {
                Player nextPlayer = Bukkit.getPlayer(next);
                if (nextPlayer != null && nextPlayer.isOnline()) {
                    corruptPlayer(nextPlayer);
                }
            }
        }
        uuids.remove(u);
    }

    public void corruptPlayer(Player p) {
        UUID newCorruptedUUID = p.getUniqueId();
        if (currentCorruptedPlayer != null && !currentCorruptedPlayer.equals(newCorruptedUUID)) {
            Player oldCorrupted = Bukkit.getPlayer(currentCorruptedPlayer);
            if (oldCorrupted != null && oldCorrupted.isOnline()) {
                CorruptionTag.getInstance().stopCorruptionTask(oldCorrupted);
                BossBarManager.removeBossBar(oldCorrupted);
                oldCorrupted.getAttribute(Attribute.MAX_HEALTH).setBaseValue(8.0);
                oldCorrupted.setHealth(8.0);
            }
        }
        currentCorruptedPlayer = newCorruptedUUID;
        int currentCorruption = corruptionLevels.getOrDefault(newCorruptedUUID, 0);
        corruptionLevels.put(newCorruptedUUID, currentCorruption);
        p.getAttribute(Attribute.MAX_HEALTH).setBaseValue(8.0);
        p.setHealth(8.0);
        CorruptionTag.getInstance().corruptPlayer(p, currentCorruption);
    }

    public void endGame(Player host) {
        Set<UUID> uuids = games.get(host.getUniqueId());
        if (uuids != null) {
            for (UUID u : new HashSet<>(uuids)) {
                Player p = Bukkit.getPlayer(u);
                if (p != null && p.isOnline()) {
                    p.teleport(originalLocations.getOrDefault(u, p.getWorld().getSpawnLocation()));
                    p.setGameMode(GameMode.SURVIVAL);
                    p.getAttribute(Attribute.MAX_HEALTH).setBaseValue(20.0);
                    p.setHealth(20.0);
                    CorruptionTag.getInstance().stopCorruptionTask(p);
                    GlowManager.removeGlow(p);
                    BossBarManager.removeBossBar(p);
                }
            }
            Optional<Player> winnerOpt = uuids.stream()
                    .map(Bukkit::getPlayer)
                    .filter(p -> p != null && p.isOnline() && p.getGameMode() == GameMode.SURVIVAL)
                    .findFirst();
            saveFinalGame(host, winnerOpt.orElse(null));
            uuids.clear();
            World w = activeGameWorlds.remove(host.getUniqueId());
            if (w != null) WorldManager.deleteWorld(w);
            games.remove(host.getUniqueId());
            corruptionLevels.clear();
            spawnPoints.clear();
            playerToHost.clear();
            currentCorruptedPlayer = null;
            BossBarManager.removeAllBossBars();
            Bukkit.broadcast(Component.text("Game over!", TextColor.color(0xFFAA00)));
            if (winnerOpt.isPresent()) {
                Bukkit.broadcast(Component.text(winnerOpt.get().getName() + " wins!", TextColor.color(0x55FF55)));
            } else {
                Bukkit.broadcast(Component.text("No winner.", TextColor.color(0xFF5555)));
            }
        }
    }

    public int getCorruptionLevel(Player p) {
        return corruptionLevels.getOrDefault(p.getUniqueId(), 0);
    }

    public boolean isGameHost(Player player) {
        return games.containsKey(player.getUniqueId());
    }

    public Set<UUID> getAllHosts() {
        return games.keySet();
    }

    public int getLobbySize(Player host) {
        Set<UUID> lobby = games.get(host.getUniqueId());
        return lobby == null ? 0 : lobby.size();
    }

    public boolean isInLobby(Player host, Player player) {
        Set<UUID> lobby = games.get(host.getUniqueId());
        return lobby != null && lobby.contains(player.getUniqueId());
    }

    public void handlePlayerCorrupted(Player player) {
        corruptPlayer(player);
        Bukkit.broadcast(Component.text(player.getName() + " has been corrupted!", TextColor.color(0xFF5555)));
    }

    public boolean isInGame(Player player) {
        UUID uuid = player.getUniqueId();
        for (Set<UUID> lobby : games.values()) {
            if (lobby.contains(uuid)) return true;
        }
        return false;
    }

    private File getSaveFolder() {
        File folder = new File(CorruptionTag.getInstance().getDataFolder(), "saved_games");
        if (!folder.exists()) folder.mkdirs();
        return folder;
    }

    public void saveGame(Player host) {
        Set<UUID> uuids = games.get(host.getUniqueId());
        if (uuids == null || uuids.isEmpty()) return;
        File saveFolder = getSaveFolder();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "game_" + host.getUniqueId().toString() + "_" + timestamp + ".yml";
        File saveFile = new File(saveFolder, fileName);
        FileConfiguration config = YamlConfiguration.loadConfiguration(saveFile);
        config.set("host", host.getUniqueId().toString());
        config.set("host_name", host.getName());
        List<String> playerUUIDs = new ArrayList<>();
        List<String> playerNames = new ArrayList<>();
        for (UUID u : uuids) {
            playerUUIDs.add(u.toString());
            Player p = Bukkit.getPlayer(u);
            playerNames.add(p != null ? p.getName() : "Offline");
        }
        config.set("players_uuids", playerUUIDs);
        config.set("players_names", playerNames);
        Map<String, Integer> corruptionMap = new HashMap<>();
        for (UUID u : uuids) {
            corruptionMap.put(u.toString(), corruptionLevels.getOrDefault(u, 0));
        }
        config.set("corruption_levels", corruptionMap);
        if (currentCorruptedPlayer != null) {
            config.set("current_corrupted_player", currentCorruptedPlayer.toString());
        } else {
            config.set("current_corrupted_player", null);
        }
        config.set("timestamp", LocalDateTime.now().toString());
        try {
            config.save(saveFile);
            Bukkit.getLogger().info("Saved game for host " + host.getName() + " to " + saveFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveFinalGame(Player host, Player winner) {
        Set<UUID> uuids = games.get(host.getUniqueId());
        if (uuids == null) return;
        File saveFolder = getSaveFolder();
        String fileName = "game_" + host.getUniqueId().toString() + ".yml";
        File saveFile = new File(saveFolder, fileName);
        FileConfiguration config = YamlConfiguration.loadConfiguration(saveFile);
        if (winner != null) {
            config.set("winner.uuid", winner.getUniqueId().toString());
            config.set("winner.name", winner.getName());
        } else {
            config.set("winner", null);
        }
        Map<String, Integer> finalCorruption = new HashMap<>();
        for (UUID u : uuids) {
            finalCorruption.put(u.toString(), corruptionLevels.getOrDefault(u, 0));
        }
        config.set("corruption_levels", finalCorruption);
        try {
            config.save(saveFile);
            Bukkit.getLogger().info("Saved final game results for host " + host.getName() + " to " + saveFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
