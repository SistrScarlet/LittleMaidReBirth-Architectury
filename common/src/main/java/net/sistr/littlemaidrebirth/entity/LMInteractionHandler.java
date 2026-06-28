package net.sistr.littlemaidrebirth.entity;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.item.SaddleItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.sistr.littlemaidmodelloader.network.SyncMultiModelPacket;
import net.sistr.littlemaidrebirth.config.LMRBConfig;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;
import net.sistr.littlemaidrebirth.tags.LMTags;

final class LMInteractionHandler {

  private LMInteractionHandler() {}

  static ActionResult handle(LittleMaidEntity maid, PlayerEntity player, Hand hand) {
    // メイドさんへの操作はメインハンドのみ受け付ける。
    // オフハンド(多くの場合空手)まで処理すると、末尾の openInventory に落ちて
    // 意図せずインベントリが開く(特にクライアントで owner 未同期だと両手分の
    // インタラクションが送られ、サーバーのオフハンド処理がインベントリを開く)。
    if (hand != Hand.MAIN_HAND) {
      return ActionResult.PASS;
    }
    if (player.isSneaking()) {
      return ActionResult.PASS;
    }
    ItemStack stack = player.getStackInHand(hand);
    // オーナーが居ない場合
    if (TameableUtil.getTameOwnerUuid(maid).isEmpty()) {
      if (stack.isIn(LMTags.Items.MAIDS_EMPLOYABLE)) {
        return contract(maid, player, stack, false);
      }
      return ActionResult.PASS;
    }
    // オーナーじゃない場合
    if (!player.getUuid().equals(maid.getOwnerUuid())) {
      return ActionResult.PASS;
    }
    // ストライキ時
    if (maid.isStrike()) {
      if (stack.isIn(LMTags.Items.MAIDS_EMPLOYABLE)) {
        return contract(maid, player, stack, true);
      }
      maid.getWorld().sendEntityStatus(maid, (byte) 6);
      return ActionResult.PASS;
    }
    // サドル持ってるとき
    if (stack.getItem() instanceof SaddleItem) {
      return handleSaddle(maid, player);
    }
    // 肩車されてるとき
    if (maid.getVehicle() == player) {
      return ActionResult.PASS;
    }
    // 砂糖
    if (stack.isIn(LMTags.Items.MAIDS_SALARY)) {
      LMRBConfig config = LittleMaidEntity.getConfig();
      maid.heal(config.health.healAmount);
      return changeState(maid, player, stack);
    }
    // Freedom切替
    if (stack.getItem() == Items.FEATHER) {
      return handleFeather(maid);
    }
    // Tracer切替
    if ((maid.getMovingMode() == MovingMode.FREEDOM || maid.getMovingMode() == MovingMode.TRACER)
        && stack.getItem() == Items.REDSTONE) {
      return handleRedstone(maid);
    }
    // ガラス瓶->エンチャントの瓶
    if (maid.getExperiencePoints() >= LittleMaidEntity.getConfig().misc.experienceBottleCost
        && stack.isOf(Items.GLASS_BOTTLE)) {
      return handleGlassBottle(maid, player, hand, stack);
    }
    // モブミルク
    if (LittleMaidEntity.getConfig().misc.canMilking && stack.isOf(Items.BUCKET)) {
      return handleBucket(maid, player, hand, stack);
    }
    // 火薬->加速
    if (stack.getItem() == Items.GUNPOWDER) {
      return handleGunpowder(maid, player, stack);
    }
    maid.openInventory(player);
    return ActionResult.success(maid.getWorld().isClient);
  }

  private static ActionResult handleSaddle(LittleMaidEntity maid, PlayerEntity player) {
    if (!maid.hasVehicle()) {
      if (player.hasPassengers()) {
        player.removeAllPassengers();
      }
      maid.startRiding(player);
    } else {
      var vehicle = maid.getVehicle();
      if (vehicle == player) {
        maid.stopRiding();
      }
    }
    return ActionResult.success(maid.getWorld().isClient);
  }

  private static ActionResult handleFeather(LittleMaidEntity maid) {
    if (maid.getMovingMode() == MovingMode.ESCORT) {
      maid.getWorld().sendEntityStatus(maid, (byte) 73);
      maid.setMovingMode(MovingMode.FREEDOM);
      maid.setFreedomPos(maid.getBlockPos());
    } else {
      maid.getWorld().sendEntityStatus(maid, (byte) 74);
      maid.setMovingMode(MovingMode.ESCORT);
    }
    return ActionResult.success(maid.getWorld().isClient);
  }

  private static ActionResult handleRedstone(LittleMaidEntity maid) {
    if (maid.getMovingMode() == MovingMode.FREEDOM) {
      maid.getWorld().sendEntityStatus(maid, (byte) 75);
      maid.setMovingMode(MovingMode.TRACER);
    } else {
      maid.getWorld().sendEntityStatus(maid, (byte) 73);
      maid.setMovingMode(MovingMode.FREEDOM);
      maid.setFreedomPos(maid.getBlockPos());
    }
    return ActionResult.success(maid.getWorld().isClient);
  }

  private static ActionResult handleGlassBottle(
      LittleMaidEntity maid, PlayerEntity player, Hand hand, ItemStack stack) {
    maid.getWorld()
        .playSound(
            null,
            maid.getX(),
            maid.getY(),
            maid.getZ(),
            SoundEvents.ITEM_BOTTLE_FILL,
            SoundCategory.PLAYERS,
            1.0f,
            1.0f);
    ItemStack itemStack2 =
        ItemUsage.exchangeStack(stack, player, Items.EXPERIENCE_BOTTLE.getDefaultStack());
    player.setStackInHand(hand, itemStack2);
    maid.addExperience(-LittleMaidEntity.getConfig().misc.experienceBottleCost);
    return ActionResult.success(maid.getWorld().isClient);
  }

  private static ActionResult handleBucket(
      LittleMaidEntity maid, PlayerEntity player, Hand hand, ItemStack stack) {
    player.playSound(SoundEvents.ENTITY_COW_MILK, 1.0F, 1.0F);
    ItemStack itemStack2 =
        ItemUsage.exchangeStack(stack, player, Items.MILK_BUCKET.getDefaultStack());
    player.setStackInHand(hand, itemStack2);
    return ActionResult.success(maid.getWorld().isClient);
  }

  private static ActionResult handleGunpowder(
      LittleMaidEntity maid, PlayerEntity player, ItemStack stack) {
    LMRBConfig config = LittleMaidEntity.getConfig();
    int maxAccelerationStack = config.misc.maxAccelerationStack;
    int accelerationTicks = config.misc.accelerationTicksPerStack;
    int resumeCount = Math.min(maxAccelerationStack, stack.getCount());
    int acTicks = resumeCount * accelerationTicks;
    maid.setAccelerationTicks(acTicks);

    if (!player.getAbilities().creativeMode) {
      stack.decrement(resumeCount);
      if (stack.isEmpty()) {
        player.getInventory().removeOne(stack);
      }
    }

    return ActionResult.success(maid.getWorld().isClient);
  }

  static ActionResult changeState(LittleMaidEntity maid, PlayerEntity player, ItemStack stack) {
    maid.getWorld().sendEntityStatus(maid, (byte) 72);
    maid.playSound(
        SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, maid.getRandom().nextFloat() * 0.1F + 1.0F);
    maid.setFreedomPos(maid.getBlockPos());
    maid.getNavigation().stop();
    TameableUtil.switchWait(maid);
    consumeItem(player, stack, 1);
    return ActionResult.success(maid.getWorld().isClient);
  }

  static ActionResult contract(
      LittleMaidEntity maid, PlayerEntity player, ItemStack stack, boolean isReContract) {
    if (!isReContract) {
      maid.getWorld().sendEntityStatus(maid, (byte) 70);
      if (player instanceof ServerPlayerEntity) {
        // TODO: NeoForge の RegisterEvent タイミングで Criteria 登録対応後に復活
        // LMRBCriteria.CONTRACT_MAID.trigger((ServerPlayerEntity) player, maid);
      }
    } else {
      maid.getWorld().sendEntityStatus(maid, (byte) 71);
    }
    maid.setOwnerUuid(player.getUuid());
    maid.setContractMM(true);
    if (!maid.getWorld().isClient) {
      SyncMultiModelPacket.sendS2CPacket(maid, maid);
    }
    maid.setStrike(false);
    maid.itemContractable.setUnpaidTimes(0);
    maid.getNavigation().stop();
    maid.setMovingMode(MovingMode.ESCORT);
    consumeItem(player, stack, 1);
    return ActionResult.success(maid.getWorld().isClient);
  }

  private static void consumeItem(PlayerEntity player, ItemStack stack, int amount) {
    if (!player.getAbilities().creativeMode) {
      stack.decrement(amount);
      if (stack.isEmpty()) {
        player.getInventory().removeOne(stack);
      }
    }
  }
}
