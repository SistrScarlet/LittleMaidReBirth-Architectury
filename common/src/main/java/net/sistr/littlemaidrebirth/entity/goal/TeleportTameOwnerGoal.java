package net.sistr.littlemaidrebirth.entity.goal;

import java.util.EnumSet;
import java.util.function.Supplier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

public class TeleportTameOwnerGoal<T extends PathAwareEntity & Tameable> extends Goal {
  protected final T tameable;
  protected final World world;
  protected final Supplier<Float> teleportStartSq;
  private final EntityNavigation navigation;
  private LivingEntity owner;
  private int updateCountdownTicks;

  public TeleportTameOwnerGoal(T tameable, Supplier<Float> teleportStart) {
    this.tameable = tameable;
    this.world = tameable.getWorld();
    this.teleportStartSq = () -> teleportStart.get() * teleportStart.get();
    this.navigation = tameable.getNavigation();
    this.setControls(EnumSet.of(Control.MOVE));
  }

  @Override
  public boolean canStart() {
    LivingEntity tameOwner = TameableUtil.getTameOwner(this.tameable).orElse(null);
    if (tameOwner == null) {
      return false;
    } else if (tameOwner.isSpectator()) {
      return false;
    } else if (this.tameable.squaredDistanceTo(tameOwner) < teleportStartSq.get()) {
      return false;
    } else {
      this.owner = tameOwner;
      return true;
    }
  }

  public boolean shouldContinue() {
    return teleportStartSq.get() < this.tameable.squaredDistanceTo(this.owner);
  }

  @Override
  public void start() {
    this.updateCountdownTicks = 0;
  }

  @Override
  public void stop() {
    this.owner = null;
    this.navigation.stop();
  }

  @Override
  public void tick() {
    this.tameable.getLookControl().lookAt(this.owner, 10.0f, this.tameable.getMaxLookPitchChange());
    if (--this.updateCountdownTicks > 0) {
      return;
    }
    this.updateCountdownTicks = getTickCount(LMRBMod.getConfig().movement.pathRecalcInterval);
    tryTeleport();
  }

  protected void tryTeleport() {
    BlockPos ownerPos = this.owner.getBlockPos();
    for (int i = 0; i < getConfigMaxTryTeleportCount(); ++i) {
      int teleportWidthRange = getConfigTeleportWidthRange();
      int teleportHeightRange = getConfigTeleportHeightRange();
      int x = this.getRandomInt(-teleportWidthRange, teleportWidthRange);
      int y = this.getRandomInt(-teleportHeightRange, teleportHeightRange);
      int z = this.getRandomInt(-teleportWidthRange, teleportWidthRange);
      boolean bl =
          this.tryTeleportTo(ownerPos.getX() + x, ownerPos.getY() + y, ownerPos.getZ() + z);
      if (!bl) continue;
      return;
    }
  }

  protected boolean tryTeleportTo(int x, int y, int z) {
    if (isOwnerRange(this.owner, x, y, z)) {
      return false;
    }
    if (!this.canTeleportTo(new BlockPos(x, y, z))) {
      return false;
    }
    this.tameable.refreshPositionAndAngles(
        x + 0.5, y, z + 0.5, this.tameable.getYaw(), this.tameable.getPitch());
    this.navigation.stop();
    return true;
  }

  protected boolean isOwnerRange(Entity owner, int x, int y, int z) {
    if (getConfigCanTeleportOwnerForwards()) {
      return false;
    }
    Vec3d ownerPos = owner.getPos();
    Vec3d entityPos = new Vec3d(x + 0.5, y, z + 0.5).subtract(ownerPos);
    Vec3d ownerRot = owner.getRotationVec(1F);
    double dot = entityPos.dotProduct(ownerRot);
    double range = getConfigOwnerForwardRange();
    // プレイヤー位置を原点としたアイテムの位置と、プレイヤーの向きの内積がプラス
    // かつ内積の大きさが4m以下
    return 0 < dot && dot < range * range;
  }

  protected boolean canTeleportTo(BlockPos pos) {
    PathNodeType pathNodeType = LandPathNodeMaker.getLandNodeType(this.world, pos.mutableCopy());
    if (pathNodeType != PathNodeType.WALKABLE) {
      return false;
    }
    BlockPos blockPos = pos.subtract(this.tameable.getBlockPos());
    return this.world.isSpaceEmpty(this.tameable, this.tameable.getBoundingBox().offset(blockPos));
  }

  protected int getRandomInt(int min, int max) {
    return this.tameable.getRandom().nextInt(max - min + 1) + min;
  }

  protected boolean getConfigCanTeleportOwnerForwards() {
    return LMRBMod.getConfig().movement.canTeleportOwnerForwards;
  }

  protected float getConfigOwnerForwardRange() {
    return LMRBMod.getConfig().movement.ownerForwardRange;
  }

  protected int getConfigMaxTryTeleportCount() {
    return LMRBMod.getConfig().movement.maxTryTeleportCount;
  }

  protected int getConfigTeleportWidthRange() {
    return LMRBMod.getConfig().movement.teleportWidth;
  }

  protected int getConfigTeleportHeightRange() {
    return LMRBMod.getConfig().movement.teleportHeight;
  }
}
