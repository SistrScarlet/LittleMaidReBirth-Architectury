package net.sistr.littlemaidrebirth.entity.mode;

import java.util.Optional;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.util.BlockSearch;
import net.sistr.littlemaidrebirth.util.SearchCondition;
import org.jetbrains.annotations.Nullable;

public final class BlockWorkMode extends Mode {
  private final LittleMaidEntity mob;
  private final BlockReservationManager reservationManager;
  private final WorkStrategy<?> strategy;
  @Nullable private BlockPos targetPos;
  @Nullable private BlockEntity targetBlockEntity;
  private int findCooldown;
  private int pathRecalcCooldown;
  private int soundCooldown;

  private final WorkActions actions =
      new WorkActions() {
        @Override
        public void pickupAction() {
          mob.swingHand(Hand.MAIN_HAND);
          mob.playSound(
              SoundEvents.ENTITY_ITEM_PICKUP, 1.0F, mob.getRandom().nextFloat() * 0.1F + 1.0F);
        }

        @Override
        public void playSoundIfReady(String sound) {
          if (soundCooldown < 0) {
            soundCooldown = 20;
            mob.play(sound);
          }
        }
      };

  public <T extends BlockEntity> BlockWorkMode(
      ModeType<? extends Mode> modeType,
      String name,
      LittleMaidEntity mob,
      BlockReservationManager reservationManager,
      WorkStrategy<T> strategy) {
    super(modeType, name);
    this.mob = mob;
    this.reservationManager = reservationManager;
    this.strategy = strategy;
  }

  // strategy と targetBlockEntity は常にコンストラクタで対になるため、キャストは安全
  @SuppressWarnings("unchecked")
  private <T extends BlockEntity> WorkStrategy<T> typedStrategy() {
    return (WorkStrategy<T>) strategy;
  }

  @Override
  public boolean shouldExecute() {
    if (0 < --findCooldown) {
      return false;
    }
    findCooldown = 20;

    WorkStrategy<BlockEntity> s = typedStrategy();
    World world = mob.getWorld();
    // @Nullable フィールドをローカル変数にキャッシュ
    BlockPos pos = targetPos;

    // モードが中断されたあと、再開するときの判定
    if (pos != null
        && pos.isWithinDistance(mob.getPos(), 6)
        && !reservationManager.isReservedByOther(world, pos, mob)) {
      BlockEntity be = s.getBlockEntity(world, pos).orElse(null);
      if (be != null && s.hasRemainingWork(be)) {
        targetBlockEntity = be;
        return true;
      }
    } else {
      targetPos = null;
      pos = null;
    }

    // 新規作業を開始するときの判定
    if (!s.hasRequiredItems(mob.getInventory(), world)) {
      return false;
    }

    // 既存のターゲットがまだ使用可能かチェック
    if (pos != null) {
      BlockEntity be = s.getBlockEntity(world, pos).orElse(null);
      if (be != null && s.isUsableTarget(be, mob.getInventory(), world)) {
        targetBlockEntity = be;
        return true;
      }
    }

    // 新しいターゲットを探索
    pos = findTargetPos().orElse(null);
    if (pos == null) {
      return false;
    }
    targetPos = pos;
    targetBlockEntity = s.getBlockEntity(world, pos).orElseThrow();
    return true;
  }

  @Override
  public boolean shouldContinueExecuting() {
    WorkStrategy<BlockEntity> s = typedStrategy();
    BlockPos pos = targetPos;
    BlockEntity be = targetBlockEntity;
    if (pos == null || be == null) {
      return false;
    }

    // ブロックエンティティが差し替えられていたら終了
    BlockEntity current = s.getBlockEntity(mob.getWorld(), pos).orElse(null);
    if (current != be) {
      targetPos = null;
      targetBlockEntity = null;
      return false;
    }

    return s.shouldContinueWork(be, mob.getInventory(), mob.getWorld());
  }

  @Override
  public void startExecuting() {
    findCooldown = 0;
    BlockPos pos = targetPos;
    if (pos != null) {
      reservationManager.reserve(mob.getWorld(), pos, mob);
    }
    mob.play(LMSounds.COOKING_START);
    soundCooldown = 20;
  }

  @Override
  public void tick() {
    BlockPos pos = targetPos;
    BlockEntity be = targetBlockEntity;
    if (pos == null || be == null) {
      return;
    }

    // 視線を向ける
    mob.getLookControl().lookAt(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);

    // ターゲットの近くに移動
    if (!mob.getBlockPos().isWithinDistance(pos, 1.75)) {
      if (mob.isSneaking()) {
        mob.setSneaking(false);
      }
      if (--pathRecalcCooldown <= 0) {
        pathRecalcCooldown = 10;
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 0.5D;
        double z = pos.getZ() + 0.5D;
        var path = mob.getNavigation().findPathTo(x, y, z, 2);
        mob.getNavigation().startMovingAlong(path, 1);
      }
      return;
    }
    mob.getNavigation().stop();

    // しゃがむ
    if (!mob.isSneaking()) {
      mob.setSneaking(true);
    }

    soundCooldown--;

    WorkStrategy<BlockEntity> s = typedStrategy();
    s.doWork(be, mob.getInventory(), mob.getWorld(), actions);
  }

  @Override
  public void resetTask() {
    soundCooldown = 0;
    mob.setSneaking(false);
    BlockPos pos = targetPos;
    if (pos != null) {
      reservationManager.release(mob.getWorld(), pos, mob);
      WorkStrategy<BlockEntity> s = typedStrategy();
      BlockEntity be = s.getBlockEntity(mob.getWorld(), pos).orElse(null);
      if (be == null) {
        targetPos = null;
        return;
      }
      s.extractAll(be, mob.getInventory(), actions);
    }
  }

  @Override
  public void writeModeData(NbtCompound nbt) {
    BlockPos pos = targetPos;
    if (pos != null) {
      nbt.put(strategy.blockPosNbtKey(), NbtHelper.fromBlockPos(pos));
    }
  }

  @Override
  public void readModeData(NbtCompound nbt) {
    String key = strategy.blockPosNbtKey();
    if (nbt.contains(key)) {
      targetPos = NbtHelper.toBlockPos(nbt.getCompound(key));
    }
  }

  private Optional<BlockPos> findTargetPos() {
    SearchCondition condition = SearchCondition.forMob(mob).maxYDiff(2).maxDistance(6).build();
    BlockSearch search = new BlockSearch(mob.getBlockPos(), this::isTargetBlock, condition, 128);
    return search.tick(128);
  }

  private boolean isTargetBlock(BlockPos pos) {
    if (reservationManager.isReservedByOther(mob.getWorld(), pos, mob)) {
      return false;
    }
    WorkStrategy<BlockEntity> s = typedStrategy();
    return s.getBlockEntity(mob.getWorld(), pos)
        .filter(be -> s.isUsableTarget(be, mob.getInventory(), mob.getWorld()))
        .isPresent();
  }
}
