package net.sistr.littlemaidrebirth.entity.goal;

import java.util.EnumSet;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.item.ItemStack;
import net.sistr.littlemaidrebirth.LMRBMod;

public class FollowAtHeldItemGoal<T extends TameableEntity> extends TameableStareAtHeldItemGoal<T> {
  private final Supplier<Float> followRangeSq;
  protected int reCalcCool;

  public FollowAtHeldItemGoal(
      T mob,
      Supplier<Float> stareAtRange,
      Predicate<ItemStack> targetItem,
      Supplier<Float> followRange,
      boolean isTamed) {
    super(mob, stareAtRange, targetItem, isTamed);
    this.followRangeSq = () -> followRange.get() * followRange.get();
    setControls(EnumSet.of(Control.MOVE));
  }

  @Override
  public void tick() {
    super.tick();
    if (mob.squaredDistanceTo(stareAt) < followRangeSq.get()) {
      mob.getNavigation().stop();
      return;
    }
    if (0 < reCalcCool--) {
      return;
    }
    reCalcCool = getTickCount(LMRBMod.getConfig().movement.pathRecalcInterval);
    mob.getNavigation().startMovingTo(stareAt, 1);
  }
}
