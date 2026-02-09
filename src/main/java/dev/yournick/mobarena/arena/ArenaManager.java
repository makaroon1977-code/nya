package dev.yournick.mobarena.arena;

import dev.yournick.mobarena.MobArenaPlugin;
import dev.yournick.mobarena.mob.EliteType;
import dev.yournick.mobarena.mob.MobStats;
import dev.yournick.mobarena.perk.Perk;
import dev.yournick.mobarena.player.PlayerProfile;
import dev.yournick.mobarena.player.PlayerStats;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ArenaManager {

    private final MobArenaPlugin plugin;

    private final Set<UUID> players = new HashSet<>();
    private final Set<LivingEntity> arenaMobs = new HashSet<>();
    private final Map<Location, Long> fireTrails = new ConcurrentHashMap<>();

    private int wave = 0;
    private boolean running = false;

    private Location playerSpawn;
    private Location mobSpawn;
    private Location lobbySpawn;

    public ArenaManager(MobArenaPlugin plugin) {
        this.plugin = plugin;
        loadLocations();
    }
    public int getWave() {
        return wave;
    }

    public MobArenaPlugin getPlugin() { return plugin; }

    /* ===================== */
    /* JOIN / LEAVE */
    /* ===================== */

    public void join(Player player) {
        if (playerSpawn == null) {
            player.sendMessage(ChatColor.RED + "Ошибка: точка спавна игроков не задана!");
            return;
        }
        players.add(player.getUniqueId());
        player.teleport(playerSpawn);
        applyPlayerEffects(player);
        player.sendMessage(ChatColor.GREEN + "Ты вошёл на арену!");
    }

    public void leave(Player player) {
        if (lobbySpawn == null) {
            player.sendMessage(ChatColor.RED + "Ошибка: точка выхода из арены не задана!");
            return;
        }
        players.remove(player.getUniqueId());
        player.teleport(lobbySpawn);
        clearEffects(player);
        player.sendMessage(ChatColor.YELLOW + "Ты покинул арену.");
    }

    /* ===================== */
    /* WAVES */
    /* ===================== */

    public void startWave() {
        if (running) return;

        running = true;
        wave++;

        // Сброс эффектов перед волной
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                PlayerProfile profile = plugin.getPlayerRepository().getProfile(p);
                if (profile != null) {
                    profile.setSecondLifeAvailable(true);
                }
            }
        }
        
        if (wave % 7 == 0) {
            broadcast(ChatColor.LIGHT_PURPLE + "⭐ СПЕЦИАЛЬНАЯ ВОЛНА! Убейте золотую свинью!");
            spawnSpecialPig();
        } else if ((wave - 5) % 10 == 0) {
            broadcast(ChatColor.RED + "⚡ ЭЛИТНАЯ ВОЛНА! Появились сильные противники!");
            int eliteCount = (wave > 30) ? 2 : 1;
            EliteType[] types = EliteType.values();
            int eliteIndex = ((wave - 5) / 10) % types.length;
            
            spawnEliteMob(types[eliteIndex]);
            if (eliteCount > 1) {
                spawnEliteMob(types[(eliteIndex + 1) % types.length]);
            }
        } else {
            broadcast(ChatColor.GOLD + "⚔ Волна " + wave + " началась!");
            // Изначально 5 мобов, каждые 3 волны +1
            int mobCount = 5 + ((wave - 1) / 3);
            for (int i = 0; i < mobCount; i++) spawnMob();
        }
    }

    private void spawnEliteMob(EliteType type) {
        if (mobSpawn == null) return;

        LivingEntity entity = (LivingEntity) mobSpawn.getWorld().spawnEntity(mobSpawn, type.getEntityType());
        
        if (entity.getEquipment() != null) {
            entity.getEquipment().clear();
            if (type == EliteType.BONE_SNIPER) {
                entity.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
            }
        }

        if (entity instanceof Zombie) {
            ((Zombie) entity).setBaby(false);
        }

        MobStats stats = MobStats.forElite(type, wave);
        entity.setMetadata("mob_stats", new FixedMetadataValue(plugin, stats));
        entity.setMetadata("elite_type", new FixedMetadataValue(plugin, type.name()));

        AttributeInstance healthAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(stats.getHealth());
            entity.setHealth(stats.getHealth());
        }

        AttributeInstance damageAttr = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(stats.getDamage());
        }

        AttributeInstance speedAttr = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(stats.getSpeed());
        }

        entity.setCustomName(ChatColor.RED + "☠ " + type.getDisplayName());
        entity.setCustomNameVisible(true);
        entity.setRemoveWhenFarAway(false);
        updateMobName(entity);

        arenaMobs.add(entity);
    }

    private void spawnSpecialPig() {
        if (mobSpawn == null) return;
        
        LivingEntity pig = (LivingEntity) mobSpawn.getWorld().spawnEntity(mobSpawn, org.bukkit.entity.EntityType.PIG);
        pig.setMetadata("is_special_pig", new FixedMetadataValue(plugin, true));
        
        int pigNumber = wave / 7;
        double health = 100.0 * pigNumber;
        
        AttributeInstance healthAttr = pig.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(health);
            pig.setHealth(health);
        }
        
        pig.setCustomName(ChatColor.GOLD + "Золотая Свинья [" + pigNumber + "]");
        pig.setCustomNameVisible(true);
        updateMobName(pig);
        
        arenaMobs.add(pig);
    }

    private void spawnMob() {
        if (mobSpawn == null) return;

        // Выбираем тип моба (Зомби 50%, Скелет 30%, Паук 20%)
        org.bukkit.entity.EntityType type = org.bukkit.entity.EntityType.ZOMBIE;
        double rand = Math.random();
        if (rand < 0.2) type = org.bukkit.entity.EntityType.SPIDER;
        else if (rand < 0.5) type = org.bukkit.entity.EntityType.SKELETON;

        LivingEntity entity = (LivingEntity) mobSpawn.getWorld().spawnEntity(mobSpawn, type);

        // Настройка экипировки
        if (entity.getEquipment() != null) {
            entity.getEquipment().clear();
            if (type == org.bukkit.entity.EntityType.SKELETON) {
                entity.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
            } else {
                entity.getEquipment().setItemInMainHand(null);
            }
            entity.getEquipment().setItemInOffHand(null);
        }

        if (entity instanceof Zombie) {
            Zombie zombie = (Zombie) entity;
            zombie.setBaby(false);
            if (zombie.getVehicle() != null) {
                zombie.getVehicle().remove();
            }
        }

        MobStats stats = MobStats.forWave(wave);
        
        // Сохраняем кастомные статы в метаданные моба
        entity.setMetadata("mob_stats", new FixedMetadataValue(plugin, stats));

        AttributeInstance healthAttr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(stats.getHealth());
            entity.setHealth(stats.getHealth());
        }

        AttributeInstance damageAttr = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damageAttr != null) {
            damageAttr.setBaseValue(stats.getDamage());
        }

        AttributeInstance speedAttr = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(stats.getSpeed());
        }

        entity.setCustomNameVisible(true);
        entity.setRemoveWhenFarAway(false);
        updateMobName(entity);

        arenaMobs.add(entity);
    }

    /* ===================== */
    /* MOB DEATH */
    /* ===================== */

    public void onMobDeath(LivingEntity mob, Player killer) {
        arenaMobs.remove(mob);

        if (killer != null) {
            PlayerProfile killerProfile = plugin.getPlayerRepository().getProfile(killer);
            killerProfile.recalculateStats();
            PlayerStats stats = killerProfile.getStats();

            // Специальная награда за свинью
            if (mob.getType() == org.bukkit.entity.EntityType.PIG && mob.hasMetadata("is_special_pig")) {
                killerProfile.addGold(350);
                killerProfile.addShards(5);
                killerProfile.addSpecialPigKill();
                if (killerProfile.getPlayerPerk() != null && killerProfile.getPlayerPerk().getPerk() != null) {
                    killerProfile.addPerkXp(killerProfile.getPlayerPerk().getPerk(), 250);
                }
                killer.sendMessage(ChatColor.LIGHT_PURPLE + "⭐ ЗОЛОТАЯ СВИНЬЯ ПОВЕРЖЕНА! +350 золота, +250 XP, +5 осколков");
            } else {
                // Дроп кастомного предмета
                ItemStack drop = null;

                // Luck/DropChance check
                double finalDropChance = stats.getDropChance() * stats.getLuck();
                if (Math.random() < finalDropChance) {
                    switch (mob.getType()) {
                        case ZOMBIE: drop = ArenaLoot.getZombieDrop(); break;
                        case SKELETON: drop = ArenaLoot.getSkeletonDrop(); break;
                        case SPIDER: drop = ArenaLoot.getSpiderDrop(); break;
                        default: drop = ArenaLoot.getZombieDrop(); break;
                    }
                }
                
                // Rare Drop Chance (e.g. Gold Nugget for everyone)
                if (Math.random() < stats.getRareDropChance()) {
                    mob.getWorld().dropItemNaturally(mob.getLocation(), ArenaLoot.getGoldDrop());
                }

                // Бонусы торговца (Дроп)
                if (killerProfile.getPlayerPerk().getPerk() == Perk.TRADER) {
                    int goldDiggerLevel = killerProfile.getPerkProfile(Perk.TRADER).getMini3();
                    // Золотоискатель (Мини 3) - повысили шанс до 4% за уровень (20% макс)
                    // Или Золотая жила (Активная) - 100% шанс
                    if (Math.random() < goldDiggerLevel * 0.04 || killerProfile.isGoldenVeinActive()) {
                        mob.getWorld().dropItemNaturally(mob.getLocation(), ArenaLoot.getGoldDrop());
                    }

                    // Удача торговца (Крупный 2) - шанс на доп предмет
                    if (killerProfile.getPerkProfile(Perk.TRADER).hasLarge2() && Math.random() < 0.20) {
                        if (drop != null) mob.getWorld().dropItemNaturally(mob.getLocation(), drop.clone());
                    }
                }

                if (drop != null) {
                    mob.getWorld().dropItemNaturally(mob.getLocation(), drop);
                    
                    // Double Drop Chance (Amulets/etc)
                    if (Math.random() < stats.getDoubleDropChance()) {
                        mob.getWorld().dropItemNaturally(mob.getLocation(), drop.clone());
                    }
                    
                    // Пират: Грабёж (Крупный 2) - 25% шанс на двойной дроп
                    if (killerProfile.getPlayerPerk().getPerk() == Perk.PIRATE && killerProfile.getPerkProfile(Perk.PIRATE).hasLarge2()) {
                        if (Math.random() < 0.25) {
                            mob.getWorld().dropItemNaturally(mob.getLocation(), drop.clone());
                            killer.sendMessage(ChatColor.GOLD + "ГРАБЁЖ! Двойной дроп.");
                        }
                    }
                }

                // Даем немного золота сразу
                int gold = plugin.getConfig().getInt("rewards.gold_per_mob", 5);
                
                // Бонус Золотой жилы (+20%)
                if (killerProfile.isGoldenVeinActive()) {
                    gold = (int) (gold * 1.2);
                }
                
                // Применяем бонусы/штрафы статов
                gold = (int) (gold * stats.getGoldGainPercent());

                // Jackpot (Lucky Large 1)
                if (killerProfile.getPlayerPerk().getPerk() == Perk.LUCKY && 
                    killerProfile.getPerkProfile(Perk.LUCKY).hasLarge1()) {
                    if (Math.random() < 0.05) {
                        gold *= 10;
                        killer.sendMessage(ChatColor.GOLD + "§lДЖЕКПОТ! §eВы получили в 10 раз больше золота!");
                    }
                }
                
                killerProfile.addGold(gold);
                
                // Heal on Kill
                if (stats.getHealOnKill() > 0) {
                    double newHealth = Math.min(killer.getMaxHealth(), killer.getHealth() + stats.getHealOnKill());
                    killer.setHealth(newHealth);
                }

                // Магические осколки (10% шанс)
                if (Math.random() < 0.10) {
                    killerProfile.addShards(1);
                    killer.sendMessage(ChatColor.AQUA + "+1 Магический осколок!");
                    killer.playSound(killer.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 2f);
                }

                // Даем опыт перка
                if (killerProfile.getPlayerPerk() != null && killerProfile.getPlayerPerk().getPerk() != null) {
                    Perk activePerk = killerProfile.getPlayerPerk().getPerk();
                    killerProfile.addPerkXp(activePerk, 5); // 50 -> 5 (10x reduction)
                    killer.sendMessage(ChatColor.GOLD + "+ " + gold + " золота | " + ChatColor.GREEN + "+5 XP " + activePerk.getDisplayName());
                } else {
                    killer.sendMessage(ChatColor.GOLD + "+ " + gold + " золота за убийство");
                }
            }
        }

        if (arenaMobs.isEmpty()) {
            running = false;
            boolean special = (wave % 7 == 0);
            
            if (special) {
                broadcast(ChatColor.LIGHT_PURPLE + "✔ Специальная волна " + wave + " пройдена! Ваше макс. HP увеличено на 10%!");
            } else {
                broadcast(ChatColor.GREEN + "✔ Волна " + wave + " пройдена!");
            }

            int waveGold = plugin.getConfig().getInt("rewards.gold_per_wave", 50);
            int waveXp = plugin.getConfig().getInt("rewards.xp_per_wave", 50);

            for (UUID uuid : players) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) continue;

                PlayerProfile profile = plugin.getPlayerRepository().getProfile(p);
                
                if (special) {
                    profile.addWaveHealthBonus();
                }
                
                profile.recalculateStats();
                
                if (wave > profile.getBestWave()) {
                    profile.setBestWave(wave);
                }
                
                int finalWaveGold = (int) (waveGold * profile.getStats().getGoldGainPercent());
                profile.addGold(finalWaveGold);

                if (profile.getPlayerPerk() != null && profile.getPlayerPerk().getPerk() != null) {
                    Perk activePerk = profile.getPlayerPerk().getPerk();
                    profile.addPerkXp(activePerk, waveXp);
                    p.sendMessage(ChatColor.GOLD + "Бонус за волну: + " + finalWaveGold + " золота | " +
                            ChatColor.GREEN + " + " + waveXp + " XP " + activePerk.getDisplayName());
                } else {
                    p.sendMessage(ChatColor.GOLD + "Бонус за волну: + " + finalWaveGold + " золота");
                }
                
                // Применяем обновленное HP
                applyPlayerEffects(p);
            }
        }
    }

    /* ===================== */
    /* PLAYER EFFECTS */
    /* ===================== */

    public void applyPlayerEffects(Player player) {
        clearEffects(player);
        PlayerProfile profile = plugin.getPlayerRepository().getProfile(player);
        if (profile == null) return;
        
        profile.recalculateStats();
        PlayerStats stats = profile.getStats();

        // Health from upgrades and stats + Wave bonus
        double baseMaxHealth = 20.0 + profile.getBonusHealth();
        double finalMaxHealth = baseMaxHealth * stats.getMaxHealthPercent();
        
        AttributeInstance healthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(finalMaxHealth);
            if (player.getHealth() > finalMaxHealth) player.setHealth(finalMaxHealth);
        }

        // Speed from upgrades and stats
        double baseSpeed = 0.1 + profile.getBonusSpeed();
        double finalSpeed = baseSpeed * stats.getMoveSpeedPercent();
        
        // Используем walkSpeed для управления скоростью и FOV. 
        // Атрибут GENERIC_MOVEMENT_SPEED оставляем дефолтным (0.1), чтобы избежать двойного ускорения.
        AttributeInstance moveSpeedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (moveSpeedAttr != null) {
            moveSpeedAttr.setBaseValue(0.1);
        }
        
        // Рассчитываем walkSpeed относительно стандарта 0.2
        float targetWalkSpeed = (float) ((finalSpeed / 0.1) * 0.2);
        player.setWalkSpeed(Math.min(1.0f, targetWalkSpeed));

        // Attack Speed from stats
        AttributeInstance attackSpeedAttr = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
        if (attackSpeedAttr != null) {
            // Base is usually 4.0 for players.
            attackSpeedAttr.setBaseValue(4.0 * stats.getAttackSpeedPercent());
        }

        // Apply perk effects
        if (profile.getPlayerPerk() != null) profile.getPlayerPerk().applyEffects(player);
    }

    private void clearEffects(Player player) {
        player.removePotionEffect(PotionEffectType.INCREASE_DAMAGE);
        player.removePotionEffect(PotionEffectType.SPEED);
        
        // Reset speed attribute to default
        AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.setBaseValue(0.1);
        player.setWalkSpeed(0.2f);
    }

    /* ===================== */
    /* MOB NAME (HP) */
    /* ===================== */

    public void updateMobName(LivingEntity mob) {
        double hp = mob.getHealth();
        AttributeInstance healthAttr = mob.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double max = (healthAttr != null) ? healthAttr.getBaseValue() : hp;
        mob.setCustomName(ChatColor.RED + "❤ " + String.format("%.1f", hp) + "/" + String.format("%.1f", max));
    }

    public boolean isArenaMob(org.bukkit.entity.Entity entity) {
        return arenaMobs.contains(entity);
    }

    public boolean isArenaPlayer(Player player) {
        return players.contains(player.getUniqueId());
    }

    public Set<LivingEntity> getArenaMobs() {
        return arenaMobs;
    }

    public void addFireTrail(Location loc) {
        fireTrails.put(loc.getBlock().getLocation(), System.currentTimeMillis() + 2000);
    }

    public Map<Location, Long> getFireTrails() {
        return fireTrails;
    }

    /* ===================== */
    /* LOCATIONS */
    /* ===================== */

    private void loadLocations() {
        playerSpawn = getLoc("arena.player-spawn");
        mobSpawn = getLoc("arena.mob-spawn");
        lobbySpawn = getLoc("arena.lobby");
    }

    private Location getLoc(String path) {
        String worldName = plugin.getConfig().getString(path + ".world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("Мир " + worldName + " не найден для " + path);
            return null;
        }
        double x = plugin.getConfig().getDouble(path + ".x");
        double y = plugin.getConfig().getDouble(path + ".y");
        double z = plugin.getConfig().getDouble(path + ".z");
        return new Location(world, x, y, z);
    }

    /* ===================== */
    /* UTILS */
    /* ===================== */

    private void broadcast(String msg) {
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) p.sendMessage(msg);
        }
    }
}
