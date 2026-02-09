package dev.yournick.mobarena.task;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.amulet.AmuletType;
import dev.yournick.mobarena.player.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AmuletEffectTask extends BukkitRunnable {

    private final MobArenaPlugin plugin;

    public AmuletEffectTask(MobArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
            if (profile == null) continue;

            if (profile.hasAmuletEquipped(AmuletType.ETERNAL_FIRE)) {
                applyEternalFireAura(player, profile);
            }
        }
    }

    private void applyEternalFireAura(Player player, PlayerProfile profile) {
        // Визуальный эффект ауры
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 0.5, 0), 10, 1.5, 0.5, 1.5, 0.05);

        // Урон врагам
        for (Entity entity : player.getNearbyEntities(3, 3, 3)) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                LivingEntity target = (LivingEntity) entity;
                target.damage(2.0, player);
                target.setFireTicks(40); // 2 секунды горения
            }
        }
    }
}
