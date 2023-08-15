package net.sistr.littlemaidrebirth.config;


import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.sistr.littlemaidrebirth.LMRBMod;

/**
 * LMRBのコンフィグ
 */
@Config(name = LMRBMod.MODID)
public class LMRBConfig implements ConfigData {

    //spawn

    @ConfigEntry.Category("spawn")
    @ConfigEntry.Gui.RequiresRestart
    private boolean canSpawn = true;

    @ConfigEntry.Category("spawn")
    private boolean canDespawn;

    @ConfigEntry.Category("spawn")
    private int spawnWeight = 5;

    @ConfigEntry.Category("spawn")
    private int minSpawnGroupSize = 1;

    @ConfigEntry.Category("spawn")
    private int maxSpawnGroupSize = 3;

    @ConfigEntry.Category("spawn")
    private boolean silentDefaultVoice;

    @ConfigEntry.Category("spawn")
    private String defaultSoundPackName = "";

    //maid

    @ConfigEntry.Category("maid")
    private float generalMaidDamageFactor = 1.0f;

    @ConfigEntry.Category("maid")
    private float battleModeMaidDamageFactor = 1.0f;

    @ConfigEntry.Category("maid")
    private float nonBattleModeMaidDamageFactor = 0.0f;

    @ConfigEntry.Category("maid")
    private boolean disableMaidDeath = true;

    @ConfigEntry.Category("maid")
    private float emergencyMaidHealthThreshold = 0.5f;

    @ConfigEntry.Category("maid")
    private boolean enableWorkInEmergency = false;

    @ConfigEntry.Category("maid")
    private int healInterval = 2;

    @ConfigEntry.Category("maid")
    private int healAmount = 1;

    @ConfigEntry.Category("maid")
    private float freedomRange = 16.0f;

    @ConfigEntry.Category("maid")
    private float followStartRange = 6.0f;

    @ConfigEntry.Category("maid")
    private float followEndRange = 5.0f;

    @ConfigEntry.Category("maid")
    private float sprintStartRange = 8.0f;

    @ConfigEntry.Category("maid")
    private float sprintEndRange = 6.0f;

    @ConfigEntry.Category("maid")
    private boolean allowTeleporting = true;

    @ConfigEntry.Category("maid")
    private float teleportStartRange = 16.0f;

    @ConfigEntry.Category("maid")
    private float emergencyTeleportStartRange = 6.0f;

    @ConfigEntry.Category("maid")
    private float emergencyTeleportHealthThreshold = 0.5f;

    @ConfigEntry.Category("maid")
    private boolean friendlyFire = false;

    @ConfigEntry.Category("maid")
    private boolean canMoveToDanger = false;

    @ConfigEntry.Category("maid")
    private boolean immortal = false;

    @ConfigEntry.Category("maid")
    private boolean fallImmunity = false;

    @ConfigEntry.Category("maid")
    private boolean nonMobDamageImmunity = false;

    @ConfigEntry.Category("maid")
    private boolean canResurrection;

    //mode

    @ConfigEntry.Category("mode")
    private float fencerRangeFactor = 1.0f;

    @ConfigEntry.Category("mode")
    private float fencerAttackRateFactor = 1.0f;

    @ConfigEntry.Category("mode")
    private float archerRangeFactor = 1.0f;

    @ConfigEntry.Category("mode")
    private float archerShootRateFactor = 1.0f;

    @ConfigEntry.Category("mode")
    private float archerShootVelocityFactor = 1.0f;

    @ConfigEntry.Category("mode")
    private int torcherLightLevelThreshold = 4;

    //contract

    @ConfigEntry.Category("contract")
    private int consumeSalaryInterval = 24000;

    @ConfigEntry.Category("contract")
    private int unpaidCountLimit = 7;

    //misc

    //todo デフォtrue
    @ConfigEntry.Category("misc")
    private boolean canPickupItemByNoOwner;

    @ConfigEntry.Category("misc")
    private boolean canMilking;


    public boolean isCanSpawn() {
        return canSpawn;
    }

    public boolean isCanDespawn() {
        return canDespawn;
    }

    public int getSpawnWeight() {
        return spawnWeight;
    }

    public int getMinSpawnGroupSize() {
        return minSpawnGroupSize;
    }

    public int getMaxSpawnGroupSize() {
        return maxSpawnGroupSize;
    }

    public boolean isCanResurrection() {
        return canResurrection;
    }

    public boolean isCanPickupItemByNoOwner() {
        return canPickupItemByNoOwner;
    }

    public boolean isCanMilking() {
        return canMilking;
    }

    public float getFencerRangeFactor() {
        return fencerRangeFactor;
    }

    public float getFencerAttackRateFactor() {
        return fencerAttackRateFactor;
    }

    public float getArcherRangeFactor() {
        return archerRangeFactor;
    }

    public float getArcherShootRateFactor() {
        return archerShootRateFactor;
    }

    public float getArcherShootVelocityFactor() {
        return archerShootVelocityFactor;
    }

    public int getConsumeSalaryInterval() {
        return consumeSalaryInterval;
    }

    public int getUnpaidCountLimit() {
        return unpaidCountLimit;
    }

    public float getFollowStartRange() {
        return followStartRange;
    }

    public float getFollowEndRange() {
        return followEndRange;
    }

    public float getSprintStartRange() {
        return sprintStartRange;
    }

    public float getSprintEndRange() {
        return sprintEndRange;
    }

    public boolean isAllowedToTeleport() {
        return allowTeleporting;
    }

    public float getTeleportStartRange() {
        return teleportStartRange;
    }

    public float getEmergencyTeleportStartRange() {
        return emergencyTeleportStartRange;
    }

    public float getFreedomRange() {
        return freedomRange;
    }

    public float getGeneralMaidDamageFactor() {
        return generalMaidDamageFactor;
    }

    public float getBattleModeMaidDamageFactor() {
        return battleModeMaidDamageFactor;
    }

    public float getNonBattleModeMaidDamageFactor() {
        return nonBattleModeMaidDamageFactor;
    }

    public boolean isDisableMaidDeath() {
        return disableMaidDeath;
    }

    public float getEmergencyMaidHealthThreshold() {
        return emergencyMaidHealthThreshold;
    }

    public boolean isEnableWorkInEmergency() {
        return enableWorkInEmergency;
    }

    public int getHealInterval() {
        return healInterval;
    }

    public int getHealAmount() {
        return healAmount;
    }

    public boolean isFriendlyFire() {
        return friendlyFire;
    }

    public boolean isCanMoveToDanger() {
        return canMoveToDanger;
    }

    public boolean isImmortal() {
        return immortal;
    }

    public boolean isFallImmunity() {
        return fallImmunity;
    }

    public boolean isNonMobDamageImmunity() {
        return nonMobDamageImmunity;
    }

    public boolean isSilentDefaultVoice() {
        return silentDefaultVoice;
    }

    public String getDefaultSoundPackName() {
        return defaultSoundPackName;
    }

    public int getTorcherLightLevelThreshold() {
        return torcherLightLevelThreshold;
    }
}
