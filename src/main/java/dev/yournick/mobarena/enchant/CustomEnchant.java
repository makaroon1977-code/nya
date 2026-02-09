package dev.yournick.mobarena.enchant;

public enum CustomEnchant {
    // --- SWORD ---
    EXECUTIONER("Палач", 500, "Common", "+20% урон по мобам", true),
    SLASH("Разрез", 500, "Common", "10% урон по области", true),
    SHARPNESS("Острота", 500, "Common", "+15% урон", true),
    
    CRITICAL_STRIKE("Критический удар", 1000, "Rare", "+75% к крит. урону", true),
    ARMOR_BREAK("Слом брони", 1000, "Rare", "-15% брони цели", true),
    SIPHON("Высасывание", 1000, "Rare", "Хил 1 HP при ударе", true),
    
    RAGE("Ярость", 2000, "Epic", "+50% урона при HP < 50%", true),
    RUTHLESSNESS("Безжалостность", 2000, "Epic", "Убийство усилит след. атаку на 200%", true),
    SMASH("Разгром", 2000, "Epic", "Каждая 4-я атака 200% AoE урона", true),
    
    DEVOURER("Поглотитель", 5000, "Legendary", "Килл хилит фулл и +50% урона на 2 удара", true),
    FATAL_STRIKE("Роковой удар", 5000, "Legendary", "7% шанс на x3 урон", true),
    BERSERKER_CURSE("Проклятие берсерка", 5000, "Legendary", "+100% урона, -25% брони (навсегда)", true),

    // --- ARMOR ---
    VITALITY("Живучесть", 500, "Common", "+20% Max HP", false),
    LIGHTNESS("Лёгкость", 500, "Common", "+20% Скорость", false),
    FIRE_RESISTANCE("Огнестойкость", 500, "Common", "-40% урон от огня", false),
    
    EVASION("Уклонение", 1000, "Rare", "+20% уклонения", false),
    SHIELD_BEARER("Щитоносец", 1000, "Rare", "Шанс 10% на щит (Armor +40% на 5с)", false),
    FORTITUDE("Стойкость", 1000, "Rare", "-30% урон от элитных мобов", false),
    
    REFLECTION("Отражение", 2000, "Epic", "15% урона возвращается", false),
    LAST_STAND("Последний рубеж", 2000, "Epic", "+40% Armor при HP < 50%", false),
    
    ABSOLUTE_BARRIER("Абсолютный барьер", 5000, "Legendary", "При HP < 25% блок 99% урона на 10с", false),
    
    SECOND_LIFE("Вторая жизнь", 15000, "Mythic", "1 раз за волну: спасение и фулл реген", false);

    private final String displayName;
    private final int cost;
    private final String rarity;
    private final String description;
    private final boolean forWeapon;

    CustomEnchant(String displayName, int cost, String rarity, String description, boolean forWeapon) {
        this.displayName = displayName;
        this.cost = cost;
        this.rarity = rarity;
        this.description = description;
        this.forWeapon = forWeapon;
    }

    public String getDisplayName() { return displayName; }
    public int getCost() { return cost; }
    public String getRarity() { return rarity; }
    public String getDescription() { return description; }
    public boolean isForWeapon() { return forWeapon; }

    public static CustomEnchant fromName(String name) {
        for (CustomEnchant e : values()) {
            if (e.name().equalsIgnoreCase(name) || e.displayName.equalsIgnoreCase(name)) return e;
        }
        return null;
    }
}
