package dev.yournick.mobarena.amulet;

import org.bukkit.ChatColor;

public enum AmuletRarity {
    COMMON("Обычный", ChatColor.WHITE, 0.70, 5, 3, 0.25),
    RARE("Редкий", ChatColor.BLUE, 0.20, 10, 7, 0.50),
    EPIC("Эпический", ChatColor.LIGHT_PURPLE, 0.08, 15, 10, 1.50),
    LEGENDARY("Легендарный", ChatColor.GOLD, 0.02, 25, 15, 2.50);

    private final String displayName;
    private final ChatColor color;
    private final double baseChance;
    private final int maxLevel;
    private final int totalUpgradeCost;
    private final double maxScaling; // Прирост на макс уровне (0.25 = +25%)

    AmuletRarity(String displayName, ChatColor color, double baseChance, int maxLevel, int totalUpgradeCost, double maxScaling) {
        this.displayName = displayName;
        this.color = color;
        this.baseChance = baseChance;
        this.maxLevel = maxLevel;
        this.totalUpgradeCost = totalUpgradeCost;
        this.maxScaling = maxScaling;
    }

    public String getDisplayName() { return displayName; }
    public ChatColor getColor() { return color; }
    public double getBaseChance() { return baseChance; }
    public int getMaxLevel() { return maxLevel; }
    public int getTotalUpgradeCost() { return totalUpgradeCost; }
    public double getMaxScaling() { return maxScaling; }

    public int getUpgradeCostForLevel(int currentLevel) {
        if (currentLevel >= maxLevel) return 0;
        
        // Распределяем totalUpgradeCost на (maxLevel - 1) шагов улучшения.
        // Используем разницу между кумулятивными значениями для получения целых чисел.
        int totalUpgrades = maxLevel - 1;
        int currentTotal = (currentLevel * totalUpgradeCost) / totalUpgrades;
        int nextTotal = ((currentLevel + 1) * totalUpgradeCost) / totalUpgrades;
        
        return nextTotal - currentTotal;
    }
}
