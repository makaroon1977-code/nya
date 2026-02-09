package dev.yournick.mobarena.listener;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.player.PlayerProfile;
import dev.yournick.mobarena.player.PlayerStats;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

public class EntityListener implements Listener {

    private final MobArenaPlugin plugin;

    public EntityListener(MobArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onKnockback(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player player = (Player) e.getEntity();
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        if (profile == null) return;

        profile.recalculateStats();
        PlayerStats stats = profile.getStats();

        double res = stats.getKnockbackResistance();
        if (res > 0) {
            // В Spigot 1.12.2 нет прямого события KnockbackEvent.
            // Мы можем попробовать уменьшить Velocity в следующем тике.
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (res >= 1.0) {
                    player.setVelocity(new Vector(0, 0, 0));
                } else {
                    Vector v = player.getVelocity();
                    player.setVelocity(v.multiply(1.0 - res));
                }
            }, 1L);
        }
    }
}
