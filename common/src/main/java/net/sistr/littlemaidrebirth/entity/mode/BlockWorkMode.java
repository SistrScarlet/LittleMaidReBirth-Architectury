package net.sistr.littlemaidrebirth.entity.mode;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.util.BlockFinder;
import org.jetbrains.annotations.Nullable;

public final class BlockWorkMode extends Mode {
  private final Delegate<?> delegate;

  public <T extends BlockEntity> BlockWorkMode(
      ModeType<? extends Mode> modeType,
      String name,
      LittleMaidEntity mob,
      BlockReservationManager reservationManager,
      WorkStrategy<T> strategy) {
    super(modeType, name);
    this.delegate = new Delegate<>(mob, reservationManager, strategy);
  }

  @Override
  public boolean shouldExecute() {
    return delegate.shouldExecute();
  }

  @Override
  public boolean shouldContinueExecuting() {
    return delegate.shouldContinueExecuting();
  }

  @Override
  public void startExecuting() {
    delegate.startExecuting();
  }

  @Override
  public void tick() {
    delegate.tick();
  }

  @Override
  public void resetTask() {
    delegate.resetTask();
  }

  @Override
  public void writeModeData(NbtCompound nbt) {
    delegate.writeModeData(nbt);
  }

  @Override
  public void readModeData(NbtCompound nbt) {
    delegate.readModeData(nbt);
  }

  private static final class Delegate<T extends BlockEntity> {
    private final LittleMaidEntity mob;
    private final BlockReservationManager reservationManager;
    private final WorkStrategy<T> strategy;
    @Nullable private BlockPos targetPos;
    @Nullable private T targetBlockEntity;
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

    Delegate(
        LittleMaidEntity mob,
        BlockReservationManager reservationManager,
        WorkStrategy<T> strategy) {
      this.mob = mob;
      this.reservationManager = reservationManager;
      this.strategy = strategy;
    }

    boolean shouldExecute() {
      if (0 < --findCooldown) {
        return false;
      }
      findCooldown = 20;

      World world = mob.getWorld();
      // @Nullable フィールドをローカル変数にキャッシュ
      BlockPos pos = targetPos;

      // モードが中断されたあと、再開するときの判定
      if (pos != null
          && pos.isWithinDistance(mob.getPos(), 6)
          && !reservationManager.isReservedByOther(world, pos, mob)) {
        T be = strategy.getBlockEntity(world, pos).orElse(null);
        if (be != null && strategy.hasRemainingWork(be)) {
          targetBlockEntity = be;
          return true;
        }
      } else {
        targetPos = null;
        pos = null;
      }

      // 新規作業を開始するときの判定
      if (!strategy.hasRequiredItems(mob.getInventory())) {
        return false;
      }

      // 既存のターゲットがまだ使用可能かチェック
      if (pos != null) {
        T be = strategy.getBlockEntity(world, pos).orElse(null);
        if (be != null && strategy.isUsableTarget(be, mob.getInventory(), world)) {
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
      targetBlockEntity = strategy.getBlockEntity(world, pos).orElseThrow();
      return true;
    }

    boolean shouldContinueExecuting() {
      BlockPos pos = targetPos;
      T be = targetBlockEntity;
      if (pos == null || be == null) {
        return false;
      }

      // ブロックエンティティが差し替えられていたら終了
      T current = strategy.getBlockEntity(mob.getWorld(), pos).orElse(null);
      if (current != be) {
        targetPos = null;
        targetBlockEntity = null;
        return false;
      }

      return strategy.shouldContinueWork(be, mob.getInventory(), mob.getWorld());
    }

    void startExecuting() {
      findCooldown = 0;
      BlockPos pos = targetPos;
      if (pos != null) {
        reservationManager.reserve(mob.getWorld(), pos, mob);
      }
      mob.play(LMSounds.COOKING_START);
      soundCooldown = 20;
    }

    void tick() {
      BlockPos pos = targetPos;
      T be = targetBlockEntity;
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

      strategy.doWork(be, mob.getInventory(), mob.getWorld(), actions);
    }

    void resetTask() {
      soundCooldown = 0;
      mob.setSneaking(false);
      BlockPos pos = targetPos;
      if (pos != null) {
        reservationManager.release(mob.getWorld(), pos, mob);
        T be = strategy.getBlockEntity(mob.getWorld(), pos).orElse(null);
        if (be == null) {
          targetPos = null;
          return;
        }
        strategy.extractAll(be, mob.getInventory(), actions);
      }
    }

    void writeModeData(NbtCompound nbt) {
      BlockPos pos = targetPos;
      if (pos != null) {
        nbt.put(strategy.blockPosNbtKey(), NbtHelper.fromBlockPos(pos));
      }
    }

    void readModeData(NbtCompound nbt) {
      String key = strategy.blockPosNbtKey();
      if (nbt.contains(key)) {
        targetPos = NbtHelper.toBlockPos(nbt.getCompound(key));
      }
    }

    private Optional<BlockPos> findTargetPos() {
      return BlockFinder.searchTargetBlock(
          mob.getBlockPos(),
          this::isTargetBlock,
          this::isSearchable,
          Arrays.asList(Direction.values()),
          128);
    }

    private boolean isTargetBlock(BlockPos pos) {
      if (reservationManager.isReservedByOther(mob.getWorld(), pos, mob)) {
        return false;
      }
      return strategy
          .getBlockEntity(mob.getWorld(), pos)
          .filter(be -> strategy.isUsableTarget(be, mob.getInventory(), mob.getWorld()))
          .isPresent();
    }

    private boolean isSearchable(BlockPos pos) {
      BlockState state;
      return Math.abs(pos.getY() - mob.getY()) < 2
          && pos.isWithinDistance(mob.getPos(), 6)
          && ((state = mob.getWorld().getBlockState(pos))
                  .canPathfindThrough(mob.getWorld(), pos, NavigationType.LAND)
              || (state.getBlock() instanceof DoorBlock
                  && ((DoorBlock) state.getBlock()).getBlockSetType().canOpenByHand()));
    }
  }
}
