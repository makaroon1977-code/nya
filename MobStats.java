public class MobStats {

    private int level;
    private double health;
    private double attack;
    private double defense;

    public MobStats(int level) {
        this.level = level;
        this.health = calculateHealth(level);
        this.attack = calculateAttack(level);
        this.defense = calculateDefense(level);
    }

    private double calculateHealth(int level) {
        return 100 + (level * 20); // Base health plus scaling with level
    }

    private double calculateAttack(int level) {
        return 10 + (level * 5); // Base attack plus scaling with level
    }

    private double calculateDefense(int level) {
        return 5 + (level * 3); // Base defense plus scaling with level
    }

    public int getLevel() {
        return level;
    }

    public double getHealth() {
        return health;
    }

    public double getAttack() {
        return attack;
    }

    public double getDefense() {
        return defense;
    }
}