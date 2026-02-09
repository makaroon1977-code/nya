package dev.yournick.mobarena.player;

import java.util.HashMap;
import java.util.Map;

public class PerkProfile {
    private int xp = 0;
    private int mini1 = 0;
    private int mini2 = 0;
    private int mini3 = 0;
    private boolean large1 = false;
    private boolean large2 = false;
    private boolean large3 = false;
    private boolean unique = false;

    public int getXp() { return xp; }
    public void addXp(int amount) { this.xp += amount; }
    public void setXp(int xp) { this.xp = xp; }

    public int getMini1() { return mini1; }
    public void setMini1(int level) { this.mini1 = level; }
    
    public int getMini2() { return mini2; }
    public void setMini2(int level) { this.mini2 = level; }

    public int getMini3() { return mini3; }
    public void setMini3(int level) { this.mini3 = level; }

    public boolean hasLarge1() { return large1; }
    public void setLarge1(boolean large1) { this.large1 = large1; }

    public boolean hasLarge2() { return large2; }
    public void setLarge2(boolean large2) { this.large2 = large2; }

    public boolean hasLarge3() { return large3; }
    public void setLarge3(boolean large3) { this.large3 = large3; }

    public boolean hasUnique() { return unique; }
    public void setUnique(boolean unique) { this.unique = unique; }

    public int getTotalMini() {
        return mini1 + mini2 + mini3;
    }
}
