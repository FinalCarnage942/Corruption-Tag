package carnage.corruptionTag.listeners;

import carnage.corruptionTag.CorruptionTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CorruptionListener implements Listener {

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager) || !(event.getEntity() instanceof Player target)) {
            return;
        }

        boolean damagerIsCorrupted = damager.isGlowing();
        boolean targetIsCorrupted = target.isGlowing();

        event.setCancelled(true);

        if (damagerIsCorrupted && !targetIsCorrupted) {
            CorruptionTag.getInstance().getGameManager().reduceHitsLeft(target);
            damager.sendActionBar(Component.text("You hit a non-corrupted player!", TextColor.color(0xAA00AA)));
            target.sendActionBar(Component.text("You've been hit by a corrupted player!", TextColor.color(0xFF5555)));
        } else if (!damagerIsCorrupted && targetIsCorrupted) {
            double newHealth = Math.max(0, target.getHealth() - 1.0);
            target.setHealth(newHealth);
            damager.sendActionBar(Component.text("You hit the corrupted player!", TextColor.color(0x00AA00)));
            target.sendActionBar(Component.text("You've been hit by a non-corrupted player!", TextColor.color(0x00AA00)));
        } else if (damagerIsCorrupted && targetIsCorrupted) {
            double newHealth = Math.max(0, target.getHealth() - 1.0);
            target.setHealth(newHealth);
            damager.sendActionBar(Component.text("You hit another corrupted player!", TextColor.color(0x00AA00)));
            target.sendActionBar(Component.text("You've been hit by a corrupted player!", TextColor.color(0x00AA00)));
        } else {
            damager.sendActionBar(Component.text("You cannot hit this player!", TextColor.color(0xAAAAAA)));
        }
    }
}
