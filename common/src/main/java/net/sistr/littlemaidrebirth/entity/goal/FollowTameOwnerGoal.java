package net.sistr.littlemaidrebirth.entity.goal;

import java.util.EnumSet;
import java.util.function.Supplier;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.BirdNavigation;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.passive.TameableEntity;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

public class FollowTameOwnerGoal<T extends TameableEntity> extends Goal {
  protected final T tameable;
  protected final Supplier<Float> speed;
  protected final Supplier<Float> followStartSq;
  protected final Supplier<Float> followEndSq;
  private final EntityNavigation navigation;
  private LivingEntity owner;
  private int updateCountdownTicks;
  private float oldWaterPathfindingPenalty;

  public FollowTameOwnerGoal(
      T tameable, Supplier<Float> speed, Supplier<Float> followStart, Supplier<Float> followEnd) {
    this.tameable = tameable;
    this.speed = speed;
    this.followStartSq = () -> followStart.get() * followStart.get();
    this.followEndSq = () -> followEnd.get() * followEnd.get();
    this.navigation = tameable.getNavigation();
    this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    if (!(tameable.getNavigation() instanceof MobNavigation)
        && !(tameable.getNavigation() instanceof BirdNavigation)) {
      throw new IllegalArgumentException("Unsupported mob type for FollowOwnerGoal");
    }
  }

  @Override
  public boolean canStart() {
    LivingEntity tameOwner = TameableUtil.getTameOwner(tameable).orElse(null);
    if (tameOwner == null) {
      return false;
    } else if (tameOwner.isSpectator()) {
      return false;
    } else if (TameableUtil.isWait(tameable)) {
      return false;
    } else if (this.tameable.squaredDistanceTo(tameOwner) < followStartSq.get()) {
      return false;
    } else {
      this.owner = tameOwner;
      return true;
    }
  }

  public boolean shouldContinue() {
    if (this.navigation.isIdle()) {
      return false;
    } else if (TameableUtil.isWait(tameable)) {
      return false;
    } else {
      return followEndSq.get() < this.tameable.squaredDistanceTo(this.owner);
    }
  }

  @Override
  public void start() {
    this.updateCountdownTicks = 0;
    this.oldWaterPathfindingPenalty = this.tameable.getPathfindingPenalty(PathNodeType.WATER);
    this.tameable.setPathfindingPenalty(PathNodeType.WATER, 0.0f);
  }

  @Override
  public void stop() {
    this.owner = null;
    this.navigation.stop();
    this.tameable.setPathfindingPenalty(PathNodeType.WATER, this.oldWaterPathfindingPenalty);
  }

  @Override
  public void tick() {
    this.tameable.getLookControl().lookAt(this.owner, 10.0f, this.tameable.getMaxLookPitchChange());
    if (--this.updateCountdownTicks > 0) {
      return;
    }
    this.updateCountdownTicks = getTickCount(10);
    this.navigation.startMovingTo(this.owner, this.speed.get());
  }
}
