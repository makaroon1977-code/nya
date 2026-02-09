package dev.yournick.mobarena.task;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.arena.ArenaManager;
import dev.yournick.mobarena.mob.MobStats;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class MobAbilityTask extends BukkitRunnable {

    private final MobArenaPlugin plugin;
    private final ArenaManager arena;
    
    private final Map<UUID, Long> skeletonLastShoot = new HashMap<>();
    private final Map<UUID, Long> spiderLastTeleport = new HashMap<>();
    private final Map<UUID, Long> lastEliteAbility = new HashMap<>();
    private final Map<UUID, Integer> sniperAimTicks = new HashMap<>();
    private final Map<UUID, Integer> sniperArrowCount = new HashMap<>();

    public MobAbilityTask(MobArenaPlugin plugin) {
        this.plugin = plugin;
        this.arena = plugin.getArenaManager();
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        
        for (LivingEntity mob : new ArrayList<>(arena.getArenaMobs())) {
            if (mob == null || mob.isDead() || !mob.isValid()) continue;
            
            Player target = getNearestPlayer(mob);
            if (target == null) continue;

            if (mob.hasMetadata("elite_type")) {
                handleEliteAbilities(mob, target, now);
            }

            double dist = mob.getLocation().distance(target.getLocation());

            // Логика Скелета
            if (mob.getType() == EntityType.SKELETON) {
                // 1. Выстрел каждые 10 секунд
                if (now - skeletonLastShoot.getOrDefault(mob.getUniqueId(), 0L) > 10000) {
                    skeletonLastShoot.put(mob.getUniqueId(), now);
                    shootHomingArrow(mob, target);
                }
                
                // 2. Отход от игрока, если он слишком близко (ближе 5 блоков)
                if (dist < 5) {
                    moveAway(mob, target);
                }
            }
            
            // Логика Паука
            if (mob.getType() == EntityType.SPIDER) {
                // Телепорт за спину каждые 50 секунд
                if (now - spiderLastTeleport.getOrDefault(mob.getUniqueId(), 0L) > 50000) {
                    spiderLastTeleport.put(mob.getUniqueId(), now);
                    teleportBehind(mob, target);
                }
            }
        }
    }

    private Player getNearestPlayer(Entity entity) {
        Player nearest = null;
        double dist = Double.MAX_VALUE;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().equals(entity.getWorld()) && p.getGameMode() == org.bukkit.GameMode.SURVIVAL) {
                double d = p.getLocation().distanceSquared(entity.getLocation());
                if (d < dist) {
                    dist = d;
                    nearest = p;
                }
            }
        }
        return nearest;
    }

    private void shootHomingArrow(LivingEntity skeleton, Player target) {
        Location start = skeleton.getEyeLocation();
        // Спавним стрелу, летящую в сторону игрока
        Arrow arrow = skeleton.getWorld().spawnArrow(start, target.getEyeLocation().toVector().subtract(start.toVector()).normalize(), 1.6f, 0);
        arrow.setShooter(skeleton);
        
        // Самонаводящаяся логика
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (arrow.isDead() || arrow.isOnGround() || ticks > 40 || target.isDead() || !target.isOnline()) {
                    cancel();
                    return;
                }
                
                Vector dir = target.getEyeLocation().toVector().subtract(arrow.getLocation().toVector()).normalize();
                arrow.setVelocity(dir.multiply(1.2));
                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void moveAway(LivingEntity mob, Player target) {
        // Вычисляем вектор "от игрока"
        Vector away = mob.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(0.25);
        away.setY(0); // Не даем мобу взлетать
        mob.setVelocity(mob.getVelocity().add(away));
    }

    private void teleportBehind(LivingEntity spider, Player target) {
        Vector direction = target.getLocation().getDirection().normalize();
        Location behind = target.getLocation().subtract(direction.multiply(2));
        
        // Простая проверка на препятствия (не телепортироваться в блок)
        if (behind.getBlock().getType().isSolid()) {
            behind = target.getLocation(); 
        }
        
        behind.setPitch(target.getLocation().getPitch());
        behind.setYaw(target.getLocation().getYaw());
        
        spider.teleport(behind);
        spider.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, spider.getLocation(), 20);
        target.sendMessage("§c§l(!) §cПаук телепортировался вам за спину!");
    }

    private void handleEliteAbilities(LivingEntity mob, Player target, long now) {
        String typeStr = mob.getMetadata("elite_type").get(0).asString();
        long last = lastEliteAbility.getOrDefault(mob.getUniqueId(), 0L);

        switch (typeStr) {
            case "BLOOD_BULLY":
                if (now - last > 15000) {
                    lastEliteAbility.put(mob.getUniqueId(), now);
                    if (Math.random() < 0.3) pullPlayer(mob, target);
                }
                break;
            case "BONE_SNIPER":
                handleSniper(mob, target, now);
                break;
            case "PUPPETEER":
                if (now - last > 240000) {
                    lastEliteAbility.put(mob.getUniqueId(), now);
                    spawnPuppeteerMinions(mob);
                }
                break;
            case "BROODMOTHER":
                if (now - last > 5000) {
                    lastEliteAbility.put(mob.getUniqueId(), now);
                    spawnBroodmotherMinion(mob);
                }
                updateBroodmotherBuffs(mob);
                break;
            case "DISTORTED_PHANTOM":
                if (now - last > 10000) {
                    lastEliteAbility.put(mob.getUniqueId(), now);
                    phantomAbility(mob, target);
                }
                break;
        }
    }

    private void pullPlayer(LivingEntity mob, Player target) {
        Vector dir = mob.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(1.5);
        target.setVelocity(dir);
        target.sendMessage("§c§l(!) §cКровавый громила притягивает вас!");
    }

    private void handleSniper(LivingEntity mob, Player target, long now) {
        int ticks = sniperAimTicks.getOrDefault(mob.getUniqueId(), 0);
        long last = lastEliteAbility.getOrDefault(mob.getUniqueId(), 0L);

        if (ticks == 0 && now - last > 15000) {
            sniperAimTicks.put(mob.getUniqueId(), 1);
            mob.getWorld().playSound(mob.getLocation(), Sound.BLOCK_NOTE_HAT, 1f, 0.5f);
            target.sendMessage("§c§l(!) §cКостяной снайпер целится в вас...");
        } else if (ticks > 0) {
            if (ticks < 4) { // 2 секунды (т.к. таск раз в 0.5с)
                sniperAimTicks.put(mob.getUniqueId(), ticks + 1);
                mob.getWorld().spawnParticle(Particle.REDSTONE, mob.getEyeLocation().add(mob.getEyeLocation().getDirection().multiply(2)), 5, 0.1, 0.1, 0.1, 0);
            } else {
                sniperAimTicks.put(mob.getUniqueId(), 0);
                lastEliteAbility.put(mob.getUniqueId(), now);
                shootSniperArrow(mob, target);
            }
        }
    }

    private void shootSniperArrow(LivingEntity mob, Player target) {
        int count = sniperArrowCount.getOrDefault(mob.getUniqueId(), 0) + 1;
        sniperArrowCount.put(mob.getUniqueId(), count);

        Arrow arrow = mob.launchProjectile(Arrow.class);
        arrow.setMetadata("sniper_arrow", new FixedMetadataValue(plugin, true));
        arrow.setMetadata("sniper_source", new FixedMetadataValue(plugin, mob.getUniqueId().toString()));

        if (count % 10 == 0) {
            new BukkitRunnable() {
                int t = 0;
                @Override
                public void run() {
                    if (arrow.isDead() || arrow.isOnGround() || t > 40 || !target.isOnline()) { cancel(); return; }
                    Vector dir = target.getEyeLocation().toVector().subtract(arrow.getLocation().toVector()).normalize();
                    arrow.setVelocity(dir.multiply(2.0));
                    t++;
                }
            }.runTaskTimer(plugin, 1L, 1L);
        } else {
            arrow.setVelocity(target.getEyeLocation().toVector().subtract(mob.getEyeLocation().toVector()).normalize().multiply(3.0));
        }

        mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_SKELETON_SHOOT, 1f, 0.5f);
    }

    private void spawnPuppeteerMinions(LivingEntity mob) {
        Location loc = mob.getLocation();
        EntityType[] types = {EntityType.ZOMBIE, EntityType.ZOMBIE, EntityType.SKELETON, EntityType.SKELETON, EntityType.SPIDER, EntityType.SPIDER};
        for (EntityType type : types) {
            LivingEntity minion = (LivingEntity) loc.getWorld().spawnEntity(loc, type);
            minion.setMetadata("puppeteer_minion", new FixedMetadataValue(plugin, mob.getUniqueId().toString()));
            
            AttributeInstance hp = minion.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (hp != null) { hp.setBaseValue(hp.getBaseValue() * 1.25); minion.setHealth(hp.getBaseValue()); }
            AttributeInstance dmg = minion.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
            if (dmg != null) dmg.setBaseValue(dmg.getBaseValue() * 1.25);
            AttributeInstance spd = minion.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
            if (spd != null) spd.setBaseValue(spd.getBaseValue() * 1.25);

            minion.setCustomName("§7Подчинённый");
            minion.setCustomNameVisible(true);
            arena.getArenaMobs().add(minion);
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().equals(mob.getWorld())) p.sendMessage("§dКукловод призвал подчинённых!");
        }
    }

    private void spawnBroodmotherMinion(LivingEntity mob) {
        Location loc = mob.getLocation();
        LivingEntity spider = (LivingEntity) loc.getWorld().spawnEntity(loc, EntityType.CAVE_SPIDER);
        spider.setMetadata("broodmother_minion", new FixedMetadataValue(plugin, mob.getUniqueId().toString()));
        spider.setCustomName("§7Паучок");
        spider.setCustomNameVisible(true);
        arena.getArenaMobs().add(spider);
    }

    private void updateBroodmotherBuffs(LivingEntity mob) {
        long minions = mob.getWorld().getLivingEntities().stream()
                .filter(e -> e.hasMetadata("broodmother_minion") && e.getMetadata("broodmother_minion").get(0).asString().equals(mob.getUniqueId().toString()) && !e.isDead())
                .count();
        
        double buff = 1.0 + (minions * 0.10);
        MobStats base = MobStats.forElite(dev.yournick.mobarena.mob.EliteType.BROODMOTHER, arena.getWave());

        AttributeInstance dmg = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(base.getDamage() * buff);
        
        AttributeInstance spd = mob.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (spd != null) spd.setBaseValue(base.getSpeed() * buff);
        
        // ХП тоже должно расти, но текущее ХП не должно прыгать. Увеличим только Max HP.
        AttributeInstance hp = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (hp != null) hp.setBaseValue(base.getHealth() * buff);
    }

    private void phantomAbility(LivingEntity mob, Player target) {
        teleportBehind(mob, target);
        
        Vex clone = (Vex) mob.getWorld().spawnEntity(mob.getLocation(), EntityType.VEX);
        clone.setMetadata("phantom_clone", new FixedMetadataValue(plugin, mob.getUniqueId().toString()));
        clone.setCustomName("§7Иллюзия");
        
        AttributeInstance hp = clone.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (hp != null) { hp.setBaseValue(mob.getMaxHealth() / 3); clone.setHealth(hp.getBaseValue()); }
        
        arena.getArenaMobs().add(clone);
    }
}
