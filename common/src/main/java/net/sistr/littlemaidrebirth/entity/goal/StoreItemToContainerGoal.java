package net.sistr.littlemaidrebirth.entity.goal;

import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidrebirth.util.BlockSearch;
import net.sistr.littlemaidrebirth.util.SearchCondition;
import org.jetbrains.annotations.Nullable;

public abstract class StoreItemToContainerGoal<T extends PathAwareEntity> extends Goal {
  protected final T mob;
  protected final Predicate<ItemStack> exceptItems;
  protected final Supplier<Float> searchRangeSq;
  @Nullable protected BlockPos containerPos;
  @Nullable protected BlockSearch blockSearch;
  protected int count;

  public StoreItemToContainerGoal(
      T mob, Predicate<ItemStack> exceptItems, Supplier<Float> searchRange) {
    this.mob = mob;
    this.exceptItems = exceptItems;
    this.searchRangeSq = () -> searchRange.get() * searchRange.get();
    this.setControls(EnumSet.of(Control.MOVE));
  }

  @Override
  public boolean canStart() {
    boolean searching = blockSearch != null && !blockSearch.isFinished() && count++ < 1000;
    // 探索中なら
    if (searching) {
      blockSearch.tick(1);

      var result = blockSearch.getResult();

      // 結果が得られて、かつ仕舞うアイテムがあるなら
      if (result.isPresent() && hasStoreItems()) {
        containerPos = result.get();
        return true;
      }

      return false;
    }

    // shouldStoreItemの毎tickチェックを避ける
    if (this.mob.getRandom().nextInt(20) == 0 && hasStoreItems()) {
      bootBF();
    }
    return false;
  }

  public void bootBF() {
    this.count = 0;
    float searchRangeSq = this.searchRangeSq.get();
    double searchRange = Math.sqrt(searchRangeSq);
    SearchCondition condition =
        SearchCondition.forMob(mob)
            .maxYDiff(2)
            .maxDistance(searchRange)
            .passable(pos -> mob.getWorld().isAir(pos))
            .build();
    blockSearch =
        new BlockSearch(
            this.mob.getBlockPos().up(),
            this::isContainer,
            condition,
            MathHelper.ceil(searchRangeSq));
  }

  @Override
  public boolean shouldContinue() {
    return false;
  }

  protected boolean isContainer(BlockPos pos) {
    BlockState state = mob.getWorld().getBlockState(pos);
    return state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock;
  }

  protected abstract boolean hasStoreItems();

  protected abstract void storeItems();

  @Override
  public void start() {
    storeItems();
  }

  @Override
  public void stop() {
    containerPos = null;
  }

  @Override
  public boolean shouldRunEveryTick() {
    return true;
  }
}
