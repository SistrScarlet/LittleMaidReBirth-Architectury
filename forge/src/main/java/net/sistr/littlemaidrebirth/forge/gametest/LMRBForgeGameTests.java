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

  // ===== D: damage チェック =====

  @GameTest(templateName = "empty")
  public static void friendDamageBlockedByDefault(TestContext context) {
    LMRBCommonTests.friendDamageBlockedByDefault(context);
  }

  @GameTest(templateName = "empty")
  public static void friendDamageAllowedWithConfig(TestContext context) {
    LMRBCommonTests.friendDamageAllowedWithConfig(context);
  }
}
