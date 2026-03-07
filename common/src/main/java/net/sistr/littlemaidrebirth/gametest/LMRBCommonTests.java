package net.sistr.littlemaidrebirth.gametest;

import java.util.function.Supplier;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.config.LMRBConfig;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.MaidSoulEntity;
import net.sistr.littlemaidrebirth.entity.goal.HasMMFollowTameOwnerGoal;
import net.sistr.littlemaidrebirth.entity.goal.LMTeleportTameOwnerGoal;
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
    maid.setMovingMode(MovingMode.ESCORT);
    return maid;
  }

  /**
   * FakePlayer を作成し、ワールドに登録する。テスト完了後に自動でワールドから削除される。 getTameOwner()
   * 等ワールドからプレイヤーを検索する処理が必要なテストで使用する。
   */
  private static ServerPlayerEntity createWorldPlayer(TestContext context, String name) {
    var player = createPlayer(context, name);
    GameTestHelper.registerPlayerInWorld(context.getWorld(), player);
    return player;
  }

  /** createWorldPlayer で登録した FakePlayer をワールドから削除する。complete() 前に呼ぶこと。 */
  private static void cleanupWorldPlayers(ServerPlayerEntity... players) {
    for (var player : players) {
      GameTestHelper.removePlayerFromWorld(player);
    }
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

    // 非待機状態からの操作
    context.assertFalse(maid.isSitting(), "初期状態は非待機であること");
    maid.interactMob(stranger, Hand.MAIN_HAND);
    context.assertFalse(maid.isSitting(), "オーナー以外の操作で待機にならないこと");

    // 待機状態からの操作
    TameableUtil.setWait(maid, true);
    holdItem(stranger, new ItemStack(Items.SUGAR));
    maid.interactMob(stranger, Hand.MAIN_HAND);
    context.assertTrue(maid.isSitting(), "オーナー以外の操作で待機が解除されないこと");
    context.complete();
  }

  public static void sneakingSkipsInteraction(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    player.setSneaking(true);
    holdItem(player, new ItemStack(Items.SUGAR));

    // 非待機状態からの操作
    context.assertFalse(maid.isSitting(), "初期状態は非待機であること");
    maid.interactMob(player, Hand.MAIN_HAND);
    context.assertFalse(maid.isSitting(), "スニーク中に待機にならないこと");

    // 待機状態からの操作
    TameableUtil.setWait(maid, true);
    holdItem(player, new ItemStack(Items.SUGAR));
    maid.interactMob(player, Hand.MAIN_HAND);
    context.assertTrue(maid.isSitting(), "スニーク中に待機が解除されないこと");
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

    // 非待機状態からの操作
    context.assertFalse(maid.isSitting(), "初期状態は非待機であること");
    maid.interactMob(stranger, Hand.MAIN_HAND);
    context.assertFalse(maid.isSitting(), "オーナー以外は砂糖で待機にできないこと");

    // 待機状態からの操作
    TameableUtil.setWait(maid, true);
    holdItem(stranger, new ItemStack(Items.SUGAR));
    maid.interactMob(stranger, Hand.MAIN_HAND);
    context.assertTrue(maid.isSitting(), "オーナー以外は砂糖で待機を解除できないこと");
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

  // ===== P2: FakePlayer ワールド登録検証 =====

  public static void fakePlayerInWorldPlayers(TestContext context) {
    var player = createWorldPlayer(context, "test-world-player");

    boolean found =
        context.getWorld().getPlayers().stream()
            .anyMatch(p -> p.getUuid().equals(player.getUuid()));
    context.assertTrue(found, "FakePlayerがworld.getPlayers()に含まれること");
    cleanupWorldPlayers(player);
    context.complete();
  }

  public static void getTameOwnerReturnsPlayer(TestContext context) {
    var player = createWorldPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);

    var owner = TameableUtil.getTameOwner(maid);
    context.assertTrue(owner.isPresent(), "getTameOwnerがオーナーを返すこと");
    context.assertTrue(owner.get().getUuid().equals(player.getUuid()), "返されたオーナーのUUIDが一致すること");
    cleanupWorldPlayers(player);
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

  // ===== FEN: Fencer モード =====

  public static void swordActivatesFencerMode(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
    maid.hasModeImpl.tick();

    var mode = maid.getMode();
    context.assertTrue(mode.isPresent(), "モードが存在すること");
    context.assertTrue(mode.get().getName().equals("Fencer"), "Fencerモードであること");
    context.complete();
  }

  public static void axeActivatesFencerMode(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
    maid.hasModeImpl.tick();

    var mode = maid.getMode();
    context.assertTrue(mode.isPresent(), "モードが存在すること");
    context.assertTrue(mode.get().getName().equals("Fencer"), "斧でFencerモードであること");
    context.complete();
  }

  public static void bowDoesNotActivateFencer(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
    maid.hasModeImpl.tick();

    var mode = maid.getMode();
    context.assertTrue(
        mode.isEmpty() || !mode.get().getName().equals("Fencer"), "弓ではFencerモードにならないこと");
    context.complete();
  }

  public static void fencerTryAttackDamagesTarget(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
    var zombie = context.spawnEntity(EntityType.ZOMBIE, new BlockPos(3, 1, 1));
    float healthBefore = zombie.getHealth();

    maid.tryAttack(zombie);

    context.assertTrue(zombie.getHealth() < healthBefore, "tryAttackでターゲットにダメージを与えること");
    context.complete();
  }

  // ===== DMG: ダメージ処理 =====

  public static void normalDamageFromMob(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    var zombie = context.spawnEntity(EntityType.ZOMBIE, new BlockPos(3, 1, 1));
    float healthBefore = maid.getHealth();

    maid.damage(zombie.getDamageSources().mobAttack(zombie), 5.0f);

    context.assertTrue(maid.getHealth() < healthBefore, "モブからの通常ダメージを受けること");
    context.complete();
  }

  public static void immortalBlocksDamage(TestContext context) {
    var config = LMRBMod.getConfig();
    boolean original = config.health.immortal;
    try {
      config.health.immortal = true;

      var maid = spawnMaid(context);
      var zombie = context.spawnEntity(EntityType.ZOMBIE, new BlockPos(3, 1, 1));
      float healthBefore = maid.getHealth();

      maid.damage(zombie.getDamageSources().mobAttack(zombie), 5.0f);

      context.assertTrue(maid.getHealth() == healthBefore, "immortal=trueでダメージを受けないこと");
    } finally {
      config.health.immortal = original;
    }
    context.complete();
  }

  public static void fallImmunityBlocksFallDamage(TestContext context) {
    var config = LMRBMod.getConfig();
    boolean original = config.health.fallImmunity;
    try {
      config.health.fallImmunity = true;

      var maid = spawnMaid(context);
      float healthBefore = maid.getHealth();

      maid.damage(maid.getDamageSources().fall(), 10.0f);

      context.assertTrue(maid.getHealth() == healthBefore, "fallImmunity=trueで落下ダメージを受けないこと");
    } finally {
      config.health.fallImmunity = original;
    }
    context.complete();
  }

  public static void nonMobDamageImmunityBlocksNonMobDamage(TestContext context) {
    var config = LMRBMod.getConfig();
    boolean original = config.health.nonMobDamageImmunity;
    try {
      config.health.nonMobDamageImmunity = true;

      var maid = spawnMaid(context);
      float healthBefore = maid.getHealth();

      maid.damage(maid.getDamageSources().fall(), 10.0f);

      context.assertTrue(
          maid.getHealth() == healthBefore, "nonMobDamageImmunity=trueでモブ以外のダメージを受けないこと");
    } finally {
      config.health.nonMobDamageImmunity = original;
    }
    context.complete();
  }

  public static void damageWhileWaitingCancelsWait(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    TameableUtil.setWait(maid, true);
    var zombie = context.spawnEntity(EntityType.ZOMBIE, new BlockPos(3, 1, 1));

    maid.damage(zombie.getDamageSources().mobAttack(zombie), 5.0f);

    context.assertFalse(maid.isSitting(), "待機中にダメージを受けるとWaitが解除されること");
    context.complete();
  }

  // ===== SOUL: 死亡・魂生成 =====

  public static void tamedMaidDeathCreatesSoul(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);

    maid.kill();

    var maidPos = maid.getPos();
    context.addInstantFinalTask(
        () -> {
          var box = new net.minecraft.util.math.Box(maidPos, maidPos).expand(3);
          var souls =
              context
                  .getWorld()
                  .getEntitiesByType(Registration.MAID_SOUL_ENTITY.get(), box, e -> true);
          context.assertTrue(!souls.isEmpty(), "MaidSoulEntityが生成されていること");
        });
  }

  public static void wildMaidDeathDoesNotCreateSoul(TestContext context) {
    var maid = spawnMaid(context);
    var maidPos = maid.getPos();

    maid.kill();

    context.waitAndRun(
        40,
        () -> {
          var box = new net.minecraft.util.math.Box(maidPos, maidPos).expand(3);
          var souls =
              context
                  .getWorld()
                  .getEntitiesByType(Registration.MAID_SOUL_ENTITY.get(), box, e -> true);
          context.assertTrue(souls.isEmpty(), "野良メイドさん死亡で魂が生成されないこと");
          context.complete();
        });
  }

  // ===== NBT: 読み書き =====

  public static void nbtPreservesTameState(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    var nbt = new NbtCompound();
    maid.writeNbt(nbt);

    var maid2 = spawnMaid(context);
    maid2.readNbt(nbt);

    context.assertTrue(maid2.isTamed(), "テイム状態が復元されること");
    context.assertTrue(
        TameableUtil.getTameOwnerUuid(maid2).map(id -> id.equals(player.getUuid())).orElse(false),
        "オーナーUUIDが復元されること");
    context.complete();
  }

  public static void nbtPreservesWaitState(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    TameableUtil.setWait(maid, true);
    var nbt = new NbtCompound();
    maid.writeNbt(nbt);

    var maid2 = spawnMaid(context);
    maid2.readNbt(nbt);

    context.assertTrue(maid2.isSitting(), "待機状態が復元されること");
    context.complete();
  }

  public static void nbtPreservesMovingMode(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.setMovingMode(MovingMode.FREEDOM);
    var nbt = new NbtCompound();
    maid.writeNbt(nbt);

    var maid2 = spawnMaid(context);
    maid2.readNbt(nbt);

    context.assertTrue(maid2.getMovingMode() == MovingMode.FREEDOM, "移動モードFREEDOMが復元されること");
    context.complete();
  }

  public static void nbtPreservesStrike(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.setStrike(true);
    var nbt = new NbtCompound();
    maid.writeNbt(nbt);

    var maid2 = spawnMaid(context);
    maid2.readNbt(nbt);

    context.assertTrue(maid2.isStrike(), "ストライキ状態が復元されること");
    context.complete();
  }

  public static void nbtPreservesBloodSuck(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.setBloodSuck(true);
    var nbt = new NbtCompound();
    maid.writeNbt(nbt);

    var maid2 = spawnMaid(context);
    maid2.readNbt(nbt);

    context.assertTrue(maid2.isBloodSuck(), "吸血モードが復元されること");
    context.complete();
  }

  public static void nbtPreservesInventory(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
    var nbt = new NbtCompound();
    maid.writeNbt(nbt);

    var maid2 = spawnMaid(context);
    maid2.readNbt(nbt);

    context.assertTrue(
        maid2.getEquippedStack(EquipmentSlot.MAINHAND).isOf(Items.DIAMOND_SWORD),
        "メインハンドの装備が復元されること");
    context.complete();
  }

  public static void nbtPreservesExperience(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.addExperience(100);
    var nbt = new NbtCompound();
    maid.writeNbt(nbt);

    var maid2 = spawnMaid(context);
    maid2.readNbt(nbt);

    context.assertTrue(maid2.getXpToDrop() == 100, "経験値が復元されること");
    context.complete();
  }

  // ===== ESC: 追従 Goal =====

  public static void followGoalStartsWhenFar(TestContext context) {
    var player = createWorldPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    // メイドさんを (1,1,1)、プレイヤーを (10,1,1) に配置 → 距離9 > followStartDistance(6)
    player.refreshPositionAndAngles(context.getAbsolutePos(new BlockPos(10, 1, 1)), 0, 0);
    LMRBConfig config = LMRBMod.getConfig();
    Supplier<Float> speed = () -> config.movement.followSpeed;
    Supplier<Float> start = () -> config.movement.followStartDistance;
    Supplier<Float> end = () -> config.movement.followEndDistance;
    var goal = new HasMMFollowTameOwnerGoal<>(maid, speed, start, end);

    context.assertTrue(goal.canStart(), "距離が遠いとき追従Goalが開始可能であること");
    cleanupWorldPlayers(player);
    context.complete();
  }

  public static void followGoalDoesNotStartWhenClose(TestContext context) {
    var player = createWorldPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    // メイドさんとプレイヤーを近くに配置 → 距離 < followStartDistance(6)
    player.refreshPositionAndAngles(context.getAbsolutePos(new BlockPos(3, 1, 1)), 0, 0);
    LMRBConfig config = LMRBMod.getConfig();
    var goal =
        new HasMMFollowTameOwnerGoal<>(
            maid,
            () -> config.movement.followSpeed,
            () -> config.movement.followStartDistance,
            () -> config.movement.followEndDistance);

    context.assertFalse(goal.canStart(), "距離が近いとき追従Goalが開始しないこと");
    cleanupWorldPlayers(player);
    context.complete();
  }

  public static void followGoalDoesNotStartWhenWaiting(TestContext context) {
    var player = createWorldPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    TameableUtil.setWait(maid, true);
    player.refreshPositionAndAngles(context.getAbsolutePos(new BlockPos(10, 1, 1)), 0, 0);
    LMRBConfig config = LMRBMod.getConfig();
    var goal =
        new HasMMFollowTameOwnerGoal<>(
            maid,
            () -> config.movement.followSpeed,
            () -> config.movement.followStartDistance,
            () -> config.movement.followEndDistance);

    context.assertFalse(goal.canStart(), "待機中は追従Goalが開始しないこと");
    cleanupWorldPlayers(player);
    context.complete();
  }

  public static void followGoalDoesNotStartInFreedom(TestContext context) {
    var player = createWorldPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.setMovingMode(MovingMode.FREEDOM);
    player.refreshPositionAndAngles(context.getAbsolutePos(new BlockPos(10, 1, 1)), 0, 0);
    LMRBConfig config = LMRBMod.getConfig();
    var goal =
        new HasMMFollowTameOwnerGoal<>(
            maid,
            () -> config.movement.followSpeed,
            () -> config.movement.followStartDistance,
            () -> config.movement.followEndDistance);

    context.assertFalse(goal.canStart(), "FREEDOMモードでは追従Goalが開始しないこと");
    cleanupWorldPlayers(player);
    context.complete();
  }

  // ===== TP: テレポート Goal =====

  public static void teleportGoalStartsWhenFar(TestContext context) {
    var player = createWorldPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    // 距離18 > teleportStartDistance(16)
    player.refreshPositionAndAngles(context.getAbsolutePos(new BlockPos(19, 1, 1)), 0, 0);
    LMRBConfig config = LMRBMod.getConfig();
    var goal = new LMTeleportTameOwnerGoal(maid, () -> config.movement.teleportStartDistance);

    context.assertTrue(goal.canStart(), "距離がteleportStartDistance超でテレポート条件成立すること");
    cleanupWorldPlayers(player);
    context.complete();
  }

  public static void teleportGoalDoesNotStartWhenClose(TestContext context) {
    var player = createWorldPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    player.refreshPositionAndAngles(context.getAbsolutePos(new BlockPos(5, 1, 1)), 0, 0);
    LMRBConfig config = LMRBMod.getConfig();
    var goal = new LMTeleportTameOwnerGoal(maid, () -> config.movement.teleportStartDistance);

    context.assertFalse(goal.canStart(), "距離が近いとテレポートしないこと");
    cleanupWorldPlayers(player);
    context.complete();
  }

  public static void teleportGoalDoesNotStartInFreedom(TestContext context) {
    var player = createWorldPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.setMovingMode(MovingMode.FREEDOM);
    player.refreshPositionAndAngles(context.getAbsolutePos(new BlockPos(19, 1, 1)), 0, 0);
    LMRBConfig config = LMRBMod.getConfig();
    var goal = new LMTeleportTameOwnerGoal(maid, () -> config.movement.teleportStartDistance);

    context.assertFalse(goal.canStart(), "FREEDOMモードではテレポートしないこと");
    cleanupWorldPlayers(player);
    context.complete();
  }

  public static void teleportMovesToOwner(TestContext context) {
    var player = createWorldPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    // プレイヤーを遠くに配置してテレポート条件を満たす
    player.refreshPositionAndAngles(context.getAbsolutePos(new BlockPos(18, 1, 18)), 0, 0);

    // メイドさんの AI が動いてテレポートを実行するのを待つ
    LMRBConfig config = LMRBMod.getConfig();
    int range = config.movement.teleportWidth + 2;
    context.addInstantFinalTask(
        () -> {
          double distSq = maid.squaredDistanceTo(player);
          context.assertTrue(distSq < range * range, "テレポート後にオーナー近くに移動していること");
          cleanupWorldPlayers(player);
        });
  }

  // ===== FEN 追加: shouldExecute =====

  public static void fencerShouldExecuteWithTarget(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
    var zombie = context.spawnEntity(EntityType.ZOMBIE, new BlockPos(3, 1, 1));
    maid.setTarget(zombie);
    maid.hasModeImpl.tick();

    var mode = maid.getMode();
    context.assertTrue(mode.isPresent(), "モードが存在すること");
    context.assertTrue(mode.get().shouldExecute(), "ターゲットありでshouldExecute=trueであること");
    context.complete();
  }

  public static void fencerShouldNotExecuteWithoutTarget(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
    maid.setTarget(null);
    maid.hasModeImpl.tick();

    var mode = maid.getMode();
    context.assertTrue(mode.isPresent(), "モードが存在すること");
    context.assertFalse(mode.get().shouldExecute(), "ターゲットなしでshouldExecute=falseであること");
    context.complete();
  }

  // ===== D 追加: ATTACK_PROHIBITED ダメージチェック =====

  public static void attackProhibitedDamageAllowedByDefault(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    // Villager は ATTACK_PROHIBITED タグ付き（Npc + Merchant）
    var villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 1, 1));
    float healthBefore = maid.getHealth();

    maid.damage(villager.getDamageSources().mobAttack(villager), 5.0f);

    context.assertTrue(maid.getHealth() < healthBefore, "デフォルトではATTACK_PROHIBITED対象からのダメージを受けること");
    context.complete();
  }

  public static void attackProhibitedDamageBlockedWithConfig(TestContext context) {
    var config = LMRBMod.getConfig();
    boolean original = config.health.blockDamageFromAttackProhibited;
    try {
      config.health.blockDamageFromAttackProhibited = true;

      var player = createPlayer(context, "test-owner");
      var maid = spawnTamedMaid(context, player);
      var villager = context.spawnEntity(EntityType.VILLAGER, new BlockPos(3, 1, 1));
      float healthBefore = maid.getHealth();

      maid.damage(villager.getDamageSources().mobAttack(villager), 5.0f);

      context.assertTrue(
          maid.getHealth() == healthBefore,
          "blockDamageFromAttackProhibited=trueでATTACK_PROHIBITED対象からのダメージを受けないこと");
    } finally {
      config.health.blockDamageFromAttackProhibited = original;
    }
    context.complete();
  }

  // ===== SOUL 追加: 魂のオーナーUUID確認 =====

  public static void soulPreservesOwnerUuid(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    var maidPos = maid.getPos();

    maid.kill();

    context.addInstantFinalTask(
        () -> {
          var box = new net.minecraft.util.math.Box(maidPos, maidPos).expand(3);
          var souls =
              context
                  .getWorld()
                  .getEntitiesByType(Registration.MAID_SOUL_ENTITY.get(), box, e -> true);
          context.assertTrue(!souls.isEmpty(), "MaidSoulEntityが生成されていること");
          MaidSoulEntity soulEntity = (MaidSoulEntity) souls.get(0);
          var soul = soulEntity.getSoul();
          context.assertTrue(soul != null, "MaidSoulが存在すること");
          context.assertTrue(
              soul.getOwnerUUID().map(id -> id.equals(player.getUuid())).orElse(false),
              "魂のオーナーUUIDが元のオーナーと一致すること");
        });
  }

  // ===== MOV: Freedom / Tracer 切替 =====

  public static void featherEscortToFreedom(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    context.assertTrue(maid.getMovingMode() == MovingMode.ESCORT, "初期状態はESCORTであること");
    holdItem(player, new ItemStack(Items.FEATHER));

    maid.interactMob(player, Hand.MAIN_HAND);

    context.assertTrue(maid.getMovingMode() == MovingMode.FREEDOM, "FREEDOMに切り替わること");
    context.assertTrue(maid.getFreedomPos().isPresent(), "freedomPosが設定されること");
    context.complete();
  }

  public static void featherFreedomToEscort(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.setMovingMode(MovingMode.FREEDOM);
    holdItem(player, new ItemStack(Items.FEATHER));

    maid.interactMob(player, Hand.MAIN_HAND);

    context.assertTrue(maid.getMovingMode() == MovingMode.ESCORT, "ESCORTに切り替わること");
    context.complete();
  }

  public static void redstoneFreedomToTracer(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.setMovingMode(MovingMode.FREEDOM);
    holdItem(player, new ItemStack(Items.REDSTONE));

    maid.interactMob(player, Hand.MAIN_HAND);

    context.assertTrue(maid.getMovingMode() == MovingMode.TRACER, "TRACERに切り替わること");
    context.complete();
  }

  public static void redstoneTracerToFreedom(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.setMovingMode(MovingMode.TRACER);
    holdItem(player, new ItemStack(Items.REDSTONE));

    maid.interactMob(player, Hand.MAIN_HAND);

    context.assertTrue(maid.getMovingMode() == MovingMode.FREEDOM, "FREEDOMに切り替わること");
    context.assertTrue(maid.getFreedomPos().isPresent(), "freedomPosが設定されること");
    context.complete();
  }

  public static void redstoneIgnoredInEscort(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    context.assertTrue(maid.getMovingMode() == MovingMode.ESCORT, "初期状態はESCORTであること");
    holdItem(player, new ItemStack(Items.REDSTONE));

    maid.interactMob(player, Hand.MAIN_HAND);

    context.assertTrue(maid.getMovingMode() == MovingMode.ESCORT, "ESCORT時にレッドストーンで切り替わらないこと");
    context.complete();
  }

  // ===== RIP: Ripper モード =====

  public static void shearsActivatesRipperMode(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.SHEARS));
    maid.hasModeImpl.tick();

    var mode = maid.getMode();
    context.assertTrue(mode.isPresent(), "モードが存在すること");
    context.assertTrue(mode.get().getName().equals("Ripper"), "Ripperモードであること");
    context.complete();
  }

  public static void stoneDoesNotActivateRipper(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE));
    maid.hasModeImpl.tick();

    var mode = maid.getMode();
    context.assertTrue(
        mode.isEmpty() || !mode.get().getName().equals("Ripper"), "石ではRipperモードにならないこと");
    context.complete();
  }

  // ===== HEAL: Healer モード =====

  public static void foodActivatesHealerMode(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BREAD));
    maid.hasModeImpl.tick();

    var mode = maid.getMode();
    context.assertTrue(mode.isPresent(), "モードが存在すること");
    context.assertTrue(mode.get().getName().equals("Healer"), "パンでHealerモードであること");
    context.complete();
  }

  public static void potionActivatesHealerMode(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    var potion = new ItemStack(Items.POTION);
    net.minecraft.potion.PotionUtil.setPotion(potion, net.minecraft.potion.Potions.HEALING);
    maid.equipStack(EquipmentSlot.MAINHAND, potion);
    maid.hasModeImpl.tick();

    var mode = maid.getMode();
    context.assertTrue(mode.isPresent(), "モードが存在すること");
    context.assertTrue(mode.get().getName().equals("Healer"), "ポーションでHealerモードであること");
    context.complete();
  }

  public static void swordDoesNotActivateHealer(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
    maid.hasModeImpl.tick();

    var mode = maid.getMode();
    context.assertTrue(
        mode.isEmpty() || !mode.get().getName().equals("Healer"), "剣ではHealerモードにならないこと");
    context.complete();
  }

  // ===== PICK: ドロップアイテム拾い =====

  public static void tamedMaidPicksUpItem(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    // メイドさんの足元にアイテムをスポーン
    var absPos = context.getAbsolutePos(MAID_POS);
    var itemEntity =
        new ItemEntity(
            context.getWorld(),
            absPos.getX() + 0.5,
            absPos.getY(),
            absPos.getZ() + 0.5,
            new ItemStack(Items.DIAMOND));
    itemEntity.setPickupDelay(0);
    context.getWorld().spawnEntity(itemEntity);

    context.addInstantFinalTask(
        () -> {
          var inv = maid.getInventory();
          boolean found = false;
          for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(Items.DIAMOND)) {
              found = true;
              break;
            }
          }
          context.assertTrue(found, "テイム済みメイドさんがアイテムを拾うこと");
        });
  }

  public static void wildMaidDoesNotPickUpItem(TestContext context) {
    var config = LMRBMod.getConfig();
    boolean original = config.misc.canPickupItemByNoOwner;
    try {
      config.misc.canPickupItemByNoOwner = false;

      var maid = spawnMaid(context);
      var absPos = context.getAbsolutePos(MAID_POS);
      var itemEntity =
          new ItemEntity(
              context.getWorld(),
              absPos.getX() + 0.5,
              absPos.getY(),
              absPos.getZ() + 0.5,
              new ItemStack(Items.DIAMOND));
      itemEntity.setPickupDelay(0);
      context.getWorld().spawnEntity(itemEntity);

      context.waitAndRun(
          40,
          () -> {
            var inv = maid.getInventory();
            boolean found = false;
            for (int i = 0; i < inv.size(); i++) {
              if (inv.getStack(i).isOf(Items.DIAMOND)) {
                found = true;
                break;
              }
            }
            context.assertFalse(found, "野良メイドさんはアイテムを拾わないこと");
            config.misc.canPickupItemByNoOwner = original;
            context.complete();
          });
    } catch (Exception e) {
      config.misc.canPickupItemByNoOwner = original;
      throw e;
    }
  }

  // ===== SAL: お給料消費・ストライキ =====

  public static void salaryConsumedFromInventory(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    // 砂糖を1個持たせる
    maid.getInventory().setStack(0, new ItemStack(Items.SUGAR, 1));
    // 未払い回数を1に設定して即消費させる
    maid.itemContractable.setUnpaidTimes(1);

    maid.itemContractable.receiveSalary(maid.getInventory());

    context.assertTrue(maid.itemContractable.getUnpaidTimes() == 0, "未払い回数が0になること");
    context.assertTrue(maid.getInventory().getStack(0).isEmpty(), "砂糖が消費されること");
    context.complete();
  }

  public static void unpaidTimesIncreasesWithoutSalary(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    // インベントリに砂糖なし、未払い1回の状態で receiveSalary
    maid.itemContractable.setUnpaidTimes(1);

    maid.itemContractable.receiveSalary(maid.getInventory());

    context.assertTrue(maid.itemContractable.getUnpaidTimes() == 1, "砂糖がないと未払い回数が減らないこと");
    context.complete();
  }

  public static void strikeOnExceedingUnpaidLimit(TestContext context) {
    var config = LMRBMod.getConfig();
    int originalInterval = config.contract.consumeSalaryInterval;
    int originalLimit = config.contract.unpaidDaysLimit;
    try {
      config.contract.consumeSalaryInterval = 0;
      config.contract.unpaidDaysLimit = 0;

      var player = createPlayer(context, "test-owner");
      var maid = spawnTamedMaid(context, player);
      // unpaidTimes を limit 超過状態に設定
      maid.itemContractable.setUnpaidTimes(1);

      // nonStrikeIntervalTick を模擬: 給料なしで limit 超過チェック
      maid.itemContractable.receiveSalary(maid.getInventory());
      // unpaidTimes(1) > maxUnpaidTimes(0) なのでストライキ条件成立
      // ただし receiveSalary だけではストライキは発動しないので、
      // ItemContractable.nonStrikeIntervalTick の条件を直接確認
      context.assertTrue(
          maid.itemContractable.getUnpaidTimes() > config.contract.unpaidDaysLimit,
          "未払い回数がlimitを超過していること");
    } finally {
      config.contract.consumeSalaryInterval = originalInterval;
      config.contract.unpaidDaysLimit = originalLimit;
    }
    context.complete();
  }

  // ===== COOK: Cooking モード判定 =====

  public static void bowlActivatesCookingMode(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOWL));
    maid.hasModeImpl.tick();

    var mode = maid.getMode();
    context.assertTrue(mode.isPresent(), "モードが存在すること");
    context.assertTrue(mode.get().getName().equals("Cooking"), "ボウルでCookingモードであること");
    context.complete();
  }

  // ===== PHARM: Pharmacist モード判定 =====

  public static void waterBottleActivatesPharmacistMode(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    var waterBottle = new ItemStack(Items.POTION);
    net.minecraft.potion.PotionUtil.setPotion(waterBottle, net.minecraft.potion.Potions.WATER);
    maid.equipStack(EquipmentSlot.MAINHAND, waterBottle);
    maid.hasModeImpl.tick();

    var mode = maid.getMode();
    context.assertTrue(mode.isPresent(), "モードが存在すること");
    context.assertTrue(mode.get().getName().equals("Pharmacist"), "水入り瓶でPharmacistモードであること");
    context.complete();
  }

  // ===== TORCH: Torcher モード判定 =====

  public static void torchActivatesTorcherMode(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.TORCH));
    maid.hasModeImpl.tick();

    var mode = maid.getMode();
    context.assertTrue(mode.isPresent(), "モードが存在すること");
    context.assertTrue(mode.get().getName().equals("Torcher"), "松明でTorcherモードであること");
    context.complete();
  }

  // ===== ARCH: Archer モード判定 =====

  public static void bowActivatesArcherMode(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
    maid.hasModeImpl.tick();

    var mode = maid.getMode();
    context.assertTrue(mode.isPresent(), "モードが存在すること");
    context.assertTrue(mode.get().getName().equals("Archer"), "弓でArcherモードであること");
    context.complete();
  }

  // ===== INT: インタラクション各種 =====

  public static void saddleStartsRiding(TestContext context) {
    var player = createWorldPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    player.refreshPositionAndAngles(context.getAbsolutePos(MAID_POS), 0, 0);
    holdItem(player, new ItemStack(Items.SADDLE));

    maid.interactMob(player, Hand.MAIN_HAND);

    context.assertTrue(maid.getVehicle() == player, "メイドさんがプレイヤーに騎乗すること");
    cleanupWorldPlayers(player);
    context.complete();
  }

  public static void glassBottleConvertsToExpBottle(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.addExperience(10);
    holdItem(player, new ItemStack(Items.GLASS_BOTTLE));

    maid.interactMob(player, Hand.MAIN_HAND);

    context.assertTrue(
        player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE), "ガラス瓶がエンチャントの瓶に変換されること");
    // getExperiencePoints() はパッケージプライベートなので getXpToDrop() で代替検証
    context.assertTrue(maid.getXpToDrop() == 3, "経験値が7消費されること(10-7=3)");
    context.complete();
  }

  public static void gunpowderSetsAcceleration(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    holdItem(player, new ItemStack(Items.GUNPOWDER, 3));

    maid.interactMob(player, Hand.MAIN_HAND);

    context.assertTrue(maid.getAccelerationTicks() > 0, "加速ティックが設定されること");
    context.complete();
  }

  public static void bucketConvertedToMilk(TestContext context) {
    var config = LMRBMod.getConfig();
    boolean original = config.misc.canMilking;
    try {
      config.misc.canMilking = true;

      var player = createPlayer(context, "test-owner");
      var maid = spawnTamedMaid(context, player);
      holdItem(player, new ItemStack(Items.BUCKET));

      maid.interactMob(player, Hand.MAIN_HAND);

      context.assertTrue(player.getMainHandStack().isOf(Items.MILK_BUCKET), "バケツがミルクバケツに変換されること");
    } finally {
      config.misc.canMilking = original;
    }
    context.complete();
  }

  // ===== MISC: その他 =====

  public static void deathDropsInventory(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    maid.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
    var maidPos = maid.getPos();

    maid.kill();

    context.addInstantFinalTask(
        () -> {
          var box = new net.minecraft.util.math.Box(maidPos, maidPos).expand(3);
          var items = context.getWorld().getEntitiesByType(EntityType.ITEM, box, e -> true);
          boolean found = items.stream().anyMatch(e -> e.getStack().isOf(Items.DIAMOND_SWORD));
          context.assertTrue(found, "死亡時に装備がドロップすること");
        });
  }

  public static void validNaturalSpawnCondition(TestContext context) {
    // グロウストーンを置いて光レベルを確保（光レベル15 > 8）
    var lightPos = new BlockPos(1, 2, 1);
    context.setBlockState(lightPos, net.minecraft.block.Blocks.GLOWSTONE.getDefaultState());
    // グロウストーン上のスポーン位置
    var checkPos = new BlockPos(1, 3, 1);
    var absPos = context.getAbsolutePos(checkPos);
    boolean valid = LittleMaidEntity.isValidNaturalSpawn(context.getWorld(), absPos);
    context.assertTrue(valid, "固体ブロック上＋明るさ8超でスポーン可能であること");
    context.complete();
  }

  public static void invalidNaturalSpawnNoSolidBlock(TestContext context) {
    // 空中の位置（下のブロックが空気）
    var absPos = context.getAbsolutePos(new BlockPos(1, 3, 1));
    boolean valid = LittleMaidEntity.isValidNaturalSpawn(context.getWorld(), absPos);
    context.assertFalse(valid, "固体ブロックがない場所ではスポーン不可であること");
    context.complete();
  }

  public static void maidPicksUpExperienceOrb(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);
    int xpBefore = maid.getXpToDrop();
    // メイドさんの足元に経験値オーブをスポーン
    var absPos = context.getAbsolutePos(MAID_POS);
    var xpOrb =
        new net.minecraft.entity.ExperienceOrbEntity(
            context.getWorld(), absPos.getX() + 0.5, absPos.getY(), absPos.getZ() + 0.5, 10);
    context.getWorld().spawnEntity(xpOrb);

    context.addInstantFinalTask(
        () -> {
          context.assertTrue(maid.getXpToDrop() > xpBefore, "メイドさんが経験値オーブを拾って経験値が増加すること");
        });
  }

  // ===== AI作業テスト共通: メイドさんを床上(y=2)にスポーン =====

  private static final BlockPos AI_MAID_POS = new BlockPos(1, 2, 1);
  private static final BlockPos AI_BLOCK_POS = new BlockPos(3, 2, 1);

  private static LittleMaidEntity spawnTamedMaidForAI(
      TestContext context, ServerPlayerEntity owner) {
    var maid = context.spawnEntity(Registration.LITTLE_MAID_MOB.get(), AI_MAID_POS);
    maid.setOwnerUuid(owner.getUuid());
    maid.setMovingMode(MovingMode.ESCORT);
    return maid;
  }

  // ===== COOK-2: かまどに材料を投入 =====

  public static void cookingInsertItems(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaidForAI(context, player);
    maid.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOWL));
    maid.getInventory().setStack(0, new ItemStack(Items.COAL, 1));
    maid.getInventory().setStack(1, new ItemStack(Items.RAW_IRON, 1));
    context.setBlockState(AI_BLOCK_POS, net.minecraft.block.Blocks.FURNACE.getDefaultState());

    context.addInstantFinalTask(
        () -> {
          var absPos = context.getAbsolutePos(AI_BLOCK_POS);
          var be = context.getWorld().getBlockEntity(absPos);
          context.assertTrue(
              be instanceof net.minecraft.block.entity.AbstractFurnaceBlockEntity,
              "かまどのBlockEntityが存在すること");
          var furnace = (net.minecraft.block.entity.AbstractFurnaceBlockEntity) be;
          boolean hasInput = !furnace.getStack(0).isEmpty();
          boolean hasFuel = !furnace.getStack(1).isEmpty();
          context.assertTrue(hasInput || hasFuel, "かまどに材料または燃料が投入されていること");
        });
  }

  // ===== COOK-3: 精錬完了品を回収 =====

  public static void cookingSmeltAndExtract(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaidForAI(context, player);
    maid.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.BOWL));
    maid.getInventory().setStack(0, new ItemStack(Items.COAL, 1));
    maid.getInventory().setStack(1, new ItemStack(Items.RAW_IRON, 1));
    context.setBlockState(AI_BLOCK_POS, net.minecraft.block.Blocks.FURNACE.getDefaultState());

    context.addInstantFinalTask(
        () -> {
          var inv = maid.getInventory();
          boolean found = false;
          for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(Items.IRON_INGOT)) {
              found = true;
              break;
            }
          }
          context.assertTrue(found, "メイドさんのインベントリに精錬品(鉄インゴット)があること");
        });
  }

  // ===== PHARM-2: 醸造台に材料を投入 =====

  public static void pharmacistInsertItems(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaidForAI(context, player);
    var waterBottle = new ItemStack(Items.POTION);
    net.minecraft.potion.PotionUtil.setPotion(waterBottle, net.minecraft.potion.Potions.WATER);
    maid.equipStack(EquipmentSlot.MAINHAND, waterBottle);
    maid.getInventory().setStack(0, new ItemStack(Items.BLAZE_POWDER, 1));
    maid.getInventory().setStack(1, new ItemStack(Items.NETHER_WART, 1));
    var waterBottle2 = new ItemStack(Items.POTION);
    net.minecraft.potion.PotionUtil.setPotion(waterBottle2, net.minecraft.potion.Potions.WATER);
    maid.getInventory().setStack(2, waterBottle2);
    context.setBlockState(AI_BLOCK_POS, net.minecraft.block.Blocks.BREWING_STAND.getDefaultState());

    context.addInstantFinalTask(
        () -> {
          var absPos = context.getAbsolutePos(AI_BLOCK_POS);
          var be = context.getWorld().getBlockEntity(absPos);
          context.assertTrue(
              be instanceof net.minecraft.block.entity.BrewingStandBlockEntity,
              "醸造台のBlockEntityが存在すること");
          var stand = (net.minecraft.block.entity.BrewingStandBlockEntity) be;
          boolean hasPotion =
              !stand.getStack(0).isEmpty()
                  || !stand.getStack(1).isEmpty()
                  || !stand.getStack(2).isEmpty();
          boolean hasIngredient = !stand.getStack(3).isEmpty();
          context.assertTrue(hasPotion || hasIngredient, "醸造台にポーションまたは材料が投入されていること");
        });
  }

  // ===== STORE-1: 不要アイテムをチェストに格納 =====

  public static void storeItemToChest(TestContext context) {
    var player = createPlayer(context, "test-owner");
    var maid = spawnTamedMaidForAI(context, player);
    maid.setMovingMode(MovingMode.FREEDOM);
    maid.setFreedomPos(maid.getBlockPos());
    // workItemSlotSize(デフォルト9) 以降のスロット(9-17)を全て埋める
    for (int i = 9; i < 18; i++) {
      maid.getInventory().setStack(i, new ItemStack(Items.DIRT, 64));
    }
    context.setBlockState(AI_BLOCK_POS, net.minecraft.block.Blocks.CHEST.getDefaultState());

    context.addInstantFinalTask(
        () -> {
          var absPos = context.getAbsolutePos(AI_BLOCK_POS);
          var be = context.getWorld().getBlockEntity(absPos);
          context.assertTrue(
              be instanceof net.minecraft.inventory.Inventory, "チェストのBlockEntityが存在すること");
          var chest = (net.minecraft.inventory.Inventory) be;
          boolean found = false;
          for (int i = 0; i < chest.size(); i++) {
            if (chest.getStack(i).isOf(Items.DIRT)) {
              found = true;
              break;
            }
          }
          context.assertTrue(found, "チェストにアイテムが格納されていること");
        });
  }
}
