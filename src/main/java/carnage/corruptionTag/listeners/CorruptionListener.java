package carnage.corruptionTag.listeners;
import carnage.corruptionTag.CorruptionTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        if (damagerIsCorrupted && !targetIsCorrupted) {
            // Corrupted hits non-corrupted
            event.setDamage(0);
            double newHealth = target.getHealth() - 2.0;
            if (newHealth <= 0) {
                CorruptionTag.getInstance().getGameManager().handlePlayerCorrupted(target);
            } else {
                target.setHealth(newHealth);
                damager.sendActionBar(Component.text("You hit a non-corrupted player!", NamedTextColor.DARK_PURPLE));
                target.sendActionBar(Component.text("You've been hit by a corrupted player!", NamedTextColor.RED));
            }
        } else if (!damagerIsCorrupted && targetIsCorrupted) {
            // Non-corrupted hits corrupted
            event.setDamage(0);
            double newHealth = target.getHealth() - 1.0;
            if (newHealth <= 0) {
                target.setHealth(1.0); // Keep them alive
                damager.sendActionBar(Component.text("Corrupted cannot die this way!", NamedTextColor.GRAY));
            } else {
                target.setHealth(newHealth);  // Apply damage correctly to the corrupted
                damager.sendActionBar(Component.text("You hit the corrupted player!", NamedTextColor.DARK_GREEN));
                target.sendActionBar(Component.text("You've been hit by a non-corrupted player!", NamedTextColor.DARK_GREEN));
            }
        } else {
            // Both are same corruption state
            event.setCancelled(true);
            damager.sendActionBar(Component.text("You cannot hit this player!", NamedTextColor.GRAY));
        }
    }
}