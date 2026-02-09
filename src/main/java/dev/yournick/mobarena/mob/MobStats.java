package dev.yournick.mobarena.mob;

public class MobStats {
    
    private final double health;
    private final double damage;
    private final double speed;
    
    // Новые статы брони
    private final double armor;
    private final double fireArmor;
    private final double windArmor;
    private final double waterArmor;
    private final double mageArmor;

    public static MobStats forWave(int wave) {
        // Начальный бонус 0. С каждой волной +7%
        // Каждая 10-я волна +20%
        double hpMultiplier = 1.0;
        double dmgMultiplier = 1.0;
        
        for (int i = 1; i < wave; i++) {
            if (i % 10 == 0) {
                hpMultiplier *= 1.20;
                dmgMultiplier *= 1.20;
            } else {
                hpMultiplier *= 1.07;
                dmgMultiplier *= 1.07;
            }
        }
        
        return new MobStats(
                20.0 * hpMultiplier,
                5.0 * dmgMultiplier,
                Math.min(0.2 + (wave - 1) * 0.005, 0.35),
                0.0, // Armor %
                0.0, // Fire Armor %
                0.0, // Wind Armor %
                0.0, // Water Armor %
                0.0  // Mage Armor %
        );
    }

    public static MobStats forElite(EliteType type, int wave) {
        // В 3 раза меньше хп и в 2.5 раза меньше урона от старых значений
        // Старые значения: hp = 500 + wave*50, damage = 10 + wave*2
        double baseHp = (500.0 + (wave * 50.0)) / 3.0;
        double baseDamage = (10.0 + (wave * 2.0)) / 2.5;
        double speed = 0.25;
        double armor = 0.0;

        switch (type) {
            case BLOOD_BULLY:
                baseHp *= 1.5;
                baseDamage *= 1.2;
                break;
            case BONE_SNIPER:
                baseHp *= 0.8;
                baseDamage *= 2.0;
                break;
            case BROODMOTHER:
                armor = 0.80;
                break;
            case PUPPETEER:
                baseHp *= 1.2;
                break;
            case WITHER_PHANTOM:
                speed = 0.30;
                break;
        }

        return new MobStats(
                baseHp, baseDamage, speed,
                armor, 0.0, 0.0, 0.0, 0.0
        );
    }

    private MobStats(double health, double damage, double speed, 
                     double armor, double fireArmor, double windArmor, 
                     double waterArmor, double mageArmor) {
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.armor = armor;
        this.fireArmor = fireArmor;
        this.windArmor = windArmor;
        this.waterArmor = waterArmor;
        this.mageArmor = mageArmor;
    }

    public double getHealth() { return health; }
    public double getDamage() { return damage; }
    public double getSpeed() { return speed; }
    
    public double getArmor() { return armor; }
    public double getFireArmor() { return fireArmor; }
    public double getWindArmor() { return windArmor; }
    public double getWaterArmor() { return waterArmor; }
    public double getMageArmor() { return mageArmor; }
}
