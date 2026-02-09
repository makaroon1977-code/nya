package dev.yournick.mobarena.listener;

import dev.yournick.mobarena.amulet.AmuletType;
import dev.yournick.mobarena.arena.ArenaManager;
import dev.yournick.mobarena.mob.MobStats;
import dev.yournick.mobarena.perk.Perk;
import dev.yournick.mobarena.player.PerkProfile;
import dev.yournick.mobarena.perk.PlayerPerk;
import dev.yournick.mobarena.player.PlayerProfile;
import dev.yournick.mobarena.player.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MobDamageListener implements Listener {

    private final ArenaManager arena;
    private final Map<UUID, Long> blacksmithUniqueCooldown = new HashMap<>();
    private boolean isProcessingAoE = false;

    public MobDamageListener(ArenaManager arena) {
        this.arena = arena;
    }

    @EventHandler
    public void onHeal(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player player = (Player) e.getEntity();
        PlayerProfile profile = arena.getPlugin().getPlayerRepository().getProfile(player);
        if (profile != null) {
            if (profile.getRegenDisabledUntil() > System.currentTimeMillis()) {
                e.setCancelled(true);
                return;
            }
            profile.recalculateStats();
            PlayerStats stats = profile.getStats();
            
            double amount = e.getAmount();
            
            // Стойкий: Сильные зелья (Крупный 3) - уже заложено в healReceived или отдельно?
            // По просьбе пользователя я сделал regenPower и healReceived.
            // Применим их.
            
            if (e.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED || 
                e.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
                amount *= stats.getRegenPower();
            } else {
                amount *= stats.getHealReceived();
            }
            
            // Стойкий Крупный 3
            if (profile.getPlayerPerk().getPerk() == Perk.TOUGH && profile.getPerkProfile(Perk.TOUGH).hasLarge3()) {
                amount *= 1.5;
            }
            
            e.setAmount(amount);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMobHitMob(EntityDamageByEntityEvent e) {
        Entity damager = e.getDamager();
        Entity target = e.getEntity();

        if (damager instanceof org.bukkit.entity.Projectile) {
            org.bukkit.projectiles.ProjectileSource source = ((org.bukkit.entity.Projectile) damager).getShooter();
            if (source instanceof Entity) {
                damager = (Entity) source;
            }
        }

        if (arena.isArenaMob(target) && arena.isArenaMob(damager)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMobTarget(EntityTargetLivingEntityEvent e) {
        if (arena.isArenaMob(e.getEntity())) {
            if (e.getTarget() != null && !(e.getTarget() instanceof Player)) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTakeDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player player = (Player) e.getEntity();

        // Отключаем эффект иссушения для элитного скелета (Искажённый скелет)
        if (e.getCause() == EntityDamageEvent.DamageCause.WITHER) {
            e.setCancelled(true);
            return;
        }

        PlayerProfile profile = arena.getPlugin().getPlayerRepository().getProfile(player);
        if (profile == null) return;

        profile.recalculateStats();
        PlayerStats stats = profile.getStats();
        PlayerPerk perk = profile.getPlayerPerk();
        if (perk == null) return;

        // 0. Стойкий: Мини 3 (Удача) или Призрачная форма или активная неуязвимость
        if (profile.isInvulnerable() || profile.isGhostFormActive()) {
            e.setCancelled(true);
            return;
        }

        // Удача (Mini 3) - Шанс стать неуязвимым на 2 сек
        if (perk.getPerk() == Perk.TOUGH) {
            int level = profile.getPerkProfile(Perk.TOUGH).getMini3();
            if (level > 0 && Math.random() < level * 0.01) {
                profile.setInvulnerable(true);
                Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> profile.setInvulnerable(false), 40L); // 2 сек
                player.sendMessage(ChatColor.GOLD + "УДАЧА! Вы неуязвимы на 2 секунды!");
                e.setCancelled(true);
                return;
            }
        }

        // Шанс уклонения (Dodge)
        if (Math.random() < stats.getDodgeChance() || profile.isPerfectDodgeActive()) {
            e.setCancelled(true);
            player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 1);
            player.sendMessage(ChatColor.AQUA + "УКЛОНЕНИЕ!");
            
            // Логика Ловкого при уклонении
            if (perk.getPerk() == Perk.AGILE) {
                // Мини 2: Контрудар
                int agileMini2 = profile.getPerkProfile(Perk.AGILE).getMini2();
                if (Math.random() < agileMini2 * 0.10) { // Шанс 10% за уровень
                    profile.addAgileCounterstrikeStack();
                    player.sendMessage(ChatColor.YELLOW + "КОНТРУДАР! Стаки: " + profile.getAgileCounterstrikeStacks());
                }

                // Крупный 1: Контратака
                if (profile.getPerkProfile(Perk.AGILE).hasLarge1()) {
                    perk.setAgileCounterattack(true);
                }
            }
            return;
        }

        // Базовый урон
        double baseDamage = e.getDamage(EntityDamageEvent.DamageModifier.BASE);

        // Скейлинг урона стрел
        if (e instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) e).getDamager();
            if (damager instanceof Arrow) {
                org.bukkit.projectiles.ProjectileSource source = ((Arrow) damager).getShooter();
                if (source instanceof Entity && arena.isArenaMob((Entity) source)) {
                    LivingEntity shooter = (LivingEntity) source;
                    if (shooter.hasMetadata("mob_stats")) {
                        MobStats mobStats = (MobStats) shooter.getMetadata("mob_stats").get(0).value();
                        baseDamage = mobStats.getDamage();
                    }
                }
            }
        }

        // Отключаем ванильную броню и другие модификаторы
        for (EntityDamageEvent.DamageModifier modifier : EntityDamageEvent.DamageModifier.values()) {
            try {
                if (e.isApplicable(modifier) && modifier != EntityDamageEvent.DamageModifier.BASE) {
                    e.setDamage(modifier, 0);
                }
            } catch (Exception ignored) {}
        }

        // Отражение урона (от урона ДО снижения)
        if (stats.getDamageReflection() > 0 && e instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) e).getDamager();
            Entity realDamager = damager;
            if (damager instanceof org.bukkit.entity.Projectile) {
                org.bukkit.projectiles.ProjectileSource source = ((org.bukkit.entity.Projectile) damager).getShooter();
                if (source instanceof Entity) realDamager = (Entity) source;
            }
            
            if (realDamager instanceof LivingEntity) {
                double reflected = baseDamage * stats.getDamageReflection();
                ((LivingEntity) realDamager).damage(reflected);
                
                // Визуальный эффект отражения
                realDamager.getWorld().spawnParticle(Particle.CRIT_MAGIC, realDamager.getLocation().add(0, 1, 0), 5);
            }
        }

        // --- Расчет снижения урона ---
        
        // Шанс Щитоносца (10% при получении урона)
        if (hasArmorEnchant(player, "Щитоносец") && Math.random() < 0.10) {
            profile.activateShieldBearer(5);
            player.sendMessage(ChatColor.BLUE + "АКТИВИРОВАН ЩИТОНОСЕЦ!");
        }

        // Кузнец: Броня в бою (Large 2) - 15% шанс на +20% брони
        if (perk.getPerk() == Perk.BLACKSMITH && profile.getPerkProfile(Perk.BLACKSMITH).hasLarge2()) {
            if (Math.random() < 0.15) {
                profile.activateBlacksmithInBattleArmor(3);
                player.sendMessage(ChatColor.YELLOW + "Броня в бою активирована!");
            }
        }
        
        // Кузнец: Активная абилка (Unique)
        if (perk.getPerk() == Perk.BLACKSMITH && profile.getPerkProfile(Perk.BLACKSMITH).hasUnique()) {
            if (player.getHealth() / player.getMaxHealth() <= 0.30 && !profile.isAbilityOnCooldown("blacksmith_unique")) {
                profile.setAbilityCooldown("blacksmith_unique", 60);
                profile.activateBlacksmithArmor(10);
                player.sendMessage(ChatColor.GOLD + "АКТИВИРОВАНА ЖИВАЯ БРОНЯ (+100% защиты)!");
            }
        }

        double reduction = stats.getArmorPercent();

        // Защита от огня (Additive)
        if (e.getCause() == EntityDamageEvent.DamageCause.FIRE || e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK || e.getCause() == EntityDamageEvent.DamageCause.LAVA) {
            double fireRes = stats.getFireResistance();
            if (fireRes >= 1.0) {
                e.setCancelled(true);
                return;
            }
            baseDamage *= (1.0 - fireRes);
        }

        // Специальные щиты
        if (profile.isTitanShieldActive()) reduction += 0.80; // Блокирует 80%
        if (profile.isAbsoluteBarrierActive()) reduction += 0.99; // Блокирует 99%
        if (profile.isShieldBearerActive()) reduction += 0.40;
        if (profile.isBlacksmithArmorActive()) reduction += 1.0;
        if (profile.isBlacksmithInBattleArmorActive()) reduction += 0.20;
        
        // Зачарование: Последний рубеж (+40% армора при HP <= 50%)
        if (player.getHealth() / player.getMaxHealth() <= 0.50 && hasArmorEnchant(player, "Последний рубеж")) {
            reduction += 0.40;
        }

        // Стойкость (защита от элитных мобов)
        if (e instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) e).getDamager();
            Entity realDamager = damager;
            if (damager instanceof org.bukkit.entity.Projectile) {
                org.bukkit.projectiles.ProjectileSource source = ((org.bukkit.entity.Projectile) damager).getShooter();
                if (source instanceof Entity) realDamager = (Entity) source;
            }
            if (realDamager.hasMetadata("elite_type") && hasArmorEnchant(player, "Стойкость")) {
                reduction += 0.30;
            }
        }

        // Эффект Стойкости от обычных мобов (должен быть 0 по условию, но у нас reduction и так база stats.getArmorPercent())
        // "Стойкость — -30 процентов урон от элитных мобов (только элитных обычные должны наносить нормально"
        // Это значит что против обычных мобов бонус +30% не дается. Это уже соблюдено.

        double finalDamage = baseDamage * (1.0 - Math.min(0.99, reduction));

        // Absolute Barrier trigger
        boolean fatal = (player.getHealth() - finalDamage <= 0);

        // Стойкий: Последний рубеж (Unique) - Passive
        if (perk.getPerk() == Perk.TOUGH && profile.getPerkProfile(Perk.TOUGH).hasUnique()) {
            if (fatal || (player.getHealth() - finalDamage) / player.getMaxHealth() < 0.10) {
                if (!profile.isAbilityOnCooldown("tough_active")) {
                    profile.setAbilityCooldown("tough_active", 120);
                    profile.setInvulnerable(true);
                    Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> profile.setInvulnerable(false), 100L); // 5 сек
                    
                    // Замедление врагов
                    for (Entity ent : player.getNearbyEntities(5, 5, 5)) {
                        if (ent instanceof LivingEntity && !(ent instanceof Player)) {
                            ((LivingEntity) ent).addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 2));
                        }
                    }
                    
                    player.sendMessage(ChatColor.DARK_RED + "ПОСЛЕДНИЙ РУБЕЖ! Вы бессмертны на 5 секунд!");
                    if (fatal) {
                        e.setDamage(0);
                        player.setHealth(Math.min(player.getMaxHealth(), 2.0)); // Оставляем немного ХП
                        return;
                    }
                }
            }
        }

        if ((fatal || player.getHealth() / player.getMaxHealth() < 0.25) && !profile.isAbsoluteBarrierActive() && profile.getAbsoluteBarrierCooldown() < System.currentTimeMillis()) {
            if (hasArmorEnchant(player, "Абсолютный барьер")) {
                profile.setAbsoluteBarrierActive(true);
                Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> profile.setAbsoluteBarrierActive(false), 200L);
                profile.setAbsoluteBarrierCooldown(System.currentTimeMillis() + 150000);
                player.sendMessage(ChatColor.GOLD + "АКТИВИРОВАН АБСОЛЮТНЫЙ БАРЬЕР!");
                
                if (fatal) {
                    finalDamage = 0; // Блокируем смертельный удар
                } else {
                    finalDamage = baseDamage * (1.0 - Math.min(0.99, reduction + 0.99));
                }
            }
        }

        // Second Life trigger
        if (player.getHealth() - finalDamage <= 0 && profile.isSecondLifeAvailable()) {
            if (hasArmorEnchant(player, "Вторая жизнь")) {
                e.setDamage(0);
                player.setHealth(player.getMaxHealth());
                profile.setSecondLifeAvailable(false);
                profile.activateShieldBearer(15);
                player.sendMessage(ChatColor.LIGHT_PURPLE + "ВТОРАЯ ЖИЗНЬ!");
                return;
            }
        }

        // Death Save Chance
        if (player.getHealth() - finalDamage <= 0) {
            if (Math.random() < stats.getDeathSaveChance()) {
                e.setDamage(0);
                player.setHealth(1.0);
                player.sendMessage(ChatColor.GOLD + "СПАСЕНИЕ ОТ СМЕРТИ!");
                return;
            }
        }

        e.setDamage(finalDamage);

        // --- Mob Abilities on hit ---
        if (e instanceof EntityDamageByEntityEvent) {
            Entity damager = ((EntityDamageByEntityEvent) e).getDamager();
            
            // Bone Sniper Arrow
            if (damager instanceof Arrow && damager.hasMetadata("sniper_arrow")) {
                double sniperDamage = 5.0 + (player.getHealth() * 0.80);
                e.setDamage(sniperDamage);
                profile.setRegenDisabledUntil(System.currentTimeMillis() + 60000);
                player.sendMessage(ChatColor.RED + "Снайпер пробил вашу броню и выключил регенерацию!");
            }

            if (damager instanceof org.bukkit.entity.Projectile) {
                org.bukkit.projectiles.ProjectileSource source = ((org.bukkit.entity.Projectile) damager).getShooter();
                if (source instanceof Entity) damager = (Entity) source;
            }

            if (arena.isArenaMob(damager) && damager instanceof LivingEntity) {
                LivingEntity mob = (LivingEntity) damager;
                
                // Элитные способности при ударе по игроку
                if (mob.hasMetadata("elite_type")) {
                    String eliteType = mob.getMetadata("elite_type").get(0).asString();
                    if (eliteType.equals("BLOOD_BULLY")) {
                        double heal = mob.getMaxHealth() * 0.25;
                        mob.setHealth(Math.min(mob.getMaxHealth(), mob.getHealth() + heal));
                        if (mob.getHealth() / mob.getMaxHealth() < 0.30) {
                            mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 2));
                            mob.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 100, 2));
                        }
                    } else if (eliteType.equals("WITHER_PHANTOM")) {
                        // Урон усиливается, если не убивать клонов
                        long clones = mob.getWorld().getEntitiesByClass(org.bukkit.entity.WitherSkeleton.class).stream()
                                .filter(sk -> sk.hasMetadata("phantom_clone") && sk.getMetadata("phantom_clone").get(0).asString().equals(mob.getUniqueId().toString()))
                                .count();
                        e.setDamage(e.getDamage() * (1.0 + clones * 0.2));
                    }
                }

                if (mob.getType() == org.bukkit.entity.EntityType.ZOMBIE) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
                    double heal = mob.getMaxHealth() * 0.1;
                    mob.setHealth(Math.min(mob.getMaxHealth(), mob.getHealth() + heal));
                } else if (mob.getType() == org.bukkit.entity.EntityType.SPIDER) {
                    if (Math.random() < 0.1) {
                        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
                    }
                }
            }
        }


        // --- Легендарные Амулеты ---
        double currentHealthRatio = player.getHealth() / player.getMaxHealth();

        // Амулет Бессмертия: Призрачная форма (<10% HP)
        if (currentHealthRatio < 0.10 && profile.hasAmuletEquipped(AmuletType.IMMORTALITY) && !profile.isOnCooldown(AmuletType.IMMORTALITY)) {
            profile.setGhostFormActive(true);
            profile.setCooldown(AmuletType.IMMORTALITY, 120);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Активирована Призрачная Форма!");
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0));
            
            Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
                profile.setGhostFormActive(false);
                player.sendMessage(ChatColor.GRAY + "Призрачная Форма закончилась.");
            }, 60L); // 3 секунды
            
            e.setCancelled(true);
            return;
        }

        // Амулет Титана: Непробиваемый щит (<30% HP)
        if (currentHealthRatio < 0.30 && profile.hasAmuletEquipped(AmuletType.TITAN) && !profile.isOnCooldown(AmuletType.TITAN)) {
            profile.setTitanShieldActive(true);
            profile.setCooldown(AmuletType.TITAN, 90);
            player.sendMessage(ChatColor.GOLD + "Активирован Непробиваемый Щит!");
            
            Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
                profile.setTitanShieldActive(false);
                player.sendMessage(ChatColor.GRAY + "Щит Титана исчез.");
            }, 100L); // 5 секунд
        }

        // Амулет Вечного Огня: Взрыв пламени (<20% HP)
        if (currentHealthRatio < 0.20 && profile.hasAmuletEquipped(AmuletType.ETERNAL_FIRE) && !profile.isOnCooldown(AmuletType.ETERNAL_FIRE)) {
            profile.setCooldown(AmuletType.ETERNAL_FIRE, 120);
            player.sendMessage(ChatColor.RED + "ВЗРЫВ ПЛАМЕНИ!");
            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 100, 2, 1, 2, 0.1);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            
            double damage = (20 + profile.getStrengthLevel() * 5) * 0.5; // Примерная база
            for (Entity entity : player.getNearbyEntities(4, 4, 4)) {
                if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                    ((LivingEntity) entity).damage(damage, player);
                    entity.setFireTicks(40);
                }
            }
        }
        
        // Стойкий Крупный 1: Железная воля
        if (perk.getPerk() == Perk.TOUGH && profile.getPerkProfile(Perk.TOUGH).hasLarge1()) {
            e.setDamage(e.getDamage() * (1.0 - profile.getIronWillStacks() * 0.03));
            profile.addIronWillStack();
        }

        // Огнерожденный Мини 3: Поджог атакующего
        if (perk.getPerk() == Perk.FIREBORN && e instanceof EntityDamageByEntityEvent) {
            int level = profile.getPerkProfile(Perk.FIREBORN).getMini3();
            if (Math.random() < level * 0.07) {
                ((EntityDamageByEntityEvent) e).getDamager().setFireTicks((int)(60 * stats.getBurnDurationPercent()));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDealDamage(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player player = (Player) e.getDamager();
        if (!(e.getEntity() instanceof LivingEntity)) return;
        LivingEntity target = (LivingEntity) e.getEntity();

        // Отключаем ванильные модификаторы
        for (EntityDamageEvent.DamageModifier modifier : EntityDamageEvent.DamageModifier.values()) {
            try {
                if (e.isApplicable(modifier) && modifier != EntityDamageEvent.DamageModifier.BASE) {
                    e.setDamage(modifier, 0);
                }
            } catch (Exception ignored) {}
        }

        if (isProcessingAoE) return;

        PlayerProfile profile = arena.getPlugin().getPlayerRepository().getProfile(player);
        if (profile == null) return;
        
        profile.recalculateStats();
        PlayerStats stats = profile.getStats();
        PlayerPerk perk = profile.getPlayerPerk();
        if (perk == null) return;

        // Элитные механики (Кукловод)
        if (target.hasMetadata("elite_type") && target.getMetadata("elite_type").get(0).asString().equals("PUPPETEER")) {
            boolean hasMinions = target.getWorld().getLivingEntities().stream()
                    .anyMatch(ent -> ent.hasMetadata("puppeteer_minion") && 
                            ent.getMetadata("puppeteer_minion").get(0).asString().equals(target.getUniqueId().toString()) && 
                            !ent.isDead());
            if (hasMinions) {
                e.setCancelled(true);
                player.sendMessage(ChatColor.RED + "Убейте всех подчинённых!");
                return;
            }
        }

        // --- Расчет Урона ---
        double baseDamage = getBaseWeaponDamage(player.getInventory().getItemInMainHand());
        
        // 1. Скорость атаки
        int ticks = Math.max(1, (int)(666.0 / (stats.getAttackSpeedPercent() * 100.0)));
        target.setNoDamageTicks(ticks);

        // 2. Броня моба
        double mobArmor = 0;
        double mobMageArmor = 0;
        double mobFireArmor = 0;

        if (arena.isArenaMob(target) && target.hasMetadata("mob_stats")) {
            MobStats mobStats = (MobStats) target.getMetadata("mob_stats").get(0).value();
            mobArmor = mobStats.getArmor();
            mobMageArmor = mobStats.getMageArmor();
            mobFireArmor = mobStats.getFireArmor();
        }

        // Слом брони (-15%)
        if (hasSwordEnchant(player, "Слом брони")) {
            mobArmor -= 0.15;
        }

        double mobReduction = mobArmor;
        double finalMobArmorMult = (mobReduction >= 0) ? (1.0 - mobReduction) : (1.0 + Math.abs(mobReduction));
        
        if (e.getCause() == EntityDamageEvent.DamageCause.MAGIC) finalMobArmorMult *= (1.0 - mobMageArmor);
        if (e.getCause() == EntityDamageEvent.DamageCause.FIRE) finalMobArmorMult *= (1.0 - mobFireArmor);
        baseDamage *= finalMobArmorMult;

        // 3. Общий урон (Система сложения)
        double currentDamage = baseDamage * stats.getDamagePercent();

        // 4. Критический урон
        boolean isCrit = Math.random() < stats.getCritChance();
        if (isCrit) {
            currentDamage *= stats.getCritDamage();
            target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1.5, 0), 15, 0.3, 0.3, 0.3, 0.1);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
            player.sendMessage(ChatColor.YELLOW + "КРИТ! " + ChatColor.GOLD + "(" + String.format("%.1f", currentDamage) + ")");
        }

        // 5. Огненный урон
        if (target.getFireTicks() > 0 || e.getCause() == EntityDamageEvent.DamageCause.FIRE) {
            currentDamage *= stats.getFireDamagePercent();
        }

        // --- Зачарования и Перки (Дополнительные множители) ---
        if (hasSwordEnchant(player, "Палач")) currentDamage *= 1.2;
        if (hasSwordEnchant(player, "Ярость") && (player.getHealth() / player.getMaxHealth() < 0.5)) currentDamage *= 1.5;
        if (profile.isRuthlessnessActive()) {
            currentDamage *= 3.0;
            profile.setRuthlessnessActive(false);
        }
        if (profile.getDevourerCharges() > 0) {
            currentDamage *= 1.5;
            profile.setDevourerCharges(profile.getDevourerCharges() - 1);
        }
        if (hasSwordEnchant(player, "Роковой удар") && Math.random() < 0.07) {
            currentDamage *= 3.0;
            player.sendMessage(ChatColor.DARK_RED + "РОКОВОЙ УДАР!");
        }

        // Кузнец: Смертельный удар
        if (perk.getPerk() == Perk.BLACKSMITH && profile.getPerkProfile(Perk.BLACKSMITH).hasLarge1() && Math.random() < 0.1) {
            currentDamage = 500;
            player.sendMessage(ChatColor.RED + "СМЕРТЕЛЬНЫЙ УДАР!");
        }

        // Ловкий: Контрудар
        if (profile.getAgileCounterstrikeStacks() > 0) {
            currentDamage *= (1.0 + profile.getAgileCounterstrikeStacks() * 0.5);
        }

        // Ловкий: Контратака (Large 1)
        if (perk.isAgileCounterattack()) {
            currentDamage *= profile.isPerfectDodgeActive() ? 10.0 : 1.5;
            perk.setAgileCounterattack(false);
        }

        // Удачливый: Удачный удар
        if (perk.getPerk() == Perk.LUCKY && profile.getPerkProfile(Perk.LUCKY).hasLarge3() && Math.random() < 0.1) {
            double mult = 1.0 + Math.random() * 2.0;
            currentDamage *= mult;
            player.sendMessage(ChatColor.GREEN + "УДАЧНЫЙ УДАР! " + ChatColor.WHITE + "(x" + String.format("%.1f", mult) + ")");
        }

        // --- Эффекты амулетов ---
        if (profile.hasAmuletEquipped(AmuletType.DESTRUCTION) && !profile.isOnCooldown(AmuletType.DESTRUCTION)) {
            profile.setCooldown(AmuletType.DESTRUCTION, 10);
            doAoEDamage(player, target, currentDamage * 0.5, 3.0);
            target.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, target.getLocation(), 1);
        }
        if (profile.hasAmuletEquipped(AmuletType.STORM)) {
            profile.setStormAttacks(profile.getStormAttacks() + 1);
            if (profile.getStormAttacks() >= 6 && !profile.isOnCooldown(AmuletType.STORM)) {
                profile.setStormAttacks(0);
                profile.setCooldown(AmuletType.STORM, 6);
                target.getWorld().strikeLightningEffect(target.getLocation());
                doAoEDamage(player, target, currentDamage * 0.5, 5.0); // Упрощенно
            }
        }

        // --- Вампиризм (от финального урона) ---
        double lifesteal = currentDamage * stats.getLifesteal();
        if (lifesteal > 0) {
            player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + lifesteal));
        }

        // Дополнительные зачарования (Разрез, Высасывание и т.д.)
        if (hasSwordEnchant(player, "Разрез")) doAoEDamage(player, target, currentDamage * 0.1, 3.0);
        if (hasSwordEnchant(player, "Высасывание")) player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 1.0));
        if (hasSwordEnchant(player, "Разгром") && !isProcessingAoE) {
            profile.setSmashCounter(profile.getSmashCounter() + 1);
            if (profile.getSmashCounter() >= 4) {
                profile.setSmashCounter(0);
                doAoEDamage(player, target, currentDamage * 1.0, 3.0);
                player.sendMessage(ChatColor.RED + "РАЗГРОМ!");
            }
        }

        // --- Пиратский грабеж (Mini 1) ---
        if (perk.getPerk() == Perk.PIRATE) {
            int level = profile.getPerkProfile(Perk.PIRATE).getMini1();
            if (level > 0 && !target.hasMetadata("looted_" + target.getUniqueId())) {
                if (Math.random() < level * 0.03) {
                    target.setMetadata("looted_" + target.getUniqueId(), new FixedMetadataValue(arena.getPlugin(), true));
                    profile.addGold(5);
                    player.sendMessage(ChatColor.GOLD + "+5 золота (Пиратский грабеж)");
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
                }
            }
        }

        e.setDamage(currentDamage);

        // Сброс стаков контрудара
        if (!isProcessingAoE && profile.getAgileCounterstrikeStacks() > 0) {
            profile.resetAgileCounterstrike();
        }

        // Обновление HP-бара моба
        if (arena.isArenaMob(target)) {
            arena.getPlugin().getServer().getScheduler().runTaskLater(arena.getPlugin(), () -> arena.updateMobName(target), 1L);
        }
    }

    private double getBaseWeaponDamage(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return 1.0;

        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            if (name.equals("Меч Гладиатора")) return 9.0;
            if (name.equals("Меч Погибели")) return 12.0;
            if (name.equals("Клинок Бездны")) return 15.0;
        }

        switch (item.getType()) {
            case WOOD_SWORD: return 1.0;
            case STONE_SWORD: return 2.5;
            case IRON_SWORD: return 4.0;
            case DIAMOND_SWORD: return 6.0;
            default: return 1.0;
        }
    }

    private void doAoEDamage(Player player, LivingEntity target, double damage, double radius) {
        isProcessingAoE = true;
        try {
            for (Entity ent : target.getNearbyEntities(radius, radius, radius)) {
                if (ent instanceof LivingEntity && arena.isArenaMob(ent) && ent != target) {
                    ((LivingEntity) ent).damage(damage, player);
                }
            }
        } finally {
            isProcessingAoE = false;
        }
    }

    private boolean hasSwordEnchant(Player player, String enchantName) {
        ItemStack item = player.getInventory().getItemInMainHand();
        return hasEnchant(item, enchantName);
    }

    private boolean hasArmorEnchant(Player player, String enchantName) {
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (hasEnchant(item, enchantName)) return true;
        }
        return false;
    }

    private boolean hasEnchant(ItemStack item, String enchantName) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) return false;
        for (String line : item.getItemMeta().getLore()) {
            if (line.contains(enchantName)) return true;
        }
        return false;
    }
}
