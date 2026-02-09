package dev.yournick.mobarena.player;

public class PlayerStats {

    // Offensive
    private double damagePercent = 1.0;
    private double critChance = 0.0;
    private double critDamage = 1.0;
    private double lifesteal = 0.0;
    private double fireDamagePercent = 1.0;
    private double burnDurationPercent = 1.0;

    // Defensive
    private double maxHealthPercent = 1.0;
    private double armorPercent = 0.0; // Reduction %
    private double dodgeChance = 0.0;
    private double knockbackResistance = 0.0;
    private double deathSaveChance = 0.0;
    private double fireResistance = 0.0;
    private double damageReflection = 0.0;

    // Utility / Health
    private double healOnKill = 0.0;
    private double regenPower = 1.0;
    private double healReceived = 1.0;
    private double moveSpeedPercent = 1.0;

    // Economy / Loot
    private double luck = 1.0;
    private double dropChance = 1.0;
    private double doubleDropChance = 0.0;
    private double rareDropChance = 0.0;
    private double goldGainPercent = 1.0;
    private double sellPricePercent = 1.0;
    private double attackSpeedPercent = 1.0;

    public void reset() {
        damagePercent = 1.0;
        critChance = 0.0;
        critDamage = 1.0;
        lifesteal = 0.0;
        fireDamagePercent = 1.0;
        burnDurationPercent = 1.0;
        maxHealthPercent = 1.0;
        armorPercent = 0.0;
        dodgeChance = 0.0;
        knockbackResistance = 0.0;
        deathSaveChance = 0.0;
        fireResistance = 0.0;
        damageReflection = 0.0;
        healOnKill = 0.0;
        regenPower = 1.0;
        healReceived = 1.0;
        moveSpeedPercent = 1.0;
        luck = 1.0;
        dropChance = 1.0;
        doubleDropChance = 0.0;
        rareDropChance = 0.0;
        goldGainPercent = 1.0;
        sellPricePercent = 1.0;
        attackSpeedPercent = 1.0;
    }

    // Getters and Setters
    public double getDamagePercent() { return damagePercent; }
    public void setDamagePercent(double val) { this.damagePercent = val; }
    public void addDamagePercent(double val) { this.damagePercent += val; }

    public double getCritChance() { return critChance; }
    public void setCritChance(double val) { this.critChance = val; }
    public void addCritChance(double val) { this.critChance += val; }

    public double getCritDamage() { return critDamage; }
    public void setCritDamage(double val) { this.critDamage = val; }
    public void addCritDamage(double val) { this.critDamage += val; }

    public double getLifesteal() { return lifesteal; }
    public void setLifesteal(double val) { this.lifesteal = val; }
    public void addLifesteal(double val) { this.lifesteal += val; }

    public double getFireDamagePercent() { return fireDamagePercent; }
    public void setFireDamagePercent(double val) { this.fireDamagePercent = val; }
    public void addFireDamagePercent(double val) { this.fireDamagePercent += val; }

    public double getBurnDurationPercent() { return burnDurationPercent; }
    public void setBurnDurationPercent(double val) { this.burnDurationPercent = val; }
    public void addBurnDurationPercent(double val) { this.burnDurationPercent += val; }

    public double getMaxHealthPercent() { return maxHealthPercent; }
    public void setMaxHealthPercent(double val) { this.maxHealthPercent = val; }
    public void addMaxHealthPercent(double val) { this.maxHealthPercent += val; }

    public double getArmorPercent() { return armorPercent; }
    public void setArmorPercent(double val) { this.armorPercent = val; }
    public void addArmorPercent(double val) { this.armorPercent += val; }

    public double getDodgeChance() { return dodgeChance; }
    public void setDodgeChance(double val) { this.dodgeChance = val; }
    public void addDodgeChance(double val) { this.dodgeChance += val; }

    public double getKnockbackResistance() { return knockbackResistance; }
    public void setKnockbackResistance(double val) { this.knockbackResistance = val; }
    public void addKnockbackResistance(double val) { this.knockbackResistance += val; }

    public double getDeathSaveChance() { return deathSaveChance; }
    public void setDeathSaveChance(double val) { this.deathSaveChance = val; }
    public void addDeathSaveChance(double val) { this.deathSaveChance += val; }

    public double getFireResistance() { return fireResistance; }
    public void setFireResistance(double val) { this.fireResistance = val; }
    public void addFireResistance(double val) { this.fireResistance += val; }

    public double getDamageReflection() { return damageReflection; }
    public void setDamageReflection(double val) { this.damageReflection = val; }
    public void addDamageReflection(double val) { this.damageReflection += val; }

    public double getHealOnKill() { return healOnKill; }
    public void setHealOnKill(double val) { this.healOnKill = val; }
    public void addHealOnKill(double val) { this.healOnKill += val; }

    public double getRegenPower() { return regenPower; }
    public void setRegenPower(double val) { this.regenPower = val; }
    public void addRegenPower(double val) { this.regenPower += val; }

    public double getHealReceived() { return healReceived; }
    public void setHealReceived(double val) { this.healReceived = val; }
    public void addHealReceived(double val) { this.healReceived += val; }

    public double getMoveSpeedPercent() { return moveSpeedPercent; }
    public void setMoveSpeedPercent(double val) { this.moveSpeedPercent = val; }
    public void addMoveSpeedPercent(double val) { this.moveSpeedPercent += val; }

    public double getLuck() { return luck; }
    public void setLuck(double val) { this.luck = val; }
    public void addLuck(double val) { this.luck += val; }

    public double getDropChance() { return dropChance; }
    public void setDropChance(double val) { this.dropChance = val; }
    public void addDropChance(double val) { this.dropChance += val; }

    public double getDoubleDropChance() { return doubleDropChance; }
    public void setDoubleDropChance(double val) { this.doubleDropChance = val; }
    public void addDoubleDropChance(double val) { this.doubleDropChance += val; }

    public double getRareDropChance() { return rareDropChance; }
    public void setRareDropChance(double val) { this.rareDropChance = val; }
    public void addRareDropChance(double val) { this.rareDropChance += val; }

    public double getGoldGainPercent() { return goldGainPercent; }
    public void setGoldGainPercent(double val) { this.goldGainPercent = val; }
    public void addGoldGainPercent(double val) { this.goldGainPercent += val; }

    public double getSellPricePercent() { return sellPricePercent; }
    public void setSellPricePercent(double val) { this.sellPricePercent = val; }
    public void addSellPricePercent(double val) { this.sellPricePercent += val; }

    public double getAttackSpeedPercent() { return attackSpeedPercent; }
    public void setAttackSpeedPercent(double val) { this.attackSpeedPercent = val; }
    public void addAttackSpeedPercent(double val) { this.attackSpeedPercent += val; }
}
