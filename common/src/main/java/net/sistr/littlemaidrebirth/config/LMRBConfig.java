package net.sistr.littlemaidrebirth.config;


import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;

@Config(name = LittleMaidReBirthMod.MODID)
public class LMRBConfig implements ConfigData {
    private boolean canSpawnLM = true;
    private boolean canDespawnLM;
    private int spawnWeightLM = 5;
    private int spawnLimitLM = 20;
    private int minSpawnGroupSizeLM = 1;
    private int maxSpawnGroupSizeLM = 3;
    private boolean canResurrectionLM;
    private boolean canPickupItemByNoOwnerLM;

    public boolean isCanSpawnLM() {
        return canSpawnLM;
    }

    public boolean isCanDespawnLM() {
        return canDespawnLM;
    }

    public int getSpawnWeightLM() {
        return spawnWeightLM;
    }

    public int getMinSpawnGroupSizeLM() {
        return minSpawnGroupSizeLM;
    }

    public int getMaxSpawnGroupSizeLM() {
        return maxSpawnGroupSizeLM;
    }

    public boolean isCanResurrectionLM() {
        return canResurrectionLM;
    }

    public boolean isCanPickupItemByNoOwnerLM() {
        return canPickupItemByNoOwnerLM;
    }

    public int getSpawnLimitLM() {
        return spawnLimitLM;
    }
}
