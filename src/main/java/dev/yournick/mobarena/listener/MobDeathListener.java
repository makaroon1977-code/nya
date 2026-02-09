package dev.yournick.mobarena.listener;

import dev.yournick.mobarena.arena.ArenaManager;
import dev.yournick.mobarena.perk.Perk;
import dev.yournick.mobarena.player.PlayerProfile;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class MobDeathListener implements Listener {

    private final ArenaManager arena;

    public MobDeathListener(ArenaManager arena) {
        this.arena = arena;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (!arena.isArenaMob(entity)) return;

        handleEliteDeath(entity);

        // Отключаем ванильный дроп
        event.getDrops().clear();

        Player killer = entity.getKiller();
        if (killer != null) {
            PlayerProfile profile = arena.getPlugin().getPlayerRepository().getProfile(killer);
            if (profile != null) {
                if (hasEnchant(killer.getInventory().getItemInMainHand(), "Безжалостность")) {
                    profile.setRuthlessnessActive(true);
                }
                if (hasEnchant(killer.getInventory().getItemInMainHand(), "Поглотитель")) {
                    killer.setHealth(killer.getMaxHealth());
                    profile.setDevourerCharges(2);
                }

                // Огнерожденный: Аватар пламени (Взрыв при смерти)
                if (profile.getPlayerPerk().getPerk() == Perk.FIREBORN && profile.isAvatarOfFlameActive()) {
                    entity.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, entity.getLocation(), 1);
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
                    double explosionDamage = entity.getMaxHealth() * 0.25;
                    for (Entity nearby : entity.getNearbyEntities(3, 3, 3)) {
                        if (nearby instanceof LivingEntity && arena.isArenaMob(nearby) && nearby != entity) {
                            ((LivingEntity) nearby).damage(explosionDamage, killer);
                        }
                    }
                }
            }
        }
        arena.onMobDeath(entity, killer);
    }

    private boolean hasEnchant(org.bukkit.inventory.ItemStack item, String enchantName) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) return false;
        for (String line : item.getItemMeta().getLore()) {
            if (line.contains(enchantName)) return true;
        }
        return false;
    }

    private void handleEliteDeath(LivingEntity dead) {
        if (dead.hasMetadata("broodmother_minion")) {
            String motherId = dead.getMetadata("broodmother_minion").get(0).asString();
            dead.getWorld().getLivingEntities().stream()
                    .filter(e -> e.getUniqueId().toString().equals(motherId))
                    .findFirst()
                    .ifPresent(mother -> {
                        double dmg = mother.getMaxHealth() * 0.03;
                        mother.damage(dmg);
                    });
        }

        if (dead.hasMetadata("elite_type")) {
            String id = dead.getUniqueId().toString();
            dead.getWorld().getLivingEntities().stream()
                    .filter(e -> (e.hasMetadata("puppeteer_minion") && e.getMetadata("puppeteer_minion").get(0).asString().equals(id)) ||
                            (e.hasMetadata("broodmother_minion") && e.getMetadata("broodmother_minion").get(0).asString().equals(id)) ||
                            (e.hasMetadata("phantom_clone") && e.getMetadata("phantom_clone").get(0).asString().equals(id)))
                    .forEach(Entity::remove);
        }
    }
}
