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

  // ===== D: damage チェック =====

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void friendDamageBlockedByDefault(TestContext context) {
    LMRBCommonTests.friendDamageBlockedByDefault(context);
  }

  @GameTest(templateName = EMPTY_STRUCTURE)
  public void friendDamageAllowedWithConfig(TestContext context) {
    LMRBCommonTests.friendDamageAllowedWithConfig(context);
  }
}
