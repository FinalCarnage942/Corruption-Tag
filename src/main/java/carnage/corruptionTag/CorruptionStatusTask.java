package carnage.corruptionTag;

import carnage.corruptionTag.managers.BossBarManager;
import carnage.corruptionTag.managers.GameManager;
import carnage.corruptionTag.managers.GlowManager;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CorruptionStatusTask extends BukkitRunnable {

    private final Player player;
    private final GameManager gameManager;

    public CorruptionStatusTask(Player player, int startingCorruption) {
        this.player = player;
        this.gameManager = CorruptionTag.getInstance().getGameManager();
        gameManager.corruptionLevels.put(player.getUniqueId(), startingCorruption);
        BossBarManager.createBossBar(player, "Corrupted: " + player.getName() + " " + startingCorruption + "%", startingCorruption / 100.0f);
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cleanup();
            cancel();
            return;
        }

        int corruptionLevel = gameManager.getCorruptionLevel(player);

        if (player.getGameMode() != org.bukkit.GameMode.SURVIVAL) {
            cleanup();
            cancel();
            return;
        }

        BossBarManager.updateBossBar(player, "Corrupted: " + player.getName() + " " + corruptionLevel + "%", corruptionLevel / 100.0f);

        if (corruptionLevel < 100) {
            gameManager.increaseCorruption(player, 1);
        } else {
            cleanup();
            cancel();
        }
    }

    private void cleanup() {
        GlowManager.removeGlow(player);
        BossBarManager.removeBossBar(player);
    }
}
