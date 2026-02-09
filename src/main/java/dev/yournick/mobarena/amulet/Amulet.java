package dev.yournick.mobarena.amulet;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Amulet {
    private final AmuletType type;
    private int level;

    public Amulet(AmuletType type, int level) {
        this.type = type;
        this.level = level;
    }

    public AmuletType getType() { return type; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public double getStatMultiplier() {
        // Прирост на макс уровне: 1.0 (база) + maxScaling * (level / maxLevel)
        // Но user сказал: "у обычных общий прирост на максимальном уровне должен быть 25 процентов"
        // Это значит на макс уровне стат становится 125% от базы.
        double progress = (double) level / type.getRarity().getMaxLevel();
        return 1.0 + (type.getRarity().getMaxScaling() * progress);
    }

    public ItemStack createItem() {
        AmuletRarity rarity = type.getRarity();
        ItemStack item = new ItemStack(type.getMaterial());
        
        // Специальная обработка для зелий
        if (type == AmuletType.REGEN) {
            org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
            pm.setMainEffect(org.bukkit.potion.PotionEffectType.REGENERATION);
            item.setItemMeta(pm);
        } else if (type == AmuletType.FORTUNE) {
            org.bukkit.inventory.meta.PotionMeta pm = (org.bukkit.inventory.meta.PotionMeta) item.getItemMeta();
            pm.setMainEffect(org.bukkit.potion.PotionEffectType.LUCK);
            item.setItemMeta(pm);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Эффект свечения для Амулета Критов и Титана
            if (type == AmuletType.CRIT || type == AmuletType.TITAN) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            meta.setDisplayName(rarity.getColor() + type.getDisplayName() + " [" + level + " ур.]");
            List<String> lore = new ArrayList<>();
            lore.add(rarity.getColor() + rarity.getDisplayName());
            lore.add("");
            lore.add(ChatColor.GRAY + "Характеристики:");
            
            double multiplier = getStatMultiplier();
            for (Map.Entry<String, Double> entry : type.getBaseStats().entrySet()) {
                String key = entry.getKey();
                String statName = AmuletType.formatStatName(key);
                double val = entry.getValue() * multiplier;
                String sign = val >= 0 ? "+" : "";
                
                String formatted;
                String unit;
                if (AmuletType.isPercent(key)) {
                    formatted = String.format("%.1f", val * 100);
                    unit = "%";
                } else {
                    formatted = String.format("%.1f", val);
                    unit = " HP";
                }
                
                lore.add(ChatColor.YELLOW + "• " + statName + ": " + ChatColor.GREEN + sign + formatted + unit);
            }
            
            // Unique buffs for Legendaries
            if (rarity == AmuletRarity.LEGENDARY) {
                lore.add("");
                lore.add(ChatColor.GOLD + "Уникальный бафф:");
                switch (type) {
                    case TITAN:
                        lore.add(ChatColor.YELLOW + "Непробиваемый щит: при <30% HP");
                        lore.add(ChatColor.YELLOW + "дает -80% урона на 5 сек. (КД 90с)");
                        break;
                    case DESTRUCTION:
                        lore.add(ChatColor.YELLOW + "Взрывной удар: каждые 10с атака");
                        lore.add(ChatColor.YELLOW + "наносит 50% AoE урона (3 блока).");
                        break;
                    case IMMORTALITY:
                        lore.add(ChatColor.YELLOW + "Призрачная форма: при <10% HP");
                        lore.add(ChatColor.YELLOW + "инвиз и неуяз на 3 сек. (КД 120с)");
                        break;
                    case STORM:
                        lore.add(ChatColor.YELLOW + "Цепная молния: каждая 6-я атака");
                        lore.add(ChatColor.YELLOW + "бьет молнией (50% урона + Стан 1.5с).");
                        break;
                    case ETERNAL_FIRE:
                        lore.add(ChatColor.YELLOW + "Пламенная аура: поджигает врагов вокруг.");
                        lore.add(ChatColor.YELLOW + "Взрыв пламени при <20% HP (КД 2м).");
                        break;
                }
            }
            
            lore.add("");
            if (level < rarity.getMaxLevel()) {
                lore.add(ChatColor.GRAY + "Прокачка: " + ChatColor.WHITE + level + "/" + rarity.getMaxLevel());
            } else {
                lore.add(ChatColor.AQUA + "МАКСИМАЛЬНЫЙ УРОВЕНЬ");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
