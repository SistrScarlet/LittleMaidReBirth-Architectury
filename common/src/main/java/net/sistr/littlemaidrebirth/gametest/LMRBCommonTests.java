package net.sistr.littlemaidrebirth.gametest;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
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

  // ===== P2: FakePlayer ワールド登録検証 =====

  private static ServerPlayerEntity createWorldPlayer(TestContext context, String name) {
    var player = createPlayer(context, name);
    GameTestHelper.registerPlayerInWorld(context.getWorld(), player);
    return player;
  }

  public static void fakePlayerInWorldPlayers(TestContext context) {
    var player = createWorldPlayer(context, "test-world-player");

    boolean found =
        context.getWorld().getPlayers().stream()
            .anyMatch(p -> p.getUuid().equals(player.getUuid()));
    context.assertTrue(found, "FakePlayerがworld.getPlayers()に含まれること");
    context.complete();
  }

  public static void getTameOwnerReturnsPlayer(TestContext context) {
    var player = createWorldPlayer(context, "test-owner");
    var maid = spawnTamedMaid(context, player);

    var owner = TameableUtil.getTameOwner(maid);
    context.assertTrue(owner.isPresent(), "getTameOwnerがオーナーを返すこと");
    context.assertTrue(owner.get().getUuid().equals(player.getUuid()), "返されたオーナーのUUIDが一致すること");
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

    maid.damage(maid.getDamageSources().outOfWorld(), Float.MAX_VALUE);

    context.addInstantFinalTask(
        () -> {
          var souls =
              context.getWorld().getEntitiesByType(Registration.MAID_SOUL_ENTITY.get(), e -> true);
          context.assertTrue(!souls.isEmpty(), "MaidSoulEntityが生成されていること");
        });
  }

  public static void wildMaidDeathDoesNotCreateSoul(TestContext context) {
    var maid = spawnMaid(context);

    maid.damage(maid.getDamageSources().outOfWorld(), Float.MAX_VALUE);

    context.waitAndRun(
        5,
        () -> {
          var souls =
              context.getWorld().getEntitiesByType(Registration.MAID_SOUL_ENTITY.get(), e -> true);
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
}
