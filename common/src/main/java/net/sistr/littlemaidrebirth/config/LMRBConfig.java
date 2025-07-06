package net.sistr.littlemaidrebirth.config;


import com.google.common.collect.Lists;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.tags.LMTags;

import java.util.List;

/**
 * LMRBのコンフィグ
 */
@Config(name = LMRBMod.MODID)
public class LMRBConfig implements ConfigData {

    @ConfigEntry.Category("spawn")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Spawn spawn = new Spawn();

    public static class Spawn {
        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.Tooltip
        public boolean canNaturalSpawn = true;
        @ConfigEntry.Gui.Tooltip
        public boolean canDespawn = false;
        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.Tooltip
        public List<String> maidSpawnBiomeTags = Lists.newArrayList(
                LMTags.Biomes.MAID_SPAWN_BIOME.id().toString(),
                BiomeTags.VILLAGE_DESERT_HAS_STRUCTURE.id().toString(),
                BiomeTags.VILLAGE_PLAINS_HAS_STRUCTURE.id().toString(),
                BiomeTags.VILLAGE_SAVANNA_HAS_STRUCTURE.id().toString(),
                BiomeTags.VILLAGE_SNOWY_HAS_STRUCTURE.id().toString(),
                BiomeTags.VILLAGE_TAIGA_HAS_STRUCTURE.id().toString()
        );
        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.Tooltip
        public List<String> maidSpawnExcludeBiomeTags = Lists.newArrayList(
                LMTags.Biomes.MAID_SPAWN_EXCLUDE_BIOME.id().toString()
        );
        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.Tooltip
        public int spawnWeight = 5;
        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.Tooltip
        public int minSpawnGroupSize = 1;
        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.Tooltip
        public int maxSpawnGroupSize = 3;
        @ConfigEntry.Gui.Tooltip
        public boolean silentDefaultVoice = false;
        @ConfigEntry.Gui.Tooltip
        public String defaultSoundPackName = "";
    }

    @ConfigEntry.Category("health")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Health health = new Health();

    public static class Health {
        @ConfigEntry.Gui.Tooltip
        public int healInterval = 2;
        @ConfigEntry.Gui.Tooltip
        public int healAmount = 1;
        @ConfigEntry.Gui.Tooltip
        public float healDelayThreshold = 0.75f;
        @ConfigEntry.Gui.Tooltip
        public boolean disableMaidDeath = false;
        @ConfigEntry.Gui.Tooltip
        public float generalMaidDamageFactor = 1.0f;
        @ConfigEntry.Gui.Tooltip
        public float battleModeMaidDamageFactor = 1.0f;
        @ConfigEntry.Gui.Tooltip
        public float nonBattleModeMaidDamageFactor = 1.0f;
        @ConfigEntry.Gui.Tooltip
        public float emergencyMaidHealthThreshold = 0.5f;
        @ConfigEntry.Gui.Tooltip
        public boolean enableWorkInEmergency = false;
        @ConfigEntry.Gui.Tooltip
        public boolean enableFriendlyFire = false;
        @ConfigEntry.Gui.Tooltip
        public boolean enableSafeMove = true;
        @ConfigEntry.Gui.Tooltip
        public boolean immortal = false;
        @ConfigEntry.Gui.Tooltip
        public boolean fallImmunity = false;
        @ConfigEntry.Gui.Tooltip
        public boolean nonMobDamageImmunity = false;
    }

    @ConfigEntry.Category("movement")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Movement movement = new Movement();

    public static class Movement {
        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.Tooltip
        public float freedomSpeed = 0.65f;
        @ConfigEntry.Gui.Tooltip
        public float freedomRange = 16.0f;
        @ConfigEntry.Gui.Tooltip
        public float tracerSpeed = 0.65f;
        @ConfigEntry.Gui.Tooltip
        public int tracerHorizonRange = 4;
        @ConfigEntry.Gui.Tooltip
        public int tracerVerticalRange = 2;
        @ConfigEntry.Gui.Tooltip
        public float followSpeed = 1.0f;
        @ConfigEntry.Gui.Tooltip
        public float followStartDistance = 6.0f;
        @ConfigEntry.Gui.Tooltip
        public float followEndDistance = 5.0f;
        @ConfigEntry.Gui.Tooltip
        public float sprintSpeed = 1.2f;
        @ConfigEntry.Gui.Tooltip
        public float sprintStartDistance = 8.0f;
        @ConfigEntry.Gui.Tooltip
        public float sprintEndDistance = 6.0f;
        @ConfigEntry.Gui.Tooltip
        public float teleportStartDistance = 16.0f;
        @ConfigEntry.Gui.Tooltip
        public float emergencyTeleportStartDistance = 6.0f;
        @ConfigEntry.Gui.Tooltip
        public int teleportWidth = 3;
        @ConfigEntry.Gui.Tooltip
        public int teleportHeight = 1;
        @ConfigEntry.Gui.Tooltip
        public boolean canTeleportOwnerForwards = false;
        @ConfigEntry.Gui.Tooltip
        public float ownerForwardRange = 4.0f;
        @ConfigEntry.Gui.Tooltip
        public int maxTryTeleportCount = 10;
        @ConfigEntry.Gui.Tooltip
        public float pickupItemSpeed = 1.0f;
        @ConfigEntry.Gui.Tooltip
        public float pickupItemRange = 8.0f;
        @ConfigEntry.Gui.Tooltip
        public int pickupItemFrequency = 40;
        @ConfigEntry.Gui.RequiresRestart
        @ConfigEntry.Gui.Tooltip
        public float escapeSpeed = 1.2f;
    }

    @ConfigEntry.Category("work")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Work work = new Work();

    public static class Work {
        @ConfigEntry.Gui.Tooltip
        public int defaultWorkItemSlotSize = 9;
        @ConfigEntry.Gui.Tooltip
        public float maxTargetRange = 16f;
        @ConfigEntry.Gui.Tooltip
        public float fencerAttackDistanceFactor = 1.0f;
        @ConfigEntry.Gui.Tooltip
        public float fencerAttackRateFactor = 0.75f;
        @ConfigEntry.Gui.Tooltip
        public float archerShootDistanceFactor = 1.0f;
        @ConfigEntry.Gui.Tooltip
        public float archerShootRateFactor = 1.0f;
        @ConfigEntry.Gui.Tooltip
        public float archerShootVelocityFactor = 1.0f;
        @ConfigEntry.Gui.Tooltip
        public int torcherLightLevelThreshold = 7;
        @ConfigEntry.Gui.Tooltip
        public float searchContainerRange = 8.0f;
    }

    @ConfigEntry.Category("contract")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Contract contract = new Contract();

    public static class Contract {
        @ConfigEntry.Gui.Tooltip
        public int consumeSalaryInterval = 24000;
        @ConfigEntry.Gui.Tooltip
        public int unpaidDaysLimit = 7;
        @ConfigEntry.Gui.Tooltip
        public int maxAutoSalaryReceiptSlotSize = 3;
        @ConfigEntry.Gui.Tooltip
        public int startAutoSalaryReceiptSlotThreshold = 1;
        @ConfigEntry.Gui.Tooltip
        public int maxMemorySalaryBoxPos = 4;
        @ConfigEntry.Gui.Tooltip
        public float memorySalaryBoxDistance = 8.0f;
        @ConfigEntry.Gui.Tooltip
        public int memorySalaryBoxInterval = 20;
        @ConfigEntry.Gui.Tooltip
        public float searchSalaryBoxDistance = 16.0f;
        @ConfigEntry.Gui.Tooltip
        public int startIntervalOfAutoSalaryReceipt = 60;
        @ConfigEntry.Gui.Tooltip
        public int findPathIntervalOfAutoSalaryReceipt = 10;
        @ConfigEntry.Gui.Tooltip
        public int maxMoveTimeOnAutoSalaryReceipt = 200;
        @ConfigEntry.Gui.Tooltip
        public int maxMoveTimeAfterAutoSalaryReceipt = 400;
    }

    @ConfigEntry.Category("misc")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Misc misc = new Misc();

    public static class Misc {
        @ConfigEntry.Gui.Tooltip
        public boolean canPickupItem = true;
        @ConfigEntry.Gui.Tooltip
        public boolean canPickupExperienceOrb = true;
        @ConfigEntry.Gui.Tooltip
        public boolean canPickupItemByNoOwner = false;
        @ConfigEntry.Gui.Tooltip
        public boolean canMilking = false;
        @ConfigEntry.Gui.Tooltip
        public int playSoundInterval = 5;
        @ConfigEntry.Gui.Tooltip
        public float followAtHeldSalaryRange = 1.5f;
        @ConfigEntry.Gui.Tooltip
        public float followAtHeldEmployItemRange = 1.5f;
        @ConfigEntry.Gui.Tooltip
        public float stareAtSalaryRange = 4.0f;
        @ConfigEntry.Gui.Tooltip
        public float stareAtEmployItemRange = 4.0f;
        @ConfigEntry.Gui.Tooltip
        public int maxAccelerationStack = 8;
        @ConfigEntry.Gui.Tooltip
        public int accelerationTicksPerStack = 80;
        @ConfigEntry.Gui.Tooltip
        public int accelerationMultiple = 2;
    }

    @ConfigEntry.Category("target")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Target target = new Target();

    public static class Target {
        @ConfigEntry.Gui.Tooltip
        public int maxPreemptiveDistance = 16;
        @ConfigEntry.Gui.Tooltip
        public int maxTargetDistance = 24;
        @ConfigEntry.Gui.Tooltip
        public int bowLongRangeThreshold = 8;
        @ConfigEntry.Gui.Tooltip
        public int swordCloseRangeThreshold = 8;
        @ConfigEntry.Gui.Tooltip
        public float dangerCloseRangeThreshold = 8.0f;
        @ConfigEntry.Gui.Tooltip
        public float injuredHealthThreshold = 0.5f;
        @ConfigEntry.Gui.Tooltip
        public float distributionPenaltyMultiplier = 900.0f;
        @ConfigEntry.Gui.Tooltip
        public float distributionPenaltyMultiplierDifferentWeapon = 400.0f;
        @ConfigEntry.Gui.Tooltip
        public float masterDistanceBonusBaseDistance = 8.0f;
        @ConfigEntry.Gui.Tooltip
        public float masterDistanceBonusMultiplier = 10.0f;
        @ConfigEntry.Gui.Tooltip
        public float masterDistancePenaltyBaseDistance = 8.0f;
        @ConfigEntry.Gui.Tooltip
        public float masterDistancePenaltyMultiplier = 5.0f;
        @ConfigEntry.Gui.Tooltip
        public float maidDistancePenaltyBaseDistance = 8.0f;
        @ConfigEntry.Gui.Tooltip
        public float maidDistancePenaltyMultiplier = 1.0f;
        @ConfigEntry.Gui.Tooltip
        public float maidDistanceBonusBaseDistance = 8.0f;
        @ConfigEntry.Gui.Tooltip
        public float maidDistanceBonusMultiplier = 1.0f;
        @ConfigEntry.Gui.Tooltip
        public int attackedByValidTicks = 100;
    }

    @ConfigEntry.Category("advanced_target")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public AdvancedTarget advancedTarget = new AdvancedTarget();

    public static class AdvancedTarget {
        @ConfigEntry.Gui.Tooltip
        public int priorityEvacuation = -1000;
        @ConfigEntry.Gui.Tooltip
        public int prioritySelfAttacker = 1500;
        @ConfigEntry.Gui.Tooltip
        public int priorityMasterAttacker = 1200;
        @ConfigEntry.Gui.Tooltip
        public int priorityMaidAttacker = 1000;
        @ConfigEntry.Gui.Tooltip
        public int priorityMasterTarget = 1000;
        @ConfigEntry.Gui.Tooltip
        public int priorityMaidTarget = 1000;
        @ConfigEntry.Gui.Tooltip
        public int priorityNormalEnemy = 800;
        @ConfigEntry.Gui.Tooltip
        public int bowLongRangeBonus = 100;
        @ConfigEntry.Gui.Tooltip
        public int bowCloseRangePenalty = 0;
        @ConfigEntry.Gui.Tooltip
        public int swordCloseRangeBonus = 0;
        @ConfigEntry.Gui.Tooltip
        public int swordLongRangePenalty = 0;
        @ConfigEntry.Gui.Tooltip
        public int swordDangerPenalty = -1000;
        @ConfigEntry.Gui.Tooltip
        public int dangerBasePenalty = -500;
        @ConfigEntry.Gui.Tooltip
        public int dangerClosePenalty = -500;
        @ConfigEntry.Gui.Tooltip
        public int injuredMasterAttackerBonus = 500;
        @ConfigEntry.Gui.Tooltip
        public int injuredMaidAttackerBonus = 300;
        @ConfigEntry.Gui.Tooltip
        public int supportModeMasterTargetPenalty = -1300;
        @ConfigEntry.Gui.Tooltip
        public int supportModeOtherEnemyBonus = 0;
        @ConfigEntry.Gui.Tooltip
        public int distanceLimitPenalty = -2000;
        @ConfigEntry.Gui.Tooltip
        public int nonProjectileHitPenalty = -2000;
    }

    @ConfigEntry.Category("client")
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.TransitiveObject
    public Client client = new Client();

    public static class Client {
        @ConfigEntry.Gui.Tooltip
        public boolean enableWaitPoseOnMoving = false;
    }
}
