package carnage.corruptionTag;

import carnage.corruptionTag.commands.CorruptionCommand;
import carnage.corruptionTag.commands.CorruptionTabCompleter;
import carnage.corruptionTag.listeners.CorruptionListener;
import carnage.corruptionTag.listeners.GameListener;
import carnage.corruptionTag.listeners.MapSelectionListener;
import carnage.corruptionTag.managers.GameManager;
import carnage.corruptionTag.managers.GlowManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class CorruptionTag extends JavaPlugin {

    private static CorruptionTag instance;
    public GameManager gameManager;
    private final Map<UUID, CorruptionStatusTask> corruptionTasks = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;
        this.gameManager = new GameManager();

        CorruptionCommand command = new CorruptionCommand(gameManager);
        CorruptionTabCompleter tabCompleter = new CorruptionTabCompleter();

        getServer().getPluginManager().registerEvents(new CorruptionListener(), this);
        getServer().getPluginManager().registerEvents(new GameListener(gameManager), this);
        getServer().getPluginManager().registerEvents(new MapSelectionListener(gameManager), this);

        getCommand("corruptiontag").setExecutor(command);
        getCommand("ct").setExecutor(command);
        getCommand("corruptiontag").setTabCompleter(tabCompleter);
        getCommand("ct").setTabCompleter(tabCompleter);

        getLogger().info("Corruption Tag has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Corruption Tag has been disabled!");
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public static CorruptionTag getInstance() {
        return instance;
    }

    public void corruptPlayer(Player player, int startCorruptionPercent) {
        CorruptionStatusTask existing = corruptionTasks.get(player.getUniqueId());

        if (existing != null) {
            existing.cancel();
        }

        CorruptionStatusTask task = new CorruptionStatusTask(player, startCorruptionPercent);
        corruptionTasks.put(player.getUniqueId(), task);
        GlowManager.setGlow(player);
        task.runTaskTimer(this, 0L, 20L);
    }

    public void stopCorruptionTask(Player player) {
        CorruptionStatusTask task = corruptionTasks.remove(player.getUniqueId());

        if (task != null) {
            task.cancel();
            GlowManager.removeGlow(player);
        }
    }
}
