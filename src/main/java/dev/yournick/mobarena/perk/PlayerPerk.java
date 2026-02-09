package dev.yournick.mobarena.perk;

import dev.yournick.mobarena.player.PerkProfile;
import dev.yournick.mobarena.player.PlayerProfile;
import dev.yournick.mobarena.player.PlayerStats;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerPerk {

    private Perk perk;
    private final PlayerProfile profile;
    private boolean agileCounterattack = false;

    public PlayerPerk(PlayerProfile profile) {
        this.profile = profile;
    }

    public Perk getPerk() {
        return perk;
    }

    public boolean isAgileCounterattack() {
        return agileCounterattack;
    }

    public void setAgileCounterattack(boolean agileCounterattack) {
        this.agileCounterattack = agileCounterattack;
    }

    public void setPerk(Perk perk) {
        this.perk = perk;
        this.agileCounterattack = false;
        profile.resetPerkState();
        profile.recalculateStats();
        Player player = profile.getPlayer();
        if (player != null) {
            applyEffects(player);
            // Обновляем HP и Скорость в ArenaManager
            dev.yournick.mobarena.MobArenaPlugin.getInstance().getArenaManager().applyPlayerEffects(player);
        }
    }

    public void applyStats(PlayerStats stats) {
        if (perk == null) return;

        PerkProfile pp = profile.getPerkProfile(perk);

        switch (perk) {
            case LUCKY:
                stats.addGoldGainPercent(0.40 + pp.getMini1() * 0.10); // +40% золота + 10% за уровень
                stats.addLuck(0.50);            // Базовая удача
                stats.addDoubleDropChance(0.10); // 10% шанс двойного дропа
                stats.addRareDropChance(0.05 + pp.getMini2() * 0.03);   // 5% + 3% за уровень
                stats.addCritChance(pp.getMini3() * 0.04);              // +4% за уровень

                // Second Chance (Large 2)
                if (pp.hasLarge2()) {
                    stats.addDeathSaveChance(0.15);
                }

                // Wheel of Fortune (Unique Active)
                if (profile.isWheelOfFortuneActive()) {
                    stats.setCritChance(1.0);
                    stats.setDoubleDropChance(1.0);
                }
                break;
            case TOUGH:
                stats.addArmorPercent(0.20);     // -20% входящего урона (вместо Resistance I)
                stats.addMoveSpeedPercent(-0.15); // -15% скорости (вместо Slowness I)
                stats.addMaxHealthPercent(0.20);  // +20% HP база
                stats.addMaxHealthPercent(pp.getMini1() * 0.04);
                stats.addDamageReflection(pp.getMini2() * 0.03);
                stats.addHealReceived(0.25);      // +25% входящего лечения
                break;
            case AGILE:
                stats.addMoveSpeedPercent(0.20);  // +20% скорости (вместо Speed I)
                stats.addDodgeChance(0.35 + pp.getMini1() * 0.03); // Уклонение
                stats.addCritChance(0.15);         // +15% Шанс крита база
                stats.addDamagePercent(pp.getMini3() * 0.03);
                break;
            case FIREBORN:
                stats.addFireResistance(1.0);      // Полный иммунитет к огню
                stats.addLifesteal(pp.getMini1() * 0.03); // +3% вампиризма за уровень
                stats.addFireDamagePercent(0.10);  // +10% урона огнем база
                stats.addFireDamagePercent(pp.getMini2() * 0.05);
                stats.addBurnDurationPercent(pp.getMini3() * 0.1);
                break;
            case PIRATE:
                stats.addDamagePercent(0.15);      // +15% урона база
                stats.addLifesteal(0.05);          // 5% Вампиризм база
                stats.addRegenPower(0.20);         // +20% к регенерации
                
                // Морской волк (Крупный 3) убирает штраф
                if (!pp.hasLarge3()) {
                    stats.addArmorPercent(-0.10);  // ШТРАФ: +10% входящего урона
                }

                stats.addMoveSpeedPercent(pp.getMini2() * 0.05);
                stats.addGoldGainPercent(pp.getMini3() * 0.05);

                // Грабёж (Крупный 2)
                if (pp.hasLarge2()) {
                    stats.addDoubleDropChance(0.25);
                }

                // Пьяная ярость (Крупный 1) - Динамический бонус от HP
                if (pp.hasLarge1()) {
                    Player p = profile.getPlayer();
                    if (p != null) {
                        double hpPercent = p.getHealth() / p.getMaxHealth();
                        if (hpPercent < 0.25) stats.addDamagePercent(0.40);      // +40%
                        else if (hpPercent < 0.75) stats.addDamagePercent(0.20); // +20%
                    }
                }

                // Безумный абордаж (Unique Active)
                if (profile.isMadBoardingActive()) {
                    stats.addDamagePercent(0.60);  // Вместо Strength III (+60% урона)
                    stats.addRegenPower(0.20);    // Доп регенерация
                }
                break;
            case BLACKSMITH:
                stats.addArmorPercent(0.10);       // +10% брони база
                stats.setSellPricePercent(1.0 - (35 + pp.getMini1() * 3) / 100.0);
                stats.addArmorPercent(pp.getMini2() * 0.02);
                stats.addKnockbackResistance(0.20); // +20% сопр. откидыванию база
                break;
            case TRADER:
                stats.addGoldGainPercent(-0.20);   // Штраф золота за убийство
                stats.setSellPricePercent(1.0 + (50 + pp.getMini1() * 5) / 100.0);
                stats.addGoldGainPercent(pp.getMini2() * 0.04);
                stats.addDropChance(0.20);         // +20% шанс дропа база
                stats.addLuck(0.30);               // +30% Удачи база
                stats.addRareDropChance(0.03);     // 3% шанс редкого дропа
                break;
        }
    }

    public void applyEffects(Player player) {
        if (player == null) return;

        // Снимаем ванильные эффекты, если они остались (ранее наложенные)
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Очищаем уникальные предметы из всего инвентаря
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (isUniqueItem(inv.getItem(i))) inv.setItem(i, null);
        }
        ItemStack[] armor = inv.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (isUniqueItem(armor[i])) armor[i] = null;
        }
        inv.setArmorContents(armor);
        
        if (isUniqueItem(inv.getItemInOffHand())) inv.setItemInOffHand(null);
        if (isUniqueItem(player.getItemOnCursor())) player.setItemOnCursor(null);

        // Обновляем зачарование на оружии (снимет, если перка нет или он не FIREBORN)
        updateWeaponEnchant(player);

        if (perk == null) return;

        // Ставим дополнительные свойства (теперь всё в applyStats)
        // Даем предмет уникальной способности
        giveUniqueItem(player);
    }

    public static boolean isUniqueItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        String name = item.getItemMeta().getDisplayName();
        return name.contains("Колесо Фортуны") ||
               name.contains("Золотая жила") ||
               name.contains("Аватар пламени") ||
               name.contains("Идеальное уклонение") ||
               name.contains("Последний рубеж") ||
               name.contains("Безумный абордаж");
    }

    public void giveUniqueItem(Player player) {
        if (player == null || perk == null) return;
        PerkProfile pp = profile.getPerkProfile(perk);
        if (!pp.hasUnique()) return;

        Material mat = null;
        String name = null;
        switch (perk) {
            case LUCKY: mat = Material.GOLDEN_APPLE; name = "§6Колесо Фортуны"; break;
            case TRADER: mat = Material.GOLD_NUGGET; name = "§6Золотая жила"; break;
            case FIREBORN: mat = Material.BLAZE_ROD; name = "§cАватар пламени"; break;
            case AGILE: mat = Material.FEATHER; name = "§fИдеальное уклонение"; break;
            case PIRATE: mat = Material.GOLD_INGOT; name = "§eБезумный абордаж"; break;
            case BLACKSMITH: return; // Живая броня пассивная
            case TOUGH: return; // Последний рубеж пассивный
        }

        if (mat != null) {
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                item.setItemMeta(meta);
            }
            // Кладём в 9-й слот (индекс 8)
            player.getInventory().setItem(8, item);
        }
    }

    public void updateWeaponEnchant(Player player) {
        if (player == null) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) return;

        // Проверяем, меч ли это
        if (!item.getType().name().contains("SWORD")) return;

        if (perk != null && perk.isFireEnchant()) {
            // Добавляем заговор огня 2
            if (item.getEnchantmentLevel(Enchantment.FIRE_ASPECT) < 2) {
                item.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
            }
        } else {
            // Убираем заговор огня, если он есть
            if (item.containsEnchantment(Enchantment.FIRE_ASPECT)) {
                // Только если это не было зачаровано игроком вручную (упрощенно: если перк FIREBORN активен, мы его накладываем)
                // Если нет — снимаем.
                item.removeEnchantment(Enchantment.FIRE_ASPECT);
            }
        }
    }

    public boolean tryDodge() {
        if (profile == null) return false;
        profile.recalculateStats();
        double chance = profile.getStats().getDodgeChance();
        
        return Math.random() < chance;
    }

    public int applyGoldBonus(int base) {
        if (profile == null) return base;
        profile.recalculateStats();
        double multiplier = profile.getStats().getGoldGainPercent();
        
        return (int) (base * multiplier);
    }

    public double getUpgradeDiscount() {
        if (perk != Perk.BLACKSMITH) return 1.0;
        // Кузнец имеет базовую скидку 35% + 3% за уровень.
        // Это хранится в sellPricePercent (костыльно, но пока так)
        // На самом деле лучше использовать отдельный стат или просто оставить логику здесь
        profile.recalculateStats();
        return profile.getStats().getSellPricePercent();
    }

    public boolean isUpgradeFree() {
        if (perk != Perk.BLACKSMITH) return false;
        PerkProfile pp = profile.getPerkProfile(Perk.BLACKSMITH);
        // Шанс: 3 * level %
        return Math.random() < (pp.getMini3() * 0.03);
    }

    public double getSellMultiplier() {
        if (profile == null) return 1.0;
        profile.recalculateStats();
        return profile.getStats().getSellPricePercent();
    }

    public boolean applyFireEnchant() {
        return perk != null && perk.isFireEnchant();
    }

    public String getStatBonusDescription(String statKey) {
        if (perk == null) return null;
        PerkProfile pp = profile.getPerkProfile(perk);
        
        switch (perk) {
            case LUCKY:
                if (statKey.equals("goldGainPercent")) return "+" + String.format("%.1f", (double)(40 + pp.getMini1() * 10)) + "%";
                if (statKey.equals("rareDropChance")) return "+" + String.format("%.1f", (double)(5 + pp.getMini2() * 3)) + "%";
                if (statKey.equals("critChance")) return "+" + String.format("%.1f", (double)(pp.getMini3() * 4)) + "%";
                if (statKey.equals("deathSaveChance") && pp.hasLarge2()) return "+15.0%";
                break;
            case TOUGH:
                if (statKey.equals("armorPercent")) return "+20.0%";
                if (statKey.equals("moveSpeedPercent")) return "-15.0%";
                if (statKey.equals("maxHealthPercent")) return "+" + String.format("%.1f", (double)(20 + pp.getMini1() * 4)) + "%";
                if (statKey.equals("damageReflection")) return "+" + String.format("%.1f", (double)(pp.getMini2() * 3)) + "%";
                if (statKey.equals("healReceived")) return "+25.0%";
                break;
            case AGILE:
                if (statKey.equals("moveSpeedPercent")) return "+20.0%";
                if (statKey.equals("dodgeChance")) return "+" + String.format("%.1f", (double)(35 + pp.getMini1() * 3)) + "%";
                if (statKey.equals("critChance")) return "+15.0%";
                if (statKey.equals("damagePercent")) return "+" + String.format("%.1f", (double)(pp.getMini3() * 3)) + "%";
                break;
            case FIREBORN:
                if (statKey.equals("fireResistance")) return "+100.0% (Иммунитет)";
                if (statKey.equals("lifesteal")) return "+" + String.format("%.1f", (double)(pp.getMini1() * 3)) + "%";
                if (statKey.equals("fireDamagePercent")) return "+" + String.format("%.1f", (double)(10 + pp.getMini2() * 5)) + "%";
                break;
            case PIRATE:
                if (statKey.equals("damagePercent")) return "+15.0% (база) + ярость";
                if (statKey.equals("lifesteal")) return "+5.0%";
                if (statKey.equals("regenPower")) return "+20.0%";
                if (statKey.equals("armorPercent")) return "-10.0% (Штраф)";
                if (statKey.equals("moveSpeedPercent")) return "+" + String.format("%.1f", (double)(pp.getMini2() * 5)) + "%";
                if (statKey.equals("goldGainPercent")) return "+" + String.format("%.1f", (double)(pp.getMini3() * 5)) + "%";
                break;
            case BLACKSMITH:
                if (statKey.equals("armorPercent")) return "+" + String.format("%.1f", (double)(10 + pp.getMini2() * 2)) + "%";
                if (statKey.equals("knockbackResistance")) return "+20.0%";
                break;
            case TRADER:
                if (statKey.equals("goldGainPercent")) return "-20.0% (Штраф) + " + String.format("%.1f", (double)(pp.getMini2() * 4)) + "%";
                if (statKey.equals("rareDropChance")) return "+3.0%";
                break;
        }
        return null;
    }
}
