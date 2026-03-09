package net.sistr.littlemaidrebirth.forge.gametest;

import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.gametest.LMRBCommonTests;

@GameTestHolder(LMRBMod.MODID)
@PrefixGameTestTemplate(false)
public class LMRBForgeGameTests {

    private static final String SMALL_FLOOR = "small_floor";
    private static final String FLOOR = "floor";

    // ===== 基本 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void maidSpawn(TestContext context) {
        LMRBCommonTests.maidSpawn(context);
    }

    // ===== E: 緊急状態判定 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void emergencyAtThreshold(TestContext context) {
        LMRBCommonTests.emergencyAtThreshold(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void emergencyBelowThreshold(TestContext context) {
        LMRBCommonTests.emergencyBelowThreshold(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void notEmergencyAboveThreshold(TestContext context) {
        LMRBCommonTests.notEmergencyAboveThreshold(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void notEmergencyFullHealth(TestContext context) {
        LMRBCommonTests.notEmergencyFullHealth(context);
    }

    // ===== C: 雇用・再雇用 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void contractWithCake(TestContext context) {
        LMRBCommonTests.contractWithCake(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void recontractFromStrike(TestContext context) {
        LMRBCommonTests.recontractFromStrike(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void cannotContractWithNonEmployItem(TestContext context) {
        LMRBCommonTests.cannotContractWithNonEmployItem(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nonOwnerCannotInteract(TestContext context) {
        LMRBCommonTests.nonOwnerCannotInteract(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void sneakingSkipsInteraction(TestContext context) {
        LMRBCommonTests.sneakingSkipsInteraction(context);
    }

    // ===== W: 待機切替 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void sugarTogglesWaitOn(TestContext context) {
        LMRBCommonTests.sugarTogglesWaitOn(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void sugarTogglesWaitOff(TestContext context) {
        LMRBCommonTests.sugarTogglesWaitOff(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void sugarHeals(TestContext context) {
        LMRBCommonTests.sugarHeals(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void strikeBlocksSugar(TestContext context) {
        LMRBCommonTests.strikeBlocksSugar(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nonOwnerCannotUseSugar(TestContext context) {
        LMRBCommonTests.nonOwnerCannotUseSugar(context);
    }

    // ===== F: isFriend =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void ownerIsFriend(TestContext context) {
        LMRBCommonTests.ownerIsFriend(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void sameOwnerTamedMobIsFriend(TestContext context) {
        LMRBCommonTests.sameOwnerTamedMobIsFriend(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void anyTamedMobIsFriend(TestContext context) {
        LMRBCommonTests.anyTamedMobIsFriend(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void anyPlayerIsFriendWhenTamed(TestContext context) {
        LMRBCommonTests.anyPlayerIsFriendWhenTamed(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void wildMobIsNotFriend(TestContext context) {
        LMRBCommonTests.wildMobIsNotFriend(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void playerNotFriendWhenWild(TestContext context) {
        LMRBCommonTests.playerNotFriendWhenWild(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void wildMobNotFriendWhenWild(TestContext context) {
        LMRBCommonTests.wildMobNotFriendWhenWild(context);
    }

    // ===== T: canTarget =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void cannotTargetOwner(TestContext context) {
        LMRBCommonTests.cannotTargetOwner(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void cannotTargetSameOwnerTamedMob(TestContext context) {
        LMRBCommonTests.cannotTargetSameOwnerTamedMob(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void cannotTargetAnyTamedMob(TestContext context) {
        LMRBCommonTests.cannotTargetAnyTamedMob(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void canTargetWildHostileMob(TestContext context) {
        LMRBCommonTests.canTargetWildHostileMob(context);
    }

    // ===== P2: FakePlayer ワールド登録検証 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void fakePlayerInWorldPlayers(TestContext context) {
        LMRBCommonTests.fakePlayerInWorldPlayers(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void getTameOwnerReturnsPlayer(TestContext context) {
        LMRBCommonTests.getTameOwnerReturnsPlayer(context);
    }

    // ===== D: damage チェック =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void friendDamageBlockedByDefault(TestContext context) {
        LMRBCommonTests.friendDamageBlockedByDefault(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void friendDamageAllowedWithConfig(TestContext context) {
        LMRBCommonTests.friendDamageAllowedWithConfig(context);
    }

    // ===== FEN: Fencer モード =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void swordActivatesFencerMode(TestContext context) {
        LMRBCommonTests.swordActivatesFencerMode(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void axeActivatesFencerMode(TestContext context) {
        LMRBCommonTests.axeActivatesFencerMode(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void bowDoesNotActivateFencer(TestContext context) {
        LMRBCommonTests.bowDoesNotActivateFencer(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void fencerTryAttackDamagesTarget(TestContext context) {
        LMRBCommonTests.fencerTryAttackDamagesTarget(context);
    }

    // ===== DMG: ダメージ処理 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void normalDamageFromMob(TestContext context) {
        LMRBCommonTests.normalDamageFromMob(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void immortalBlocksDamage(TestContext context) {
        LMRBCommonTests.immortalBlocksDamage(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void fallImmunityBlocksFallDamage(TestContext context) {
        LMRBCommonTests.fallImmunityBlocksFallDamage(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nonMobDamageImmunityBlocksNonMobDamage(TestContext context) {
        LMRBCommonTests.nonMobDamageImmunityBlocksNonMobDamage(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void damageWhileWaitingCancelsWait(TestContext context) {
        LMRBCommonTests.damageWhileWaitingCancelsWait(context);
    }

    // ===== SOUL: 死亡・魂生成 =====

    @GameTest(templateName = SMALL_FLOOR, tickLimit = 200)
    public static void tamedMaidDeathCreatesSoul(TestContext context) {
        LMRBCommonTests.tamedMaidDeathCreatesSoul(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void wildMaidDeathDoesNotCreateSoul(TestContext context) {
        LMRBCommonTests.wildMaidDeathDoesNotCreateSoul(context);
    }

    // ===== NBT: 読み書き =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void nbtPreservesTameState(TestContext context) {
        LMRBCommonTests.nbtPreservesTameState(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nbtPreservesWaitState(TestContext context) {
        LMRBCommonTests.nbtPreservesWaitState(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nbtPreservesMovingMode(TestContext context) {
        LMRBCommonTests.nbtPreservesMovingMode(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nbtPreservesStrike(TestContext context) {
        LMRBCommonTests.nbtPreservesStrike(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nbtPreservesBloodSuck(TestContext context) {
        LMRBCommonTests.nbtPreservesBloodSuck(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nbtPreservesInventory(TestContext context) {
        LMRBCommonTests.nbtPreservesInventory(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void nbtPreservesExperience(TestContext context) {
        LMRBCommonTests.nbtPreservesExperience(context);
    }

    // ===== ESC: 追従 Goal =====

    @GameTest(templateName = FLOOR)
    public static void followGoalStartsWhenFar(TestContext context) {
        LMRBCommonTests.followGoalStartsWhenFar(context);
    }

    @GameTest(templateName = FLOOR)
    public static void followGoalDoesNotStartWhenClose(TestContext context) {
        LMRBCommonTests.followGoalDoesNotStartWhenClose(context);
    }

    @GameTest(templateName = FLOOR)
    public static void followGoalDoesNotStartWhenWaiting(TestContext context) {
        LMRBCommonTests.followGoalDoesNotStartWhenWaiting(context);
    }

    @GameTest(templateName = FLOOR)
    public static void followGoalDoesNotStartInFreedom(TestContext context) {
        LMRBCommonTests.followGoalDoesNotStartInFreedom(context);
    }

    // ===== TP: テレポート Goal =====

    @GameTest(templateName = FLOOR)
    public static void teleportGoalStartsWhenFar(TestContext context) {
        LMRBCommonTests.teleportGoalStartsWhenFar(context);
    }

    @GameTest(templateName = FLOOR)
    public static void teleportGoalDoesNotStartWhenClose(TestContext context) {
        LMRBCommonTests.teleportGoalDoesNotStartWhenClose(context);
    }

    @GameTest(templateName = FLOOR)
    public static void teleportGoalDoesNotStartInFreedom(TestContext context) {
        LMRBCommonTests.teleportGoalDoesNotStartInFreedom(context);
    }

    @GameTest(templateName = FLOOR, tickLimit = 200)
    public static void teleportMovesToOwner(TestContext context) {
        LMRBCommonTests.teleportMovesToOwner(context);
    }

    // ===== FEN 追加: shouldExecute =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void fencerShouldExecuteWithTarget(TestContext context) {
        LMRBCommonTests.fencerShouldExecuteWithTarget(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void fencerShouldNotExecuteWithoutTarget(TestContext context) {
        LMRBCommonTests.fencerShouldNotExecuteWithoutTarget(context);
    }

    // ===== D 追加: ATTACK_PROHIBITED =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void attackProhibitedDamageAllowedByDefault(TestContext context) {
        LMRBCommonTests.attackProhibitedDamageAllowedByDefault(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void attackProhibitedDamageBlockedWithConfig(TestContext context) {
        LMRBCommonTests.attackProhibitedDamageBlockedWithConfig(context);
    }

    // ===== SOUL 追加 =====

    @GameTest(templateName = SMALL_FLOOR, tickLimit = 200)
    public static void soulPreservesOwnerUuid(TestContext context) {
        LMRBCommonTests.soulPreservesOwnerUuid(context);
    }

    // ===== MOV: Freedom / Tracer 切替 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void featherEscortToFreedom(TestContext context) {
        LMRBCommonTests.featherEscortToFreedom(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void featherFreedomToEscort(TestContext context) {
        LMRBCommonTests.featherFreedomToEscort(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void redstoneFreedomToTracer(TestContext context) {
        LMRBCommonTests.redstoneFreedomToTracer(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void redstoneTracerToFreedom(TestContext context) {
        LMRBCommonTests.redstoneTracerToFreedom(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void redstoneIgnoredInEscort(TestContext context) {
        LMRBCommonTests.redstoneIgnoredInEscort(context);
    }

    // ===== RIP: Ripper モード =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void shearsActivatesRipperMode(TestContext context) {
        LMRBCommonTests.shearsActivatesRipperMode(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void stoneDoesNotActivateRipper(TestContext context) {
        LMRBCommonTests.stoneDoesNotActivateRipper(context);
    }

    // ===== HEAL: Healer モード =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void foodActivatesHealerMode(TestContext context) {
        LMRBCommonTests.foodActivatesHealerMode(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void potionActivatesHealerMode(TestContext context) {
        LMRBCommonTests.potionActivatesHealerMode(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void swordDoesNotActivateHealer(TestContext context) {
        LMRBCommonTests.swordDoesNotActivateHealer(context);
    }

    // ===== PICK: ドロップアイテム拾い =====

    @GameTest(templateName = SMALL_FLOOR, tickLimit = 200)
    public static void tamedMaidPicksUpItem(TestContext context) {
        LMRBCommonTests.tamedMaidPicksUpItem(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void wildMaidDoesNotPickUpItem(TestContext context) {
        LMRBCommonTests.wildMaidDoesNotPickUpItem(context);
    }

    // ===== SAL: お給料消費・ストライキ =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void salaryConsumedFromInventory(TestContext context) {
        LMRBCommonTests.salaryConsumedFromInventory(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void unpaidTimesIncreasesWithoutSalary(TestContext context) {
        LMRBCommonTests.unpaidTimesIncreasesWithoutSalary(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void strikeOnExceedingUnpaidLimit(TestContext context) {
        LMRBCommonTests.strikeOnExceedingUnpaidLimit(context);
    }

    // ===== COOK: Cooking モード判定 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void bowlActivatesCookingMode(TestContext context) {
        LMRBCommonTests.bowlActivatesCookingMode(context);
    }

    // ===== PHARM: Pharmacist モード判定 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void waterBottleActivatesPharmacistMode(TestContext context) {
        LMRBCommonTests.waterBottleActivatesPharmacistMode(context);
    }

    // ===== TORCH: Torcher モード判定 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void torchActivatesTorcherMode(TestContext context) {
        LMRBCommonTests.torchActivatesTorcherMode(context);
    }

    // ===== ARCH: Archer モード判定 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void bowActivatesArcherMode(TestContext context) {
        LMRBCommonTests.bowActivatesArcherMode(context);
    }

    // ===== INT: インタラクション各種 =====

    @GameTest(templateName = SMALL_FLOOR)
    public static void saddleStartsRiding(TestContext context) {
        LMRBCommonTests.saddleStartsRiding(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void glassBottleConvertsToExpBottle(TestContext context) {
        LMRBCommonTests.glassBottleConvertsToExpBottle(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void gunpowderSetsAcceleration(TestContext context) {
        LMRBCommonTests.gunpowderSetsAcceleration(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void bucketConvertedToMilk(TestContext context) {
        LMRBCommonTests.bucketConvertedToMilk(context);
    }

    // ===== MISC: その他 =====

    @GameTest(templateName = SMALL_FLOOR, tickLimit = 200)
    public static void deathDropsInventory(TestContext context) {
        LMRBCommonTests.deathDropsInventory(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void validNaturalSpawnCondition(TestContext context) {
        LMRBCommonTests.validNaturalSpawnCondition(context);
    }

    @GameTest(templateName = SMALL_FLOOR)
    public static void invalidNaturalSpawnNoSolidBlock(TestContext context) {
        LMRBCommonTests.invalidNaturalSpawnNoSolidBlock(context);
    }

    @GameTest(templateName = SMALL_FLOOR, tickLimit = 200)
    public static void maidPicksUpExperienceOrb(TestContext context) {
        LMRBCommonTests.maidPicksUpExperienceOrb(context);
    }

    // ===== COOK: かまど作業 =====

    @GameTest(templateName = SMALL_FLOOR, maxAttempts = 3)
    public static void cookingInsertItems(TestContext context) {
        LMRBCommonTests.cookingInsertItems(context);
    }

    @GameTest(templateName = SMALL_FLOOR, tickLimit = 300, maxAttempts = 3)
    public static void cookingSmeltAndExtract(TestContext context) {
        LMRBCommonTests.cookingSmeltAndExtract(context);
    }

    // ===== PHARM: 醸造台作業 =====

    @GameTest(templateName = SMALL_FLOOR, maxAttempts = 3)
    public static void pharmacistInsertItems(TestContext context) {
        LMRBCommonTests.pharmacistInsertItems(context);
    }

    // ===== STORE: チェストに格納 =====

    @GameTest(templateName = SMALL_FLOOR, maxAttempts = 3)
    public static void storeItemToChest(TestContext context) {
        LMRBCommonTests.storeItemToChest(context);
    }

    // ===== ARCH: 射撃 =====

    private static final String ARCHER_ARENA = "archer_arena";

    @GameTest(templateName = ARCHER_ARENA, maxAttempts = 3)
    public static void archerShootsDamagesTarget(TestContext context) {
        LMRBCommonTests.archerShootsDamagesTarget(context);
    }
}
