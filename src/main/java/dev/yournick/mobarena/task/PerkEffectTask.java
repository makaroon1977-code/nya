package dev.yournick.mobarena.task;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.arena.ArenaManager;
import dev.yournick.mobarena.perk.Perk;
import dev.yournick.mobarena.player.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class PerkEffectTask extends BukkitRunnable {

    private final MobArenaPlugin plugin;
    private int tickCounter = 0;
    private final java.util.Map<java.util.UUID, org.bukkit.Location> lastLocations = new java.util.HashMap<>();

    public PerkEffectTask(MobArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        tickCounter++;
        ArenaManager arena = plugin.getArenaManager();

        // 1. Очистка и визуализация огненных следов (каждые 5 тиков)
        long now = System.currentTimeMillis();
        arena.getFireTrails().entrySet().removeIf(entry -> now > entry.getValue());

        // Создаем копию ключей для безопасной итерации
        for (Location loc : new java.util.HashSet<>(arena.getFireTrails().keySet())) {
            loc.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0.5, 0.1, 0.5), 3, 0.2, 0.1, 0.2, 0.02);
        }

        // 2. Урон от огненных следов (каждую секунду - 20 тиков)
        if (tickCounter % 4 == 0) {
            // Создаем копию списка мобов, так как damage() может вызвать смерть и удаление из arenaMobs
            for (LivingEntity mob : new java.util.ArrayList<>(arena.getArenaMobs())) {
                if (!mob.isValid()) {
                    arena.getArenaMobs().remove(mob);
                    continue;
                }

                if (arena.getFireTrails().containsKey(mob.getLocation().getBlock().getLocation())) {
                    mob.setFireTicks(40);
                    mob.damage(3.0);
                }
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
            if (profile == null || profile.getPlayerPerk() == null) continue;

            Perk perk = profile.getPlayerPerk().getPerk();
            if (perk == null) continue;

            // Каждую секунду
            if (tickCounter % 4 == 0) {
                // 🛡 Стойкий: Крепость (Regen I при неподвижности)
                if (perk == Perk.TOUGH && profile.getPerkProfile(Perk.TOUGH).hasLarge2()) {
                    Location last = lastLocations.get(player.getUniqueId());
                    boolean standing = last != null && 
                                     last.getWorld().equals(player.getWorld()) && 
                                     last.distanceSquared(player.getLocation()) < 0.001;
                    
                    if (standing) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 40, 0, false, false));
                    }
                }
                lastLocations.put(player.getUniqueId(), player.getLocation());

                // 🏃 Ловкий: Медленный враг (Крупный 2)
                if (perk == Perk.AGILE && profile.getPerkProfile(Perk.AGILE).hasLarge2()) {
                    for (Entity e : player.getNearbyEntities(3, 3, 3)) {
                        if (e instanceof LivingEntity && plugin.getArenaManager().isArenaMob(e)) {
                            ((LivingEntity) e).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 40, 0));
                        }
                    }
                }
            }
        }
    }
}
