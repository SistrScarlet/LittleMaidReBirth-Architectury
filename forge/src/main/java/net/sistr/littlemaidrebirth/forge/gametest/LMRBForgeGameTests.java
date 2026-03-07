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

  // ===== 基本 =====

  @GameTest(templateName = "empty")
  public static void maidSpawn(TestContext context) {
    LMRBCommonTests.maidSpawn(context);
  }

  // ===== E: 緊急状態判定 =====

  @GameTest(templateName = "empty")
  public static void emergencyAtThreshold(TestContext context) {
    LMRBCommonTests.emergencyAtThreshold(context);
  }

  @GameTest(templateName = "empty")
  public static void emergencyBelowThreshold(TestContext context) {
    LMRBCommonTests.emergencyBelowThreshold(context);
  }

  @GameTest(templateName = "empty")
  public static void notEmergencyAboveThreshold(TestContext context) {
    LMRBCommonTests.notEmergencyAboveThreshold(context);
  }

  @GameTest(templateName = "empty")
  public static void notEmergencyFullHealth(TestContext context) {
    LMRBCommonTests.notEmergencyFullHealth(context);
  }

  // ===== C: 雇用・再雇用 =====

  @GameTest(templateName = "empty")
  public static void contractWithCake(TestContext context) {
    LMRBCommonTests.contractWithCake(context);
  }

  @GameTest(templateName = "empty")
  public static void recontractFromStrike(TestContext context) {
    LMRBCommonTests.recontractFromStrike(context);
  }

  @GameTest(templateName = "empty")
  public static void cannotContractWithNonEmployItem(TestContext context) {
    LMRBCommonTests.cannotContractWithNonEmployItem(context);
  }

  @GameTest(templateName = "empty")
  public static void nonOwnerCannotInteract(TestContext context) {
    LMRBCommonTests.nonOwnerCannotInteract(context);
  }

  @GameTest(templateName = "empty")
  public static void sneakingSkipsInteraction(TestContext context) {
    LMRBCommonTests.sneakingSkipsInteraction(context);
  }

  // ===== W: 待機切替 =====

  @GameTest(templateName = "empty")
  public static void sugarTogglesWaitOn(TestContext context) {
    LMRBCommonTests.sugarTogglesWaitOn(context);
  }

  @GameTest(templateName = "empty")
  public static void sugarTogglesWaitOff(TestContext context) {
    LMRBCommonTests.sugarTogglesWaitOff(context);
  }

  @GameTest(templateName = "empty")
  public static void sugarHeals(TestContext context) {
    LMRBCommonTests.sugarHeals(context);
  }

  @GameTest(templateName = "empty")
  public static void strikeBlocksSugar(TestContext context) {
    LMRBCommonTests.strikeBlocksSugar(context);
  }

  @GameTest(templateName = "empty")
  public static void nonOwnerCannotUseSugar(TestContext context) {
    LMRBCommonTests.nonOwnerCannotUseSugar(context);
  }

  // ===== F: isFriend =====

  @GameTest(templateName = "empty")
  public static void ownerIsFriend(TestContext context) {
    LMRBCommonTests.ownerIsFriend(context);
  }

  @GameTest(templateName = "empty")
  public static void sameOwnerTamedMobIsFriend(TestContext context) {
    LMRBCommonTests.sameOwnerTamedMobIsFriend(context);
  }

  @GameTest(templateName = "empty")
  public static void anyTamedMobIsFriend(TestContext context) {
    LMRBCommonTests.anyTamedMobIsFriend(context);
  }

  @GameTest(templateName = "empty")
  public static void anyPlayerIsFriendWhenTamed(TestContext context) {
    LMRBCommonTests.anyPlayerIsFriendWhenTamed(context);
  }

  @GameTest(templateName = "empty")
  public static void wildMobIsNotFriend(TestContext context) {
    LMRBCommonTests.wildMobIsNotFriend(context);
  }

  @GameTest(templateName = "empty")
  public static void playerNotFriendWhenWild(TestContext context) {
    LMRBCommonTests.playerNotFriendWhenWild(context);
  }

  @GameTest(templateName = "empty")
  public static void wildMobNotFriendWhenWild(TestContext context) {
    LMRBCommonTests.wildMobNotFriendWhenWild(context);
  }

  // ===== T: canTarget =====

  @GameTest(templateName = "empty")
  public static void cannotTargetOwner(TestContext context) {
    LMRBCommonTests.cannotTargetOwner(context);
  }

  @GameTest(templateName = "empty")
  public static void cannotTargetSameOwnerTamedMob(TestContext context) {
    LMRBCommonTests.cannotTargetSameOwnerTamedMob(context);
  }

  @GameTest(templateName = "empty")
  public static void cannotTargetAnyTamedMob(TestContext context) {
    LMRBCommonTests.cannotTargetAnyTamedMob(context);
  }

  @GameTest(templateName = "empty")
  public static void canTargetWildHostileMob(TestContext context) {
    LMRBCommonTests.canTargetWildHostileMob(context);
  }

  // ===== P2: FakePlayer ワールド登録検証 =====

  @GameTest(templateName = "empty")
  public static void fakePlayerInWorldPlayers(TestContext context) {
    LMRBCommonTests.fakePlayerInWorldPlayers(context);
  }

  @GameTest(templateName = "empty")
  public static void getTameOwnerReturnsPlayer(TestContext context) {
    LMRBCommonTests.getTameOwnerReturnsPlayer(context);
  }

  // ===== D: damage チェック =====

  @GameTest(templateName = "empty")
  public static void friendDamageBlockedByDefault(TestContext context) {
    LMRBCommonTests.friendDamageBlockedByDefault(context);
  }

  @GameTest(templateName = "empty")
  public static void friendDamageAllowedWithConfig(TestContext context) {
    LMRBCommonTests.friendDamageAllowedWithConfig(context);
  }

  // ===== FEN: Fencer モード =====

  @GameTest(templateName = "empty")
  public static void swordActivatesFencerMode(TestContext context) {
    LMRBCommonTests.swordActivatesFencerMode(context);
  }

  @GameTest(templateName = "empty")
  public static void axeActivatesFencerMode(TestContext context) {
    LMRBCommonTests.axeActivatesFencerMode(context);
  }

  @GameTest(templateName = "empty")
  public static void bowDoesNotActivateFencer(TestContext context) {
    LMRBCommonTests.bowDoesNotActivateFencer(context);
  }

  @GameTest(templateName = "empty")
  public static void fencerTryAttackDamagesTarget(TestContext context) {
    LMRBCommonTests.fencerTryAttackDamagesTarget(context);
  }

  // ===== DMG: ダメージ処理 =====

  @GameTest(templateName = "empty")
  public static void normalDamageFromMob(TestContext context) {
    LMRBCommonTests.normalDamageFromMob(context);
  }

  @GameTest(templateName = "empty")
  public static void immortalBlocksDamage(TestContext context) {
    LMRBCommonTests.immortalBlocksDamage(context);
  }

  @GameTest(templateName = "empty")
  public static void fallImmunityBlocksFallDamage(TestContext context) {
    LMRBCommonTests.fallImmunityBlocksFallDamage(context);
  }

  @GameTest(templateName = "empty")
  public static void nonMobDamageImmunityBlocksNonMobDamage(TestContext context) {
    LMRBCommonTests.nonMobDamageImmunityBlocksNonMobDamage(context);
  }

  @GameTest(templateName = "empty")
  public static void damageWhileWaitingCancelsWait(TestContext context) {
    LMRBCommonTests.damageWhileWaitingCancelsWait(context);
  }

  // ===== SOUL: 死亡・魂生成 =====

  @GameTest(templateName = "empty", tickLimit = 200)
  public static void tamedMaidDeathCreatesSoul(TestContext context) {
    LMRBCommonTests.tamedMaidDeathCreatesSoul(context);
  }

  @GameTest(templateName = "empty")
  public static void wildMaidDeathDoesNotCreateSoul(TestContext context) {
    LMRBCommonTests.wildMaidDeathDoesNotCreateSoul(context);
  }

  // ===== NBT: 読み書き =====

  @GameTest(templateName = "empty")
  public static void nbtPreservesTameState(TestContext context) {
    LMRBCommonTests.nbtPreservesTameState(context);
  }

  @GameTest(templateName = "empty")
  public static void nbtPreservesWaitState(TestContext context) {
    LMRBCommonTests.nbtPreservesWaitState(context);
  }

  @GameTest(templateName = "empty")
  public static void nbtPreservesMovingMode(TestContext context) {
    LMRBCommonTests.nbtPreservesMovingMode(context);
  }

  @GameTest(templateName = "empty")
  public static void nbtPreservesStrike(TestContext context) {
    LMRBCommonTests.nbtPreservesStrike(context);
  }

  @GameTest(templateName = "empty")
  public static void nbtPreservesBloodSuck(TestContext context) {
    LMRBCommonTests.nbtPreservesBloodSuck(context);
  }

  @GameTest(templateName = "empty")
  public static void nbtPreservesInventory(TestContext context) {
    LMRBCommonTests.nbtPreservesInventory(context);
  }

  @GameTest(templateName = "empty")
  public static void nbtPreservesExperience(TestContext context) {
    LMRBCommonTests.nbtPreservesExperience(context);
  }

  // ===== ESC: 追従 Goal =====

  @GameTest(templateName = "floor")
  public static void followGoalStartsWhenFar(TestContext context) {
    LMRBCommonTests.followGoalStartsWhenFar(context);
  }

  @GameTest(templateName = "floor")
  public static void followGoalDoesNotStartWhenClose(TestContext context) {
    LMRBCommonTests.followGoalDoesNotStartWhenClose(context);
  }

  @GameTest(templateName = "floor")
  public static void followGoalDoesNotStartWhenWaiting(TestContext context) {
    LMRBCommonTests.followGoalDoesNotStartWhenWaiting(context);
  }

  @GameTest(templateName = "floor")
  public static void followGoalDoesNotStartInFreedom(TestContext context) {
    LMRBCommonTests.followGoalDoesNotStartInFreedom(context);
  }

  // ===== TP: テレポート Goal =====

  @GameTest(templateName = "floor")
  public static void teleportGoalStartsWhenFar(TestContext context) {
    LMRBCommonTests.teleportGoalStartsWhenFar(context);
  }

  @GameTest(templateName = "floor")
  public static void teleportGoalDoesNotStartWhenClose(TestContext context) {
    LMRBCommonTests.teleportGoalDoesNotStartWhenClose(context);
  }

  @GameTest(templateName = "floor")
  public static void teleportGoalDoesNotStartInFreedom(TestContext context) {
    LMRBCommonTests.teleportGoalDoesNotStartInFreedom(context);
  }

  @GameTest(templateName = "floor", tickLimit = 200)
  public static void teleportMovesToOwner(TestContext context) {
    LMRBCommonTests.teleportMovesToOwner(context);
  }

  // ===== FEN 追加: shouldExecute =====

  @GameTest(templateName = "empty")
  public static void fencerShouldExecuteWithTarget(TestContext context) {
    LMRBCommonTests.fencerShouldExecuteWithTarget(context);
  }

  @GameTest(templateName = "empty")
  public static void fencerShouldNotExecuteWithoutTarget(TestContext context) {
    LMRBCommonTests.fencerShouldNotExecuteWithoutTarget(context);
  }

  // ===== D 追加: ATTACK_PROHIBITED =====

  @GameTest(templateName = "empty")
  public static void attackProhibitedDamageAllowedByDefault(TestContext context) {
    LMRBCommonTests.attackProhibitedDamageAllowedByDefault(context);
  }

  @GameTest(templateName = "empty")
  public static void attackProhibitedDamageBlockedWithConfig(TestContext context) {
    LMRBCommonTests.attackProhibitedDamageBlockedWithConfig(context);
  }

  // ===== SOUL 追加 =====

  @GameTest(templateName = "empty", tickLimit = 200)
  public static void soulPreservesOwnerUuid(TestContext context) {
    LMRBCommonTests.soulPreservesOwnerUuid(context);
  }
}
