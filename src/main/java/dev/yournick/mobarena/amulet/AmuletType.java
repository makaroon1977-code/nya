package dev.yournick.mobarena.amulet;

import org.bukkit.Material;
import java.util.HashMap;
import java.util.Map;

public enum AmuletType {
    // --- COMMON ---
    STRENGTH("Амулет силы", AmuletRarity.COMMON, Material.IRON_SWORD),
    HEALTH("Амулет здоровья", AmuletRarity.COMMON, Material.APPLE),
    SPEED("Амулет скорости", AmuletRarity.COMMON, Material.FEATHER),
    LUCK("Амулет удачи", AmuletRarity.COMMON, Material.RABBIT_FOOT),
    TOUGHNESS("Амулет стойкости", AmuletRarity.COMMON, Material.SHIELD),

    // --- RARE ---
    CRIT("Амулет критов", AmuletRarity.RARE, Material.IRON_SWORD),
    HUNTER("Амулет охотника", AmuletRarity.RARE, Material.BOW),
    AGILE("Амулет ловкости", AmuletRarity.RARE, Material.FEATHER),
    GOLD("Амулет золота", AmuletRarity.RARE, Material.GOLD_NUGGET),
    REGEN("Амулет регенерации", AmuletRarity.RARE, Material.POTION),

    // --- EPIC ---
    BERSERKER("Амулет берсерка", AmuletRarity.EPIC, Material.IRON_AXE),
    VAMPIRE("Амулет вампира", AmuletRarity.EPIC, Material.LEATHER), // Bat wing imitation
    DODGE("Амулет уклона", AmuletRarity.EPIC, Material.ELYTRA),
    ASSASSIN("Амулет убийцы", AmuletRarity.EPIC, Material.DIAMOND_SWORD),
    FORTUNE("Амулет фортуны", AmuletRarity.EPIC, Material.POTION),

    // --- LEGENDARY ---
    TITAN("Амулет титана", AmuletRarity.LEGENDARY, Material.SHIELD),
    DESTRUCTION("Амулет разрушения", AmuletRarity.LEGENDARY, Material.TNT),
    IMMORTALITY("Амулет бессмертия", AmuletRarity.LEGENDARY, Material.TOTEM),
    STORM("Амулет Бури", AmuletRarity.LEGENDARY, Material.NETHER_STAR),
    ETERNAL_FIRE("Амулет Вечного Огня", AmuletRarity.LEGENDARY, Material.BLAZE_POWDER);

    private final String displayName;
    private final AmuletRarity rarity;
    private final Material material;

    AmuletType(String displayName, AmuletRarity rarity, Material material) {
        this.displayName = displayName;
        this.rarity = rarity;
        this.material = material;
    }

    public String getDisplayName() { return displayName; }
    public AmuletRarity getRarity() { return rarity; }
    public Material getMaterial() { return material; }

    public static String formatStatName(String key) {
        switch (key) {
            case "damagePercent": return "Урон";
            case "maxHealthPercent": return "Макс. HP";
            case "moveSpeedPercent": return "Скорость";
            case "goldGainPercent": return "Золото";
            case "armorPercent": return "Броня";
            case "critChance": return "Шанс крита";
            case "critDamage": return "Крит. урон";
            case "dodgeChance": return "Шанс уклонения";
            case "lifesteal": return "Вампиризм";
            case "healOnKill": return "Лечение при убийстве";
            case "regenPower": return "Сила регена";
            case "healReceived": return "Получаемое лечение";
            case "doubleDropChance": return "Шанс двойного дропа";
            case "rareDropChance": return "Шанс редкого дропа";
            case "deathSaveChance": return "Шанс спасения";
            case "fireDamagePercent": return "Огненный урон";
            case "fireResistance": return "Сопр. огню";
            case "attackSpeed": return "Скор. атаки";
            default: return key;
        }
    }

    public static boolean isPercent(String key) {
        return !key.equals("healOnKill");
    }

    public Map<String, Double> getBaseStats() {
        Map<String, Double> stats = new HashMap<>();
        switch (this) {
            case STRENGTH: stats.put("damagePercent", 0.10); break;
            case HEALTH: stats.put("maxHealthPercent", 0.15); break;
            case SPEED: stats.put("moveSpeedPercent", 0.10); break;
            case LUCK: stats.put("goldGainPercent", 0.15); break;
            case TOUGHNESS: stats.put("armorPercent", 0.10); break;

            case CRIT: 
                stats.put("critChance", 0.10); 
                stats.put("critDamage", 0.20); 
                break;
            case HUNTER: 
                stats.put("damagePercent", 0.15); 
                stats.put("healOnKill", 3.0); 
                break;
            case AGILE: 
                stats.put("dodgeChance", 0.12); 
                stats.put("moveSpeedPercent", 0.15); 
                break;
            case GOLD: 
                stats.put("goldGainPercent", 0.25); 
                stats.put("rareDropChance", 0.10); // Bonus Gold Chance
                break;
            case REGEN: 
                stats.put("regenPower", 0.25); 
                stats.put("healReceived", 0.10); 
                break;

            case BERSERKER: 
                stats.put("damagePercent", 0.30); 
                stats.put("attackSpeed", 0.20); 
                stats.put("maxHealthPercent", -0.15); 
                break;
            case VAMPIRE: 
                stats.put("lifesteal", 0.12); 
                stats.put("damagePercent", 0.10); 
                break;
            case DODGE: 
                stats.put("dodgeChance", 0.25); 
                stats.put("moveSpeedPercent", 0.20); 
                break;
            case ASSASSIN: 
                stats.put("damagePercent", 0.40); 
                stats.put("critChance", 0.15); 
                break;
            case FORTUNE: 
                stats.put("goldGainPercent", 0.40); 
                stats.put("doubleDropChance", 0.15); 
                break;

            case TITAN: 
                stats.put("maxHealthPercent", 0.50); 
                stats.put("armorPercent", 0.25); 
                stats.put("moveSpeedPercent", -0.20); 
                break;
            case DESTRUCTION: 
                stats.put("damagePercent", 0.45); 
                stats.put("critDamage", 0.60); 
                stats.put("armorPercent", -0.15); 
                break;
            case IMMORTALITY: 
                stats.put("deathSaveChance", 0.20); 
                stats.put("healOnKill", 10.0); 
                break;
            case STORM: 
                stats.put("damagePercent", 0.40); 
                stats.put("moveSpeedPercent", -0.10); 
                break;
            case ETERNAL_FIRE: 
                stats.put("damagePercent", 0.35); 
                stats.put("fireDamagePercent", 0.40); 
                stats.put("fireResistance", 0.25); 
                break;
        }
        return stats;
    }
}
