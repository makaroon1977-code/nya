package dev.yournick.mobarena.perk;

import org.bukkit.Material;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public enum Perk {

    LUCKY("Удачливый", Material.RABBIT_FOOT),
    TOUGH("Стойкий", Material.SHIELD),
    AGILE("Ловкий", Material.FEATHER),
    FIREBORN("Огнерождённый", Material.BLAZE_POWDER),
    PIRATE("Пират", Material.POTION), // Will handle as Fire Resistance in menu
    BLACKSMITH("Кузнец", Material.IRON_INGOT),
    TRADER("Торговец", Material.GOLD_NUGGET);

    private final String displayName;
    private final Material icon;

    Perk(String displayName, Material icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() { return displayName; }
    public Material getIcon() { return icon; }

    public boolean isFireEnchant() {
        return this == FIREBORN;
    }

    public List<PotionEffect> getPermanentEffects() {
        return new ArrayList<>();
    }
}
