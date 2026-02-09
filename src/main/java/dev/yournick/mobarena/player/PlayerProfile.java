package dev.yournick.mobarena.player;

import dev.yournick.mobarena.amulet.Amulet;
import dev.yournick.mobarena.amulet.AmuletRarity;
import dev.yournick.mobarena.amulet.AmuletType;
import dev.yournick.mobarena.perk.Perk;
import dev.yournick.mobarena.perk.PlayerPerk;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerProfile {

    private final UUID uuid;
    private final PlayerStats stats;

    private PlayerPerk playerPerk;

    private int gold;
    private int shards;
    private int bestWave;
    private int specialPigsKilled;
    private double bonusHealth;
    private double waveHealthMultiplier = 1.0;
    private double bonusSpeed;
    private int strengthLevel;

    private final Map<Perk, PerkProfile> perkProfiles = new ConcurrentHashMap<>();

    // Амулеты
    private final Map<AmuletType, Integer> ownedAmulets = new ConcurrentHashMap<>(); // Тип -> Уровень
    private final AmuletType[] equippedAmulets = new AmuletType[4];
    private final Map<AmuletRarity, Integer> amuletCurrency = new ConcurrentHashMap<>();

    // Гача счетчики
    private int gachaTotalRolls = 0;
    private int rollsSinceLastEpic = 0;
    private int rollsSinceLastLegendary = 0;

    // Перки: дополнительные свойства (теперь всё в PlayerStats)
    private boolean goldenVeinActive = false;
    private boolean avatarOfFlameActive = false;
    private boolean perfectDodgeActive = false;
    private boolean invulnerable = false;
    private boolean madBoardingActive = false;
    private boolean wheelOfFortuneActive = false;
    private boolean titanShieldActive = false;
    private boolean ghostFormActive = false;
    private boolean fireExplosionActive = false;
    private long regenDisabledUntil = 0;

    // Новые эффекты зачарований
    private boolean absoluteBarrierActive = false;
    private long absoluteBarrierCooldown = 0;
    private boolean secondLifeAvailable = true;
    private boolean shieldBearerActive = false;
    private long shieldBearerEndTime = 0;

    // Кузнец: временные баффы
    private boolean blacksmithArmorActive = false;
    private long blacksmithArmorEndTime = 0;
    private boolean blacksmithInBattleArmorActive = false;
    private long blacksmithInBattleArmorEndTime = 0;

    // Счётчики зачарований меча
    private boolean ruthlessnessActive = false;
    private int devourerCharges = 0;
    private int smashCounter = 0;

    // Счётчики способностей
    private int ironWillStacks = 0;
    private long lastIronWillHit = 0;
    private int agileCounterstrikeStacks = 0;
    private long lastAgileDodgeTime = 0;

    // Временные баффы от расходников
    private boolean steakBuffActive = false;
    private long hpPotionEndTime = 0;
    private long damagePotionEndTime = 0;
    private long speedPotionEndTime = 0;
    private long gAppleBuffEndTime = 0;

    // Кулдауны амулетов
    private final Map<AmuletType, Long> amuletCooldowns = new ConcurrentHashMap<>();
    private final Map<String, Long> abilityCooldowns = new ConcurrentHashMap<>();
    private int stormAttacks = 0;
    private long lastEternalFireAura = 0;

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
        this.stats = new PlayerStats();

        this.gold = 0;
        this.bonusHealth = 0;
        this.bonusSpeed = 0;
        this.strengthLevel = 0;

        this.playerPerk = new PlayerPerk(this);
    }

    public UUID getUuid() {
        return uuid;
    }

    public Player getPlayer() {
        return org.bukkit.Bukkit.getPlayer(uuid);
    }

    public PlayerStats getStats() {
        return stats;
    }

    private double applyDiminishingReturns(double raw, double cap) {
        if (raw <= 0) return 0;
        // Формула: x / (1 + x) приближается к 1.0 (100%)
        // Мы масштабируем это до капа.
        // Чтобы raw=1.0 давало примерно половину капа: cap * (1 / (1 + 1)) = 0.5 * cap
        return cap * (raw / (1.0 + raw));
    }

    public void recalculateStats() {
        stats.reset();

        // Сбор сырых данных
        Map<String, Double> raw = new HashMap<>();

        // 1. Применяем амулеты
        for (AmuletType type : equippedAmulets) {
            if (type != null && ownedAmulets.containsKey(type)) {
                int level = ownedAmulets.get(type);
                Amulet amulet = new Amulet(type, level);
                double multiplier = amulet.getStatMultiplier();

                for (Map.Entry<String, Double> entry : type.getBaseStats().entrySet()) {
                    raw.put(entry.getKey(), raw.getOrDefault(entry.getKey(), 0.0) + entry.getValue() * multiplier);
                }
            }
        }

        // 2. Покупки из магазина (Сила)
        raw.put("damagePercent", raw.getOrDefault("damagePercent", 0.0) + strengthLevel * 0.1);

        // 3. Зачарования
        Player player = getPlayer();
        if (player != null) {
            // Собираем только те предметы, которые надеты или в руке, чтобы избежать дублирования
            List<org.bukkit.inventory.ItemStack> itemsToProcess = new ArrayList<>();
            
            // Оружие в основной руке
            org.bukkit.inventory.ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (mainHand != null && mainHand.getType().name().contains("SWORD")) {
                itemsToProcess.add(mainHand);
            }
            
            // Броня в слотах брони
            for (org.bukkit.inventory.ItemStack armor : player.getInventory().getArmorContents()) {
                if (armor != null && isArmorMaterial(armor.getType())) {
                    itemsToProcess.add(armor);
                }
            }

            for (org.bukkit.inventory.ItemStack item : itemsToProcess) {
                if (hasEnchant(item, "Острота")) raw.put("damagePercent", raw.getOrDefault("damagePercent", 0.0) + 0.15);
                if (hasEnchant(item, "Критический удар")) raw.put("critDamage", raw.getOrDefault("critDamage", 0.0) + 0.75);
                if (hasEnchant(item, "Проклятие берсерка")) {
                    raw.put("damagePercent", raw.getOrDefault("damagePercent", 0.0) + 1.0);
                    raw.put("armorPercent", raw.getOrDefault("armorPercent", 0.0) - 0.25);
                }
                if (hasEnchant(item, "Живучесть")) raw.put("maxHealthPercent", raw.getOrDefault("maxHealthPercent", 0.0) + 0.20);
                if (hasEnchant(item, "Лёгкость")) raw.put("moveSpeedPercent", raw.getOrDefault("moveSpeedPercent", 0.0) + 0.20);
                if (hasEnchant(item, "Уклонение")) raw.put("dodgeChance", raw.getOrDefault("dodgeChance", 0.0) + 0.20);
                if (hasEnchant(item, "Огнестойкость")) raw.put("fireResistance", raw.getOrDefault("fireResistance", 0.0) + 0.40);
                if (hasEnchant(item, "Отражение")) raw.put("damageReflection", raw.getOrDefault("damageReflection", 0.0) + 0.15);
            }
        }

        // 4. Бонус брони от материала
        raw.put("armorPercent", raw.getOrDefault("armorPercent", 0.0) + calculateArmorMaterialBonus());

        // 5. Временные баффы от расходников
        double tempHpBonus = 0;
        if (isSteakBuffActive()) tempHpBonus = Math.max(tempHpBonus, 0.25);
        if (isHpPotionActive()) tempHpBonus = Math.max(tempHpBonus, 0.50);
        if (isGAppleBuffActive()) tempHpBonus = Math.max(tempHpBonus, 1.00);
        raw.put("maxHealthPercent", raw.getOrDefault("maxHealthPercent", 0.0) + tempHpBonus);

        if (isDamagePotionActive()) raw.put("damagePercent", raw.getOrDefault("damagePercent", 0.0) + 0.35);
        if (isSpeedPotionActive()) raw.put("moveSpeedPercent", raw.getOrDefault("moveSpeedPercent", 0.0) + 0.25);

        // --- Применяем статы с учетом систем сложения и убывания ---

        // Аддитивные статы (база 1.0 или 0.0)
        stats.setDamagePercent(1.0 + raw.getOrDefault("damagePercent", 0.0));
        stats.setMaxHealthPercent(1.0 + raw.getOrDefault("maxHealthPercent", 0.0));
        stats.setFireDamagePercent(1.0 + raw.getOrDefault("fireDamagePercent", 0.0));
        stats.setFireResistance(raw.getOrDefault("fireResistance", 0.0));
        stats.setHealOnKill(raw.getOrDefault("healOnKill", 0.0));
        stats.setRegenPower(1.0 + raw.getOrDefault("regenPower", 0.0));
        stats.setHealReceived(1.0 + raw.getOrDefault("healReceived", 0.0));
        stats.setMoveSpeedPercent(1.0 + raw.getOrDefault("moveSpeedPercent", 0.0));
        stats.setGoldGainPercent(1.0 + raw.getOrDefault("goldGainPercent", 0.0));
        stats.setAttackSpeedPercent(1.0 + raw.getOrDefault("attackSpeed", 0.0));
        stats.setDamageReflection(raw.getOrDefault("damageReflection", 0.0));
        stats.setCritDamage(1.5 + raw.getOrDefault("critDamage", 0.0)); // База 1.5 (150%)
        stats.setDeathSaveChance(raw.getOrDefault("deathSaveChance", 0.0));

        // Отражение от перка Стойкий (Mini 1)
        if (playerPerk != null && playerPerk.getPerk() == Perk.TOUGH) {
            stats.addDamageReflection(getPerkProfile(Perk.TOUGH).getMini1() * 0.03);
        }

        // Статы с убывающей полезностью
        stats.setCritChance(applyDiminishingReturns(raw.getOrDefault("critChance", 0.0), 0.8));
        stats.setLifesteal(applyDiminishingReturns(raw.getOrDefault("lifesteal", 0.0), 0.75));
        stats.setArmorPercent(applyDiminishingReturns(raw.getOrDefault("armorPercent", 0.0), 0.85));
        stats.setDodgeChance(applyDiminishingReturns(raw.getOrDefault("dodgeChance", 0.0), 0.9));
        stats.setKnockbackResistance(applyDiminishingReturns(raw.getOrDefault("knockbackResistance", 0.0), 0.9));

        // 5. ПРИМЕНЯЕМ ПЕРК (ПОСЛЕ ЛИМИТОВ)
        if (playerPerk != null) {
            playerPerk.applyStats(stats);
        }

        // Контрудар (Ловкий Мини 2)
        if (agileCounterstrikeStacks > 0) {
            stats.addDamagePercent(agileCounterstrikeStacks * 0.5);
        }

        // Палач (Ловкий Крупный 3)
        if (player != null && playerPerk.getPerk() == Perk.AGILE && getPerkProfile(Perk.AGILE).hasLarge3()) {
            long slowedMobs = player.getNearbyEntities(3, 3, 3).stream()
                    .filter(e -> e instanceof LivingEntity && ((LivingEntity) e).hasPotionEffect(PotionEffectType.SLOW))
                    .count();
            stats.addDamagePercent(slowedMobs * 0.5);
        }

        // Принудительные лимиты (Caps)
        stats.setCritChance(Math.min(0.8, stats.getCritChance()));
        stats.setLifesteal(Math.min(0.75, stats.getLifesteal()));
        stats.setArmorPercent(Math.min(0.85, stats.getArmorPercent()));
        stats.setDodgeChance(Math.min(0.9, stats.getDodgeChance()));
        stats.setKnockbackResistance(Math.min(0.9, stats.getKnockbackResistance()));

        // 6. Бонусы волн (перемножение на макс хп)
        stats.setMaxHealthPercent(stats.getMaxHealthPercent() * waveHealthMultiplier);

        // По просьбе пользователя: Защита от огня (fireResistance) суммируется, 
        // но в статистике мы должны видеть её корректно.
        // Сейчас она аддитивно суммируется в raw.getOrDefault("fireResistance", 0.0).
    }

    public List<org.bukkit.inventory.ItemStack> getStatBreakdown(String statKey) {
        List<org.bukkit.inventory.ItemStack> breakdown = new ArrayList<>();
        Player player = getPlayer();

        // 1. Базовые значения
        if (isBaseStat(statKey)) {
            breakdown.add(createSourceItem(org.bukkit.Material.PAPER, "§7Базовое значение", "§f" + getBaseValueStr(statKey)));
        }

        // 2. Амулеты
        for (AmuletType type : equippedAmulets) {
            if (type != null && ownedAmulets.containsKey(type)) {
                int level = ownedAmulets.get(type);
                Amulet amulet = new Amulet(type, level);
                Double baseValue = type.getBaseStats().get(statKey);
                if (baseValue != null) {
                    double val = baseValue * amulet.getStatMultiplier();
                    breakdown.add(createSourceItem(type.getMaterial(), "§6Амулет: §f" + type.getDisplayName(), "§a" + formatValue(val, statKey)));
                }
            }
        }

        // 3. Сила (Апгрейды магазина)
        if (statKey.equals("damagePercent") && strengthLevel > 0) {
            breakdown.add(createSourceItem(org.bukkit.Material.BLAZE_POWDER, "§eАпгрейд силы", "§a+" + String.format("%.1f", (double)(strengthLevel * 10)) + "%"));
        }

        // 4. Зачарования и Броня
        if (player != null) {
            // Зачарования на мече
            org.bukkit.inventory.ItemStack sword = player.getInventory().getItemInMainHand();
            if (sword != null && sword.getType().name().contains("SWORD")) {
                if (statKey.equals("damagePercent") && hasEnchant(sword, "Острота")) 
                    breakdown.add(createSourceItem(sword.getType(), "§bОстрота (Меч)", "§a+15.0%"));
                if (statKey.equals("critDamage") && hasEnchant(sword, "Критический удар")) 
                    breakdown.add(createSourceItem(sword.getType(), "§bКрит. удар (Меч)", "§a+75.0%"));
                if (statKey.equals("damagePercent") && hasEnchant(sword, "Проклятие берсерка")) 
                    breakdown.add(createSourceItem(sword.getType(), "§cПроклятие берсерка", "§a+100.0%"));
                if (statKey.equals("armorPercent") && hasEnchant(sword, "Проклятие берсерка")) 
                    breakdown.add(createSourceItem(sword.getType(), "§cПроклятие берсерка", "§c-25.0%"));
            }

            // Броня и её зачарования
            for (org.bukkit.inventory.ItemStack item : player.getInventory().getArmorContents()) {
                if (item == null || item.getType() == org.bukkit.Material.AIR) continue;
                
                // Материал
                if (statKey.equals("armorPercent")) {
                    double matBonus = getMaterialBonus(item);
                    if (matBonus > 0) 
                        breakdown.add(createSourceItem(item.getType(), "§3" + getMaterialName(item), "§a+" + String.format("%.1f", matBonus * 100) + "%"));
                }

                // Зачарования брони
                if (statKey.equals("maxHealthPercent") && hasEnchant(item, "Живучесть")) 
                    breakdown.add(createSourceItem(item.getType(), "§bЖивучесть (" + getSlotName(item) + ")", "§a+20.0%"));
                if (statKey.equals("moveSpeedPercent") && hasEnchant(item, "Лёгкость")) 
                    breakdown.add(createSourceItem(item.getType(), "§bЛёгкость (" + getSlotName(item) + ")", "§a+20.0%"));
                if (statKey.equals("dodgeChance") && hasEnchant(item, "Уклонение")) 
                    breakdown.add(createSourceItem(item.getType(), "§bУклонение (" + getSlotName(item) + ")", "§a+20.0%"));
                if (statKey.equals("fireResistance") && hasEnchant(item, "Огнестойкость")) 
                    breakdown.add(createSourceItem(item.getType(), "§bОгнестойкость (" + getSlotName(item) + ")", "§a+40.0%"));
                if (statKey.equals("damageReflection") && hasEnchant(item, "Отражение")) 
                    breakdown.add(createSourceItem(item.getType(), "§bОтражение (" + getSlotName(item) + ")", "§a+15.0%"));
            }
        }

        // 5. Перки
        if (playerPerk != null && playerPerk.getPerk() != null) {
            String perkDesc = playerPerk.getStatBonusDescription(statKey);
            if (perkDesc != null) {
                breakdown.add(createSourceItem(playerPerk.getPerk().getIcon(), "§5Перк: " + playerPerk.getPerk().getDisplayName(), "§f" + perkDesc));
            }
        }

        // 6. Бонус волн
        if (statKey.equals("maxHealthPercent") && waveHealthMultiplier > 1.0) {
            breakdown.add(createSourceItem(org.bukkit.Material.NETHER_STAR, "§dБонус от волн", "§a+" + String.format("%.1f", (waveHealthMultiplier - 1.0) * 100) + "% (мультипликатор)"));
        }

        if (breakdown.isEmpty()) {
            breakdown.add(createSourceItem(org.bukkit.Material.BARRIER, "§8Источники отсутствуют", ""));
        }

        return breakdown;
    }

    private org.bukkit.inventory.ItemStack createSourceItem(org.bukkit.Material mat, String name, String value) {
        org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(mat);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (!value.isEmpty()) {
                List<String> lore = new ArrayList<>();
                lore.add("§7Бонус: §f" + value);
                meta.setLore(lore);
            }
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isBaseStat(String key) {
        return key.equals("damagePercent") || key.equals("critDamage") || key.equals("maxHealthPercent") ||
               key.equals("moveSpeedPercent") || key.equals("goldGainPercent") || key.equals("attackSpeed") ||
               key.equals("regenPower") || key.equals("healReceived") || key.equals("fireDamagePercent");
    }

    private String getBaseValueStr(String key) {
        if (key.equals("critDamage")) return "150%";
        return "100%";
    }

    private String formatValue(double val, String key) {
        boolean isPercent = AmuletType.isPercent(key);
        String sign = val >= 0 ? "+" : "";
        if (isPercent) return sign + String.format("%.1f%%", val * 100);
        return sign + String.format("%.1f HP", val);
    }

    private double getMaterialBonus(org.bukkit.inventory.ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
            if (name.contains("Элитная броня")) return 0.09;
            if (name.contains("Героическая броня")) return 0.10;
            if (name.contains("Мифическая броня")) return 0.125;
        }
        String name = item.getType().name();
        if (name.startsWith("LEATHER_")) return 0.025;
        else if (name.startsWith("CHAINMAIL_")) return 0.04;
        else if (name.startsWith("IRON_")) return 0.05;
        else if (name.startsWith("DIAMOND_")) return 0.07;
        return 0;
    }

    private String getMaterialName(org.bukkit.inventory.ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            return ChatColor.stripColor(item.getItemMeta().getDisplayName());
        }
        return item.getType().name();
    }

    private String getSlotName(org.bukkit.inventory.ItemStack item) {
        String n = item.getType().name();
        if (n.contains("HELMET")) return "Шлем";
        if (n.contains("CHESTPLATE")) return "Нагрудник";
        if (n.contains("LEGGINGS")) return "Поножи";
        if (n.contains("BOOTS")) return "Ботинки";
        return "Броня";
    }

    private boolean isArmorMaterial(org.bukkit.Material mat) {
        String n = mat.name();
        return n.contains("HELMET") || n.contains("CHESTPLATE") || n.contains("LEGGINGS") || n.contains("BOOTS");
    }

    private boolean isEquipped(Player player, org.bukkit.inventory.ItemStack item) {
        for (org.bukkit.inventory.ItemStack armor : player.getInventory().getArmorContents()) {
            if (item.equals(armor)) return true;
        }
        return false;
    }

    private boolean hasEnchant(org.bukkit.inventory.ItemStack item, String enchantName) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) return false;
        for (String line : item.getItemMeta().getLore()) {
            if (line.contains(enchantName)) return true;
        }
        return false;
    }

    private double calculateArmorMaterialBonus() {
        Player player = getPlayer();
        if (player == null) return 0;
        double total = 0;
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getArmorContents()) {
            if (item == null || item.getType() == org.bukkit.Material.AIR) continue;
            
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
                if (name.contains("Элитная броня")) { total += 0.09; continue; }
                if (name.contains("Героическая броня")) { total += 0.10; continue; }
                if (name.contains("Мифическая броня")) { total += 0.125; continue; }
            }

            String name = item.getType().name();
            if (name.startsWith("LEATHER_")) total += 0.025;
            else if (name.startsWith("CHAINMAIL_")) total += 0.04;
            else if (name.startsWith("IRON_")) total += 0.05;
            else if (name.startsWith("DIAMOND_")) total += 0.07;
        }

        // Бонус от щита (+15% брони)
        org.bukkit.inventory.ItemStack offHand = player.getInventory().getItemInOffHand();
        if (offHand != null && offHand.getType() == org.bukkit.Material.SHIELD) {
            total += 0.15;
        }

        return total;
    }

    private void applyAmuletStat(String statName, double value) {
        switch (statName) {
            case "damagePercent": stats.addDamagePercent(value); break;
            case "maxHealthPercent": stats.addMaxHealthPercent(value); break;
            case "moveSpeedPercent": stats.addMoveSpeedPercent(value); break;
            case "goldGainPercent": stats.addGoldGainPercent(value); break;
            case "armorPercent": stats.addArmorPercent(value); break;
            case "critChance": stats.addCritChance(value); break;
            case "critDamage": stats.addCritDamage(value); break;
            case "dodgeChance": stats.addDodgeChance(value); break;
            case "lifesteal": stats.addLifesteal(value); break;
            case "healOnKill": stats.addHealOnKill(value); break;
            case "regenPower": stats.addRegenPower(value); break;
            case "healReceived": stats.addHealReceived(value); break;
            case "doubleDropChance": stats.addDoubleDropChance(value); break;
            case "rareDropChance": stats.addRareDropChance(value); break;
            case "deathSaveChance": stats.addDeathSaveChance(value); break;
            case "fireDamagePercent": stats.addFireDamagePercent(value); break;
            case "fireResistance": stats.addFireResistance(value); break;
            case "attackSpeed": stats.addAttackSpeedPercent(value); break;
        }
    }

    // ===== Амулеты Getters/Setters =====
    public Map<AmuletType, Integer> getOwnedAmulets() { return ownedAmulets; }
    public AmuletType[] getEquippedAmulets() { return equippedAmulets; }
    public Map<AmuletRarity, Integer> getAmuletCurrency() { return amuletCurrency; }
    
    public int getCurrency(AmuletRarity rarity) {
        return amuletCurrency.getOrDefault(rarity, 0);
    }
    
    public void addCurrency(AmuletRarity rarity, int amount) {
        amuletCurrency.put(rarity, getCurrency(rarity) + amount);
    }

    public int getGachaTotalRolls() { return gachaTotalRolls; }
    public void setGachaTotalRolls(int rolls) { this.gachaTotalRolls = rolls; }
    
    public int getRollsSinceLastEpic() { return rollsSinceLastEpic; }
    public void setRollsSinceLastEpic(int rolls) { this.rollsSinceLastEpic = rolls; }
    
    public int getRollsSinceLastLegendary() { return rollsSinceLastLegendary; }
    public void setRollsSinceLastLegendary(int rolls) { this.rollsSinceLastLegendary = rolls; }

    // ===== Золото =====
    public int getGold() {
        return gold;
    }

    public void addGold(int amount) {
        gold += amount;
    }

    public int getBestWave() {
        return bestWave;
    }

    public void setBestWave(int bestWave) {
        this.bestWave = bestWave;
    }

    public int getSpecialPigsKilled() {
        return specialPigsKilled;
    }

    public void setSpecialPigsKilled(int specialPigsKilled) {
        this.specialPigsKilled = specialPigsKilled;
    }

    public void addSpecialPigKill() {
        this.specialPigsKilled++;
    }

    public boolean removeGold(int amount) {
        if (gold < amount) return false;
        gold -= amount;
        return true;
    }

    // ===== Осколки =====
    public int getShards() {
        return shards;
    }

    public void addShards(int amount) {
        shards += amount;
    }

    public boolean removeShards(int amount) {
        if (shards < amount) return false;
        shards -= amount;
        return true;
    }

    // ===== Покупки =====
    public boolean buyHealthUpgrade(int cost, double value) {
        if (!removeGold(cost)) return false;
        bonusHealth += value;
        return true;
    }

    public boolean buySpeedUpgrade(int cost, double value) {
        if (!removeGold(cost)) return false;
        bonusSpeed += value;
        return true;
    }

    public boolean buyStrengthUpgrade(int cost) {
        if (!removeGold(cost)) return false;
        strengthLevel += 1;
        return true;
    }

    // ===== Бонусы =====
    public double getBonusHealth() {
        return bonusHealth;
    }

    public void setBonusHealth(double bonusHealth) {
        this.bonusHealth = bonusHealth;
    }

    public double getWaveHealthMultiplier() {
        return waveHealthMultiplier;
    }

    public void setWaveHealthMultiplier(double waveHealthMultiplier) {
        this.waveHealthMultiplier = waveHealthMultiplier;
    }

    public void addWaveHealthBonus() {
        this.waveHealthMultiplier += 0.1;
    }

    public double getBonusSpeed() {
        return bonusSpeed;
    }

    public void setBonusSpeed(double bonusSpeed) {
        this.bonusSpeed = bonusSpeed;
    }

    public int getStrengthLevel() {
        return strengthLevel;
    }

    public void setStrengthLevel(int strengthLevel) {
        this.strengthLevel = strengthLevel;
    }

    public PerkProfile getPerkProfile(Perk perk) {
        return perkProfiles.computeIfAbsent(perk, k -> new PerkProfile());
    }

    public void addPerkXp(Perk perk, int amount) {
        getPerkProfile(perk).addXp(amount);
    }

    // ===== Перки =====
    public PlayerPerk getPlayerPerk() {
        if (playerPerk == null) {
            playerPerk = new PlayerPerk(this);
        }
        return playerPerk;
    }

    public boolean isGoldenVeinActive() { return goldenVeinActive; }
    public void setGoldenVeinActive(boolean goldenVeinActive) { this.goldenVeinActive = goldenVeinActive; }

    public boolean isAvatarOfFlameActive() { return avatarOfFlameActive; }
    public void setAvatarOfFlameActive(boolean avatarOfFlameActive) { this.avatarOfFlameActive = avatarOfFlameActive; }

    public boolean isPerfectDodgeActive() { return perfectDodgeActive; }
    public void setPerfectDodgeActive(boolean perfectDodgeActive) { this.perfectDodgeActive = perfectDodgeActive; }

    public boolean isInvulnerable() { return invulnerable; }
    public void setInvulnerable(boolean invulnerable) { this.invulnerable = invulnerable; }

    public boolean isMadBoardingActive() { return madBoardingActive; }
    public void setMadBoardingActive(boolean madBoardingActive) { this.madBoardingActive = madBoardingActive; }

    public boolean isWheelOfFortuneActive() { return wheelOfFortuneActive; }
    public void setWheelOfFortuneActive(boolean wheelOfFortuneActive) { this.wheelOfFortuneActive = wheelOfFortuneActive; }

    public boolean isTitanShieldActive() { return titanShieldActive; }
    public void setTitanShieldActive(boolean titanShieldActive) { this.titanShieldActive = titanShieldActive; }

    public boolean isGhostFormActive() { return ghostFormActive; }
    public void setGhostFormActive(boolean ghostFormActive) { this.ghostFormActive = ghostFormActive; }

    public boolean isFireExplosionActive() { return fireExplosionActive; }
    public void setFireExplosionActive(boolean fireExplosionActive) { this.fireExplosionActive = fireExplosionActive; }

    public long getRegenDisabledUntil() { return regenDisabledUntil; }
    public void setRegenDisabledUntil(long regenDisabledUntil) { this.regenDisabledUntil = regenDisabledUntil; }

    public boolean isAbsoluteBarrierActive() { return absoluteBarrierActive; }
    public void setAbsoluteBarrierActive(boolean absoluteBarrierActive) { this.absoluteBarrierActive = absoluteBarrierActive; }
    public long getAbsoluteBarrierCooldown() { return absoluteBarrierCooldown; }
    public void setAbsoluteBarrierCooldown(long absoluteBarrierCooldown) { this.absoluteBarrierCooldown = absoluteBarrierCooldown; }
    public boolean isSecondLifeAvailable() { return secondLifeAvailable; }
    public void setSecondLifeAvailable(boolean secondLifeAvailable) { this.secondLifeAvailable = secondLifeAvailable; }
    public boolean isShieldBearerActive() {
        if (shieldBearerActive && System.currentTimeMillis() > shieldBearerEndTime) shieldBearerActive = false;
        return shieldBearerActive;
    }
    public void activateShieldBearer(int seconds) {
        this.shieldBearerActive = true;
        this.shieldBearerEndTime = System.currentTimeMillis() + (seconds * 1000L);
    }

    public boolean isBlacksmithArmorActive() {
        if (blacksmithArmorActive && System.currentTimeMillis() > blacksmithArmorEndTime) blacksmithArmorActive = false;
        return blacksmithArmorActive;
    }
    public void activateBlacksmithArmor(int seconds) {
        this.blacksmithArmorActive = true;
        this.blacksmithArmorEndTime = System.currentTimeMillis() + (seconds * 1000L);
    }

    public boolean isBlacksmithInBattleArmorActive() {
        if (blacksmithInBattleArmorActive && System.currentTimeMillis() > blacksmithInBattleArmorEndTime) blacksmithInBattleArmorActive = false;
        return blacksmithInBattleArmorActive;
    }
    public void activateBlacksmithInBattleArmor(int seconds) {
        this.blacksmithInBattleArmorActive = true;
        this.blacksmithInBattleArmorEndTime = System.currentTimeMillis() + (seconds * 1000L);
    }

    public boolean isRuthlessnessActive() { return ruthlessnessActive; }
    public void setRuthlessnessActive(boolean active) { this.ruthlessnessActive = active; }
    public int getDevourerCharges() { return devourerCharges; }
    public void setDevourerCharges(int charges) { this.devourerCharges = Math.max(0, charges); }
    public int getSmashCounter() { return smashCounter; }
    public void setSmashCounter(int val) { this.smashCounter = val; }

    public int getIronWillStacks() {
        if (System.currentTimeMillis() - lastIronWillHit > 10000) ironWillStacks = 0;
        return ironWillStacks;
    }
    public void addIronWillStack() {
        this.ironWillStacks = Math.min(getIronWillStacks() + 1, 7);
        this.lastIronWillHit = System.currentTimeMillis();
    }

    public int getAgileCounterstrikeStacks() {
        if (System.currentTimeMillis() - lastAgileDodgeTime > 10000) agileCounterstrikeStacks = 0;
        return agileCounterstrikeStacks;
    }

    public void addAgileCounterstrikeStack() {
        this.agileCounterstrikeStacks = Math.min(getAgileCounterstrikeStacks() + 1, 3);
        this.lastAgileDodgeTime = System.currentTimeMillis();
        recalculateStats();
    }

    public void resetAgileCounterstrike() {
        this.agileCounterstrikeStacks = 0;
        this.lastAgileDodgeTime = 0;
        recalculateStats();
    }

    public void resetPerkState() {
        this.goldenVeinActive = false;
        this.avatarOfFlameActive = false;
        this.perfectDodgeActive = false;
        this.invulnerable = false;
        this.madBoardingActive = false;
        this.wheelOfFortuneActive = false;
        this.titanShieldActive = false;
        this.ghostFormActive = false;
        this.fireExplosionActive = false;
        
        this.ironWillStacks = 0;
        this.lastIronWillHit = 0;
        this.agileCounterstrikeStacks = 0;
        this.lastAgileDodgeTime = 0;
    }

    // ===== Кулдауны способностей =====
    public boolean isAbilityOnCooldown(String key) {
        return abilityCooldowns.containsKey(key) && abilityCooldowns.get(key) > System.currentTimeMillis();
    }

    public void setAbilityCooldown(String key, int seconds) {
        abilityCooldowns.put(key, System.currentTimeMillis() + (seconds * 1000L));
    }

    public long getAbilityCooldownRemaining(String key) {
        if (!isAbilityOnCooldown(key)) return 0;
        return (abilityCooldowns.get(key) - System.currentTimeMillis()) / 1000;
    }

    // ===== Легендарные амулеты =====
    public boolean isOnCooldown(AmuletType type) {
        return amuletCooldowns.containsKey(type) && amuletCooldowns.get(type) > System.currentTimeMillis();
    }

    public void setCooldown(AmuletType type, int seconds) {
        amuletCooldowns.put(type, System.currentTimeMillis() + (seconds * 1000L));
    }

    public int getStormAttacks() {
        return stormAttacks;
    }

    public void setStormAttacks(int stormAttacks) {
        this.stormAttacks = stormAttacks;
    }

    public long getLastEternalFireAura() {
        return lastEternalFireAura;
    }

    public void setLastEternalFireAura(long lastEternalFireAura) {
        this.lastEternalFireAura = lastEternalFireAura;
    }

    public boolean hasAmuletEquipped(AmuletType type) {
        for (AmuletType eq : equippedAmulets) {
            if (eq == type) return true;
        }
        return false;
    }

    // Методы временных баффов
    public void activateSteakBuff() { this.steakBuffActive = true; recalculateStats(); }
    public void activateHpPotion(int seconds) { this.hpPotionEndTime = System.currentTimeMillis() + seconds * 1000L; recalculateStats(); }
    public void activateDamagePotion(int seconds) { this.damagePotionEndTime = System.currentTimeMillis() + seconds * 1000L; recalculateStats(); }
    public void activateSpeedPotion(int seconds) { this.speedPotionEndTime = System.currentTimeMillis() + seconds * 1000L; recalculateStats(); }
    public void activateGAppleBuff(int seconds) { this.gAppleBuffEndTime = System.currentTimeMillis() + seconds * 1000L; recalculateStats(); }

    public boolean isSteakBuffActive() { return steakBuffActive; }
    public boolean isHpPotionActive() { return System.currentTimeMillis() < hpPotionEndTime; }
    public boolean isDamagePotionActive() { return System.currentTimeMillis() < damagePotionEndTime; }
    public boolean isSpeedPotionActive() { return System.currentTimeMillis() < speedPotionEndTime; }
    public boolean isGAppleBuffActive() { return System.currentTimeMillis() < gAppleBuffEndTime; }
}
