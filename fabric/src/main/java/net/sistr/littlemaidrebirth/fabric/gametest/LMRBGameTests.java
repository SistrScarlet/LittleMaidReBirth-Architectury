package net.sistr.littlemaidrebirth.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.sistr.littlemaidrebirth.gametest.LMRBCommonTests;

public class LMRBGameTests implements FabricGameTest {

  private static final String SMALL_FLOOR = "littlemaidrebirth:small_floor";
  private static final String FLOOR = "littlemaidrebirth:floor";

  // ===== 基本 =====

  @GameTest(templateName = SMALL_FLOOR)
  public void maidSpawn(TestContext context) {
    LMRBCommonTests.maidSpawn(context);
  }

  // ===== E: 緊急状態判定 =====

  @GameTest(templateName = SMALL_FLOOR)
  public void emergencyAtThreshold(TestContext context) {
    LMRBCommonTests.emergencyAtThreshold(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void emergencyBelowThreshold(TestContext context) {
    LMRBCommonTests.emergencyBelowThreshold(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void notEmergencyAboveThreshold(TestContext context) {
    LMRBCommonTests.notEmergencyAboveThreshold(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void notEmergencyFullHealth(TestContext context) {
    LMRBCommonTests.notEmergencyFullHealth(context);
  }

  // ===== C: 雇用・再雇用 =====

  @GameTest(templateName = SMALL_FLOOR)
  public void contractWithCake(TestContext context) {
    LMRBCommonTests.contractWithCake(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void recontractFromStrike(TestContext context) {
    LMRBCommonTests.recontractFromStrike(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void cannotContractWithNonEmployItem(TestContext context) {
    LMRBCommonTests.cannotContractWithNonEmployItem(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void nonOwnerCannotInteract(TestContext context) {
    LMRBCommonTests.nonOwnerCannotInteract(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void sneakingSkipsInteraction(TestContext context) {
    LMRBCommonTests.sneakingSkipsInteraction(context);
  }

  // ===== W: 待機切替 =====

  @GameTest(templateName = SMALL_FLOOR)
  public void sugarTogglesWaitOn(TestContext context) {
    LMRBCommonTests.sugarTogglesWaitOn(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void sugarTogglesWaitOff(TestContext context) {
    LMRBCommonTests.sugarTogglesWaitOff(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void sugarHeals(TestContext context) {
    LMRBCommonTests.sugarHeals(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void strikeBlocksSugar(TestContext context) {
    LMRBCommonTests.strikeBlocksSugar(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void nonOwnerCannotUseSugar(TestContext context) {
    LMRBCommonTests.nonOwnerCannotUseSugar(context);
  }

  // ===== F: isFriend =====

  @GameTest(templateName = SMALL_FLOOR)
  public void ownerIsFriend(TestContext context) {
    LMRBCommonTests.ownerIsFriend(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void sameOwnerTamedMobIsFriend(TestContext context) {
    LMRBCommonTests.sameOwnerTamedMobIsFriend(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void anyTamedMobIsFriend(TestContext context) {
    LMRBCommonTests.anyTamedMobIsFriend(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void anyPlayerIsFriendWhenTamed(TestContext context) {
    LMRBCommonTests.anyPlayerIsFriendWhenTamed(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void wildMobIsNotFriend(TestContext context) {
    LMRBCommonTests.wildMobIsNotFriend(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void playerNotFriendWhenWild(TestContext context) {
    LMRBCommonTests.playerNotFriendWhenWild(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void wildMobNotFriendWhenWild(TestContext context) {
    LMRBCommonTests.wildMobNotFriendWhenWild(context);
  }

  // ===== T: canTarget =====

  @GameTest(templateName = SMALL_FLOOR)
  public void cannotTargetOwner(TestContext context) {
    LMRBCommonTests.cannotTargetOwner(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void cannotTargetSameOwnerTamedMob(TestContext context) {
    LMRBCommonTests.cannotTargetSameOwnerTamedMob(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void cannotTargetAnyTamedMob(TestContext context) {
    LMRBCommonTests.cannotTargetAnyTamedMob(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void canTargetWildHostileMob(TestContext context) {
    LMRBCommonTests.canTargetWildHostileMob(context);
  }

  // ===== P2: FakePlayer ワールド登録検証 =====

  @GameTest(templateName = SMALL_FLOOR)
  public void fakePlayerInWorldPlayers(TestContext context) {
    LMRBCommonTests.fakePlayerInWorldPlayers(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void getTameOwnerReturnsPlayer(TestContext context) {
    LMRBCommonTests.getTameOwnerReturnsPlayer(context);
  }

  // ===== D: damage チェック =====

  @GameTest(templateName = SMALL_FLOOR)
  public void friendDamageBlockedByDefault(TestContext context) {
    LMRBCommonTests.friendDamageBlockedByDefault(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void friendDamageAllowedWithConfig(TestContext context) {
    LMRBCommonTests.friendDamageAllowedWithConfig(context);
  }

  // ===== FEN: Fencer モード =====

  @GameTest(templateName = SMALL_FLOOR)
  public void swordActivatesFencerMode(TestContext context) {
    LMRBCommonTests.swordActivatesFencerMode(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void axeActivatesFencerMode(TestContext context) {
    LMRBCommonTests.axeActivatesFencerMode(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void bowDoesNotActivateFencer(TestContext context) {
    LMRBCommonTests.bowDoesNotActivateFencer(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void fencerTryAttackDamagesTarget(TestContext context) {
    LMRBCommonTests.fencerTryAttackDamagesTarget(context);
  }

  // ===== DMG: ダメージ処理 =====

  @GameTest(templateName = SMALL_FLOOR)
  public void normalDamageFromMob(TestContext context) {
    LMRBCommonTests.normalDamageFromMob(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void immortalBlocksDamage(TestContext context) {
    LMRBCommonTests.immortalBlocksDamage(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void fallImmunityBlocksFallDamage(TestContext context) {
    LMRBCommonTests.fallImmunityBlocksFallDamage(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void nonMobDamageImmunityBlocksNonMobDamage(TestContext context) {
    LMRBCommonTests.nonMobDamageImmunityBlocksNonMobDamage(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void damageWhileWaitingCancelsWait(TestContext context) {
    LMRBCommonTests.damageWhileWaitingCancelsWait(context);
  }

  // ===== SOUL: 死亡・魂生成 =====

  @GameTest(templateName = SMALL_FLOOR, tickLimit = 200)
  public void tamedMaidDeathCreatesSoul(TestContext context) {
    LMRBCommonTests.tamedMaidDeathCreatesSoul(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void wildMaidDeathDoesNotCreateSoul(TestContext context) {
    LMRBCommonTests.wildMaidDeathDoesNotCreateSoul(context);
  }

  // ===== NBT: 読み書き =====

  @GameTest(templateName = SMALL_FLOOR)
  public void nbtPreservesTameState(TestContext context) {
    LMRBCommonTests.nbtPreservesTameState(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void nbtPreservesWaitState(TestContext context) {
    LMRBCommonTests.nbtPreservesWaitState(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void nbtPreservesMovingMode(TestContext context) {
    LMRBCommonTests.nbtPreservesMovingMode(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void nbtPreservesStrike(TestContext context) {
    LMRBCommonTests.nbtPreservesStrike(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void nbtPreservesBloodSuck(TestContext context) {
    LMRBCommonTests.nbtPreservesBloodSuck(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void nbtPreservesInventory(TestContext context) {
    LMRBCommonTests.nbtPreservesInventory(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void nbtPreservesExperience(TestContext context) {
    LMRBCommonTests.nbtPreservesExperience(context);
  }

  // ===== ESC: 追従 Goal =====

  @GameTest(templateName = FLOOR)
  public void followGoalStartsWhenFar(TestContext context) {
    LMRBCommonTests.followGoalStartsWhenFar(context);
  }

  @GameTest(templateName = FLOOR)
  public void followGoalDoesNotStartWhenClose(TestContext context) {
    LMRBCommonTests.followGoalDoesNotStartWhenClose(context);
  }

  @GameTest(templateName = FLOOR)
  public void followGoalDoesNotStartWhenWaiting(TestContext context) {
    LMRBCommonTests.followGoalDoesNotStartWhenWaiting(context);
  }

  @GameTest(templateName = FLOOR)
  public void followGoalDoesNotStartInFreedom(TestContext context) {
    LMRBCommonTests.followGoalDoesNotStartInFreedom(context);
  }

  // ===== TP: テレポート Goal =====

  @GameTest(templateName = FLOOR)
  public void teleportGoalStartsWhenFar(TestContext context) {
    LMRBCommonTests.teleportGoalStartsWhenFar(context);
  }

  @GameTest(templateName = FLOOR)
  public void teleportGoalDoesNotStartWhenClose(TestContext context) {
    LMRBCommonTests.teleportGoalDoesNotStartWhenClose(context);
  }

  @GameTest(templateName = FLOOR)
  public void teleportGoalDoesNotStartInFreedom(TestContext context) {
    LMRBCommonTests.teleportGoalDoesNotStartInFreedom(context);
  }

  @GameTest(templateName = FLOOR, tickLimit = 200)
  public void teleportMovesToOwner(TestContext context) {
    LMRBCommonTests.teleportMovesToOwner(context);
  }

  // ===== FEN 追加: shouldExecute =====

  @GameTest(templateName = SMALL_FLOOR)
  public void fencerShouldExecuteWithTarget(TestContext context) {
    LMRBCommonTests.fencerShouldExecuteWithTarget(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void fencerShouldNotExecuteWithoutTarget(TestContext context) {
    LMRBCommonTests.fencerShouldNotExecuteWithoutTarget(context);
  }

  // ===== D 追加: ATTACK_PROHIBITED =====

  @GameTest(templateName = SMALL_FLOOR)
  public void attackProhibitedDamageAllowedByDefault(TestContext context) {
    LMRBCommonTests.attackProhibitedDamageAllowedByDefault(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void attackProhibitedDamageBlockedWithConfig(TestContext context) {
    LMRBCommonTests.attackProhibitedDamageBlockedWithConfig(context);
  }

  // ===== SOUL 追加 =====

  @GameTest(templateName = SMALL_FLOOR, tickLimit = 200)
  public void soulPreservesOwnerUuid(TestContext context) {
    LMRBCommonTests.soulPreservesOwnerUuid(context);
  }

  // ===== MOV: Freedom / Tracer 切替 =====

  @GameTest(templateName = SMALL_FLOOR)
  public void featherEscortToFreedom(TestContext context) {
    LMRBCommonTests.featherEscortToFreedom(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void featherFreedomToEscort(TestContext context) {
    LMRBCommonTests.featherFreedomToEscort(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void redstoneFreedomToTracer(TestContext context) {
    LMRBCommonTests.redstoneFreedomToTracer(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void redstoneTracerToFreedom(TestContext context) {
    LMRBCommonTests.redstoneTracerToFreedom(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void redstoneIgnoredInEscort(TestContext context) {
    LMRBCommonTests.redstoneIgnoredInEscort(context);
  }

  // ===== RIP: Ripper モード =====

  @GameTest(templateName = SMALL_FLOOR)
  public void shearsActivatesRipperMode(TestContext context) {
    LMRBCommonTests.shearsActivatesRipperMode(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void stoneDoesNotActivateRipper(TestContext context) {
    LMRBCommonTests.stoneDoesNotActivateRipper(context);
  }

  // ===== HEAL: Healer モード =====

  @GameTest(templateName = SMALL_FLOOR)
  public void foodActivatesHealerMode(TestContext context) {
    LMRBCommonTests.foodActivatesHealerMode(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void potionActivatesHealerMode(TestContext context) {
    LMRBCommonTests.potionActivatesHealerMode(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void swordDoesNotActivateHealer(TestContext context) {
    LMRBCommonTests.swordDoesNotActivateHealer(context);
  }

  // ===== PICK: ドロップアイテム拾い =====

  @GameTest(templateName = SMALL_FLOOR, tickLimit = 200)
  public void tamedMaidPicksUpItem(TestContext context) {
    LMRBCommonTests.tamedMaidPicksUpItem(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void wildMaidDoesNotPickUpItem(TestContext context) {
    LMRBCommonTests.wildMaidDoesNotPickUpItem(context);
  }

  // ===== SAL: お給料消費・ストライキ =====

  @GameTest(templateName = SMALL_FLOOR)
  public void salaryConsumedFromInventory(TestContext context) {
    LMRBCommonTests.salaryConsumedFromInventory(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void unpaidTimesIncreasesWithoutSalary(TestContext context) {
    LMRBCommonTests.unpaidTimesIncreasesWithoutSalary(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void strikeOnExceedingUnpaidLimit(TestContext context) {
    LMRBCommonTests.strikeOnExceedingUnpaidLimit(context);
  }

  // ===== COOK: Cooking モード判定 =====

  @GameTest(templateName = SMALL_FLOOR)
  public void bowlActivatesCookingMode(TestContext context) {
    LMRBCommonTests.bowlActivatesCookingMode(context);
  }

  // ===== PHARM: Pharmacist モード判定 =====

  @GameTest(templateName = SMALL_FLOOR)
  public void waterBottleActivatesPharmacistMode(TestContext context) {
    LMRBCommonTests.waterBottleActivatesPharmacistMode(context);
  }

  // ===== TORCH: Torcher モード判定 =====

  @GameTest(templateName = SMALL_FLOOR)
  public void torchActivatesTorcherMode(TestContext context) {
    LMRBCommonTests.torchActivatesTorcherMode(context);
  }

  // ===== ARCH: Archer モード判定 =====

  @GameTest(templateName = SMALL_FLOOR)
  public void bowActivatesArcherMode(TestContext context) {
    LMRBCommonTests.bowActivatesArcherMode(context);
  }

  // ===== INT: インタラクション各種 =====

  @GameTest(templateName = SMALL_FLOOR)
  public void saddleStartsRiding(TestContext context) {
    LMRBCommonTests.saddleStartsRiding(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void glassBottleConvertsToExpBottle(TestContext context) {
    LMRBCommonTests.glassBottleConvertsToExpBottle(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void gunpowderSetsAcceleration(TestContext context) {
    LMRBCommonTests.gunpowderSetsAcceleration(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void bucketConvertedToMilk(TestContext context) {
    LMRBCommonTests.bucketConvertedToMilk(context);
  }

  // ===== MISC: その他 =====

  @GameTest(templateName = SMALL_FLOOR, tickLimit = 200)
  public void deathDropsInventory(TestContext context) {
    LMRBCommonTests.deathDropsInventory(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void validNaturalSpawnCondition(TestContext context) {
    LMRBCommonTests.validNaturalSpawnCondition(context);
  }

  @GameTest(templateName = SMALL_FLOOR)
  public void invalidNaturalSpawnNoSolidBlock(TestContext context) {
    LMRBCommonTests.invalidNaturalSpawnNoSolidBlock(context);
  }

  @GameTest(templateName = SMALL_FLOOR, tickLimit = 200)
  public void maidPicksUpExperienceOrb(TestContext context) {
    LMRBCommonTests.maidPicksUpExperienceOrb(context);
  }

  // ===== COOK: かまど作業 =====

  @GameTest(templateName = SMALL_FLOOR, maxAttempts = 3)
  public void cookingInsertItems(TestContext context) {
    LMRBCommonTests.cookingInsertItems(context);
  }

  @GameTest(templateName = SMALL_FLOOR, tickLimit = 300, maxAttempts = 3)
  public void cookingSmeltAndExtract(TestContext context) {
    LMRBCommonTests.cookingSmeltAndExtract(context);
  }

  // ===== PHARM: 醸造台作業 =====

  @GameTest(templateName = SMALL_FLOOR, maxAttempts = 3)
  public void pharmacistInsertItems(TestContext context) {
    LMRBCommonTests.pharmacistInsertItems(context);
  }

  // ===== STORE: チェストに格納 =====

  @GameTest(templateName = SMALL_FLOOR, maxAttempts = 3)
  public void storeItemToChest(TestContext context) {
    LMRBCommonTests.storeItemToChest(context);
  }

  // ===== ARCH: 射撃 =====

  private static final String ARCHER_ARENA = "littlemaidrebirth:archer_arena";

  @GameTest(templateName = ARCHER_ARENA, maxAttempts = 3)
  public void archerShootsDamagesTarget(TestContext context) {
    LMRBCommonTests.archerShootsDamagesTarget(context);
  }
}
