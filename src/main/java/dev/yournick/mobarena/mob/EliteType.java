package dev.yournick.mobarena.mob;

import org.bukkit.entity.EntityType;

public enum EliteType {
    BLOOD_BULLY("Кровавый громила", EntityType.ZOMBIE),
    BONE_SNIPER("Костяной снайпер", EntityType.SKELETON),
    PUPPETEER("Кукловод", EntityType.SPIDER),
    BROODMOTHER("Матка роя", EntityType.SPIDER),
    WITHER_PHANTOM("Искажённый скелет", EntityType.WITHER_SKELETON);

    private final String displayName;
    private final EntityType entityType;

    EliteType(String displayName, EntityType entityType) {
        this.displayName = displayName;
        this.entityType = entityType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public EntityType getEntityType() {
        return entityType;
    }
}
