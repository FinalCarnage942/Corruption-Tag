package carnage.corruptionTag.managers;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarManager {
    private static final Map<UUID, BossBar> bossBars = new HashMap<>();

    public static void createBossBar(Player corrupted, String title, float progress) {
        removeBossBar(corrupted); // Remove existing boss bar for this player if any
        BossBar bossBar = BossBar.bossBar(Component.text(title), progress, BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        for (Player player : Bukkit.getOnlinePlayers()) {
            bossBar.addViewer(player);
        }
        bossBars.put(corrupted.getUniqueId(), bossBar);
    }

    public static void updateBossBar(Player player, String title, float progress) {
        BossBar bossBar = bossBars.get(player.getUniqueId());
        if (bossBar != null) {
            bossBar.name(Component.text(title));
            bossBar.progress(progress);
        }
    }

    public static void removeBossBar(Player player) {
        BossBar bossBar = bossBars.remove(player.getUniqueId());
        if (bossBar != null) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                bossBar.removeViewer(online);
            }
        }
    }

    public static void removeAllBossBars() {
        for (BossBar bossBar : bossBars.values()) {
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                bossBar.removeViewer(viewer);
            }
        }
        bossBars.clear();
    }
}
