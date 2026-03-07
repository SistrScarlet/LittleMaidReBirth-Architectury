package net.sistr.littlemaidrebirth.fabric.gametest;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.sistr.littlemaidrebirth.gametest.LMRBCommonTests;

public class LMRBGameTests implements FabricGameTest {

  // ===== 基本 =====

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void maidSpawn(TestContext context) {
    LMRBCommonTests.maidSpawn(context);
  }

  // ===== E: 緊急状態判定 =====

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void emergencyAtThreshold(TestContext context) {
    LMRBCommonTests.emergencyAtThreshold(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void emergencyBelowThreshold(TestContext context) {
    LMRBCommonTests.emergencyBelowThreshold(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void notEmergencyAboveThreshold(TestContext context) {
    LMRBCommonTests.notEmergencyAboveThreshold(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void notEmergencyFullHealth(TestContext context) {
    LMRBCommonTests.notEmergencyFullHealth(context);
  }

  // ===== C: 雇用・再雇用 =====

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void contractWithCake(TestContext context) {
    LMRBCommonTests.contractWithCake(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void recontractFromStrike(TestContext context) {
    LMRBCommonTests.recontractFromStrike(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void cannotContractWithNonEmployItem(TestContext context) {
    LMRBCommonTests.cannotContractWithNonEmployItem(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void nonOwnerCannotInteract(TestContext context) {
    LMRBCommonTests.nonOwnerCannotInteract(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void sneakingSkipsInteraction(TestContext context) {
    LMRBCommonTests.sneakingSkipsInteraction(context);
  }

  // ===== W: 待機切替 =====

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void sugarTogglesWaitOn(TestContext context) {
    LMRBCommonTests.sugarTogglesWaitOn(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void sugarTogglesWaitOff(TestContext context) {
    LMRBCommonTests.sugarTogglesWaitOff(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void sugarHeals(TestContext context) {
    LMRBCommonTests.sugarHeals(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void strikeBlocksSugar(TestContext context) {
    LMRBCommonTests.strikeBlocksSugar(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void nonOwnerCannotUseSugar(TestContext context) {
    LMRBCommonTests.nonOwnerCannotUseSugar(context);
  }

  // ===== F: isFriend =====

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void ownerIsFriend(TestContext context) {
    LMRBCommonTests.ownerIsFriend(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void sameOwnerTamedMobIsFriend(TestContext context) {
    LMRBCommonTests.sameOwnerTamedMobIsFriend(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void anyTamedMobIsFriend(TestContext context) {
    LMRBCommonTests.anyTamedMobIsFriend(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void anyPlayerIsFriendWhenTamed(TestContext context) {
    LMRBCommonTests.anyPlayerIsFriendWhenTamed(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void wildMobIsNotFriend(TestContext context) {
    LMRBCommonTests.wildMobIsNotFriend(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void playerNotFriendWhenWild(TestContext context) {
    LMRBCommonTests.playerNotFriendWhenWild(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void wildMobNotFriendWhenWild(TestContext context) {
    LMRBCommonTests.wildMobNotFriendWhenWild(context);
  }

  // ===== T: canTarget =====

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void cannotTargetOwner(TestContext context) {
    LMRBCommonTests.cannotTargetOwner(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void cannotTargetSameOwnerTamedMob(TestContext context) {
    LMRBCommonTests.cannotTargetSameOwnerTamedMob(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void cannotTargetAnyTamedMob(TestContext context) {
    LMRBCommonTests.cannotTargetAnyTamedMob(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void canTargetWildHostileMob(TestContext context) {
    LMRBCommonTests.canTargetWildHostileMob(context);
  }

  // ===== P2: FakePlayer ワールド登録検証 =====

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void fakePlayerInWorldPlayers(TestContext context) {
    LMRBCommonTests.fakePlayerInWorldPlayers(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void getTameOwnerReturnsPlayer(TestContext context) {
    LMRBCommonTests.getTameOwnerReturnsPlayer(context);
  }

  // ===== D: damage チェック =====

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void friendDamageBlockedByDefault(TestContext context) {
    LMRBCommonTests.friendDamageBlockedByDefault(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void friendDamageAllowedWithConfig(TestContext context) {
    LMRBCommonTests.friendDamageAllowedWithConfig(context);
  }

  // ===== FEN: Fencer モード =====

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void swordActivatesFencerMode(TestContext context) {
    LMRBCommonTests.swordActivatesFencerMode(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void axeActivatesFencerMode(TestContext context) {
    LMRBCommonTests.axeActivatesFencerMode(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void bowDoesNotActivateFencer(TestContext context) {
    LMRBCommonTests.bowDoesNotActivateFencer(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void fencerTryAttackDamagesTarget(TestContext context) {
    LMRBCommonTests.fencerTryAttackDamagesTarget(context);
  }

  // ===== DMG: ダメージ処理 =====

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void normalDamageFromMob(TestContext context) {
    LMRBCommonTests.normalDamageFromMob(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void immortalBlocksDamage(TestContext context) {
    LMRBCommonTests.immortalBlocksDamage(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void fallImmunityBlocksFallDamage(TestContext context) {
    LMRBCommonTests.fallImmunityBlocksFallDamage(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void nonMobDamageImmunityBlocksNonMobDamage(TestContext context) {
    LMRBCommonTests.nonMobDamageImmunityBlocksNonMobDamage(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void damageWhileWaitingCancelsWait(TestContext context) {
    LMRBCommonTests.damageWhileWaitingCancelsWait(context);
  }

  // ===== SOUL: 死亡・魂生成 =====

  @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
  public void tamedMaidDeathCreatesSoul(TestContext context) {
    LMRBCommonTests.tamedMaidDeathCreatesSoul(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void wildMaidDeathDoesNotCreateSoul(TestContext context) {
    LMRBCommonTests.wildMaidDeathDoesNotCreateSoul(context);
  }

  // ===== NBT: 読み書き =====

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void nbtPreservesTameState(TestContext context) {
    LMRBCommonTests.nbtPreservesTameState(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void nbtPreservesWaitState(TestContext context) {
    LMRBCommonTests.nbtPreservesWaitState(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void nbtPreservesMovingMode(TestContext context) {
    LMRBCommonTests.nbtPreservesMovingMode(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void nbtPreservesStrike(TestContext context) {
    LMRBCommonTests.nbtPreservesStrike(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void nbtPreservesBloodSuck(TestContext context) {
    LMRBCommonTests.nbtPreservesBloodSuck(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void nbtPreservesInventory(TestContext context) {
    LMRBCommonTests.nbtPreservesInventory(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void nbtPreservesExperience(TestContext context) {
    LMRBCommonTests.nbtPreservesExperience(context);
  }

  // ===== ESC: 追従 Goal =====

  @GameTest(templateName = "littlemaidrebirth:floor")
  public void followGoalStartsWhenFar(TestContext context) {
    LMRBCommonTests.followGoalStartsWhenFar(context);
  }

  @GameTest(templateName = "littlemaidrebirth:floor")
  public void followGoalDoesNotStartWhenClose(TestContext context) {
    LMRBCommonTests.followGoalDoesNotStartWhenClose(context);
  }

  @GameTest(templateName = "littlemaidrebirth:floor")
  public void followGoalDoesNotStartWhenWaiting(TestContext context) {
    LMRBCommonTests.followGoalDoesNotStartWhenWaiting(context);
  }

  @GameTest(templateName = "littlemaidrebirth:floor")
  public void followGoalDoesNotStartInFreedom(TestContext context) {
    LMRBCommonTests.followGoalDoesNotStartInFreedom(context);
  }

  // ===== TP: テレポート Goal =====

  @GameTest(templateName = "littlemaidrebirth:floor")
  public void teleportGoalStartsWhenFar(TestContext context) {
    LMRBCommonTests.teleportGoalStartsWhenFar(context);
  }

  @GameTest(templateName = "littlemaidrebirth:floor")
  public void teleportGoalDoesNotStartWhenClose(TestContext context) {
    LMRBCommonTests.teleportGoalDoesNotStartWhenClose(context);
  }

  @GameTest(templateName = "littlemaidrebirth:floor")
  public void teleportGoalDoesNotStartInFreedom(TestContext context) {
    LMRBCommonTests.teleportGoalDoesNotStartInFreedom(context);
  }

  @GameTest(templateName = "littlemaidrebirth:floor", tickLimit = 200)
  public void teleportMovesToOwner(TestContext context) {
    LMRBCommonTests.teleportMovesToOwner(context);
  }

  // ===== FEN 追加: shouldExecute =====

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void fencerShouldExecuteWithTarget(TestContext context) {
    LMRBCommonTests.fencerShouldExecuteWithTarget(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void fencerShouldNotExecuteWithoutTarget(TestContext context) {
    LMRBCommonTests.fencerShouldNotExecuteWithoutTarget(context);
  }

  // ===== D 追加: ATTACK_PROHIBITED =====

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void attackProhibitedDamageAllowedByDefault(TestContext context) {
    LMRBCommonTests.attackProhibitedDamageAllowedByDefault(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void attackProhibitedDamageBlockedWithConfig(TestContext context) {
    LMRBCommonTests.attackProhibitedDamageBlockedWithConfig(context);
  }

  // ===== SOUL 追加 =====

  @GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 200)
  public void soulPreservesOwnerUuid(TestContext context) {
    LMRBCommonTests.soulPreservesOwnerUuid(context);
  }
}
