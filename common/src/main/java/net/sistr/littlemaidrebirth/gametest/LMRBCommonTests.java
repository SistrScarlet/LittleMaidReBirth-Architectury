package net.sistr.littlemaidrebirth.gametest;

import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.config.LMRBConfig;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;
import net.sistr.littlemaidrebirth.setup.Registration;

public final class LMRBCommonTests {

  private static final BlockPos MAID_POS = new BlockPos(1, 1, 1);

  private LMRBCommonTests() {}

  // ===== ヘルパー =====

  private static LittleMaidEntity spawnMaid(TestContext context) {
    return context.spawnEntity(Registration.LITTLE_MAID_MOB.get(), MAID_POS);
  }

  private static ServerPlayerEntity createPlayer(TestContext context, String name) {
    return GameTestHelper.createFakePlayer(context.getWorld(), name);
  }

  private static void holdItem(ServerPlayerEntity player, ItemStack stack) {
    player.getInventory().setStack(player.getInventory().selectedSlot, stack);
  }

  private static LittleMaidEntity spawnTamedMaid(TestContext context, ServerPlayerEntity owner) {
    var maid = spawnMaid(context);
    maid.setOwnerUuid(owner.getUuid());
    return maid;
  }

  // ===== 基本 =====

  public static void maidSpawn(TestContext context) {
    var maid = spawnMaid(context);
    context.assertTrue(maid != null, "メイドさんがスポーンできること");
    context.complete();
  }

  // ===== E: 緊急状態判定 =====

  public static void emergencyAtThreshold(TestContext context) {
    var maid = spawnMaid(context);
    // デフォルト: maxHP=20, threshold=0.5 → 閾値=10
    maid.setHealth(10);
    context.assertTrue(maid.isEmergency(), "HP閾値ちょうどで緊急状態であること");
    context.complete();
  }

  public static void emergencyBelowThreshold(TestContext context) {
    var maid = spawnMaid(context);
    maid.setHealth(5);
    context.assertTrue(maid.isEmergency(), "HP閾値未満で緊急状態であること");
    context.complete();
  }

  public static void notEmergencyAboveThreshold(TestContext context) {
    var maid = spawnMaid(context);
    maid.setHealth(11);
    context.assertFalse(maid.isEmergency(), "HP閾値超で通常状態であること");
    context.complete();
  }

  public static void notEmergencyFullHealth(TestContext context) {
    var maid = spawnMaid(context);
    maid.setHealth(maid.getMaxHealth());
    context.assertFalse(maid.isEmergency(), "HP満タンで通常状態であること");
    context.complete();
  }

  // ===== C: 雇用・再雇用 =====

  public static void contractWithCake(TestContext context) {
    var maid = spawnMaid(context);
    var player = createPlayer(context, "test-owner");
    holdItem(player, new ItemStack(Items.CAKE));

    maid.interactMob(player, Hand.MAIN_HAND);

    context.assertTrue(maid.isTamed(), "テイム済みであること");
    context.assertTrue(
        TameableUtil.getTameOwnerUuid(maid).map(id -> id.equals(player.getUuid())).orElse(false),
        "オーナーがプレイヤーであること");
    context.assertTrue(maid.getMovingMode() == MovingMode.ESCORT, "移動モードがESCORTであること");
    context.assertTrue(maid.isContractMM(), "契約モデルであること");
    context.assertTrue(player.getMainHandStack().isEmpty(), "ケーキが消費されていること");
    context.complete();
  }

  public static void recontractFromStrike(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.setStrike(true);
    holdItem(player, new ItemStack(Items.CAKE));

    maid.interactMob(player, Hand.MAIN_HAND);

    context.assertFalse(maid.isStrike(), "ストライキが解除されていること");
    context.assertTrue(maid.isTamed(), "テイム状態が維持されていること");
    context.complete();
  }

  public static void cannotContractWithNonEmployItem(TestContext context) {
    var maid = spawnMaid(context);
    var player = createPlayer(context, "test-owner");
    holdItem(player, new ItemStack(Items.STONE));

    maid.interactMob(player, Hand.MAIN_HAND);

    context.assertFalse(maid.isTamed(), "テイムされていないこと");
    context.assertTrue(player.getMainHandStack().getCount() == 1, "石が消費されていないこと");
    context.complete();
  }

  public static void nonOwnerCannotInteract(TestContext context) {
    var owner = createPlayer(context, "test-owner");
    var stranger = createPlayer(context, "test-stranger");
    var maid = spawnTamedMaid(context, owner);
    holdItem(stranger, new ItemStack(Items.SUGAR));

    boolean wasSitting = maid.isSitting();
    maid.interactMob(stranger, Hand.MAIN_HAND);

    context.assertTrue(maid.isSitting() == wasSitting, "オーナー以外の操作で状態変化しないこと");
    context.complete();
  }

  public static void sneakingSkipsInteraction(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    player.setSneaking(true);
    holdItem(player, new ItemStack(Items.SUGAR));

    boolean wasSitting = maid.isSitting();
    maid.interactMob(player, Hand.MAIN_HAND);

    context.assertTrue(maid.isSitting() == wasSitting, "スニーク中に状態変化しないこと");
    context.complete();
  }

  // ===== W: 待機切替 =====

  public static void sugarTogglesWaitOn(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    holdItem(player, new ItemStack(Items.SUGAR, 2));

    maid.interactMob(player, Hand.MAIN_HAND);

    context.assertTrue(maid.isSitting(), "待機状態になること");
    context.assertTrue(player.getMainHandStack().getCount() == 1, "砂糖が1個消費されること");
    context.complete();
  }

  public static void sugarTogglesWaitOff(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    TameableUtil.setWait(maid, true);
    holdItem(player, new ItemStack(Items.SUGAR, 2));

    maid.interactMob(player, Hand.MAIN_HAND);

    context.assertFalse(maid.isSitting(), "待機が解除されること");
    context.complete();
  }

  public static void sugarHeals(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    LMRBConfig config = LMRBMod.getConfig();
    maid.setHealth(maid.getMaxHealth() - config.health.healAmount - 1);
    float healthBefore = maid.getHealth();
    holdItem(player, new ItemStack(Items.SUGAR));

    maid.interactMob(player, Hand.MAIN_HAND);

    context.assertTrue(maid.getHealth() > healthBefore, "HPが回復していること");
    context.complete();
  }

  public static void strikeBlocksSugar(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.setStrike(true);
    holdItem(player, new ItemStack(Items.SUGAR));

    boolean wasSitting = maid.isSitting();
    maid.interactMob(player, Hand.MAIN_HAND);

    context.assertTrue(maid.isSitting() == wasSitting, "ストライキ中は砂糖で状態変化しないこと");
    context.complete();
  }

  public static void nonOwnerCannotUseSugar(TestContext context) {
    var owner = createPlayer(context, "test-owner");
    var stranger = createPlayer(context, "test-stranger");
    var maid = spawnTamedMaid(context, owner);
    holdItem(stranger, new ItemStack(Items.SUGAR));

    boolean wasSitting = maid.isSitting();
    maid.interactMob(stranger, Hand.MAIN_HAND);

    context.assertTrue(maid.isSitting() == wasSitting, "オーナー以外は砂糖で操作できないこと");
    context.complete();
  }

  // ===== F: isFriend =====

  public static void ownerIsFriend(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);

    context.assertTrue(TameableUtil.isFriend(maid, player), "ご主人はフレンドであること");
    context.complete();
  }

  public static void sameOwnerTamedMobIsFriend(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    var wolf = context.spawnEntity(EntityType.WOLF, new BlockPos(3, 1, 1));
    wolf.setOwnerUuid(player.getUuid());

    context.assertTrue(TameableUtil.isFriend(maid, wolf), "同オーナーのテイム済みモブはフレンドであること");
    context.complete();
  }

  public static void anyTamedMobIsFriend(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var otherPlayer = createPlayer(context, "test-other");
    var maid = spawnTamedMaid(context, player);
    var wolf = context.spawnEntity(EntityType.WOLF, new BlockPos(3, 1, 1));
    wolf.setOwnerUuid(otherPlayer.getUuid());

    context.assertTrue(TameableUtil.isFriend(maid, wolf), "テイム済みモブ全般はフレンドであること");
    context.complete();
  }

  public static void anyPlayerIsFriendWhenTamed(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var stranger = createPlayer(context, "test-stranger");
    var maid = spawnTamedMaid(context, player);

    context.assertTrue(TameableUtil.isFriend(maid, stranger), "オーナーあり時は任意プレイヤーがフレンドであること");
    context.complete();
  }

  public static void wildMobIsNotFriend(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    var zombie = context.spawnEntity(EntityType.ZOMBIE, new BlockPos(3, 1, 1));

    context.assertFalse(TameableUtil.isFriend(maid, zombie), "野生モブはフレンドでないこと");
    context.complete();
  }

  public static void playerNotFriendWhenWild(TestContext context) {
    var maid = spawnMaid(context);
    var player = createPlayer(context, "test-player");

    context.assertFalse(TameableUtil.isFriend(maid, player), "オーナーなし時はプレイヤーはフレンドでないこと");
    context.complete();
  }

  public static void wildMobNotFriendWhenWild(TestContext context) {
    var maid = spawnMaid(context);
    var zombie = context.spawnEntity(EntityType.ZOMBIE, new BlockPos(3, 1, 1));

    context.assertFalse(TameableUtil.isFriend(maid, zombie), "オーナーなし+野生モブはフレンドでないこと");
    context.complete();
  }

  // ===== T: canTarget =====

  public static void cannotTargetOwner(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);

    context.assertFalse(maid.canTarget(player), "ご主人を攻撃対象にしないこと");
    context.complete();
  }

  public static void cannotTargetSameOwnerTamedMob(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    var wolf = context.spawnEntity(EntityType.WOLF, new BlockPos(3, 1, 1));
    wolf.setOwnerUuid(player.getUuid());

    context.assertFalse(maid.canTarget(wolf), "同オーナーのテイム済みモブを攻撃対象にしないこと");
    context.complete();
  }

  public static void cannotTargetAnyTamedMob(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var otherPlayer = createPlayer(context, "test-other");
    var maid = spawnTamedMaid(context, player);
    var wolf = context.spawnEntity(EntityType.WOLF, new BlockPos(3, 1, 1));
    wolf.setOwnerUuid(otherPlayer.getUuid());

    context.assertFalse(maid.canTarget(wolf), "テイム済みモブ全般を攻撃対象にしないこと");
    context.complete();
  }

  public static void canTargetWildHostileMob(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    var zombie = context.spawnEntity(EntityType.ZOMBIE, new BlockPos(3, 1, 1));

    context.assertTrue(maid.canTarget(zombie), "野生の敵対モブを攻撃対象にすること");
    context.complete();
  }

  // ===== D: damage フレンド/ATTACK_PROHIBITED チェック =====

  public static void friendDamageBlockedByDefault(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    float healthBefore = maid.getHealth();

    maid.damage(player.getDamageSources().playerAttack(player), 5.0f);

    context.assertTrue(maid.getHealth() == healthBefore, "デフォルトではフレンドからのダメージを受けないこと");
    context.complete();
  }

  public static void friendDamageAllowedWithConfig(TestContext context) {
    var config = LMRBMod.getConfig();
    boolean original = config.health.enableFriendlyFire;
    try {
      config.health.enableFriendlyFire = true;

      var player = createPlayer(context, "test-owner");
      var maid = spawnTamedMaid(context, player);
      float healthBefore = maid.getHealth();

      maid.damage(player.getDamageSources().playerAttack(player), 5.0f);

      context.assertTrue(
          maid.getHealth() < healthBefore, "enableFriendlyFire=trueならフレンドからダメージを受けること");
    } finally {
      config.health.enableFriendlyFire = original;
    }
    context.complete();
  }
}
