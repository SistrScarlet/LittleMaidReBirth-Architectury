package net.sistr.littlemaidrebirth.entity.goal;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;
import org.jetbrains.annotations.Nullable;

public class TeleportTameOwnerGoal<T extends PathAwareEntity & Tameable> extends Goal {
  protected final T tameable;
  protected final Supplier<Float> teleportStartSq;
  private final EntityNavigation navigation;
  @Nullable private LivingEntity owner;
  private boolean crossDimension;

  public TeleportTameOwnerGoal(T tameable, Supplier<Float> teleportStart) {
    this.tameable = tameable;
    this.teleportStartSq = () -> teleportStart.get() * teleportStart.get();
    this.navigation = tameable.getNavigation();
    this.setControls(EnumSet.of(Control.MOVE));
  }

  @Override
  public boolean canStart() {
    if (!(this.tameable.getWorld() instanceof ServerWorld serverWorld)) {
      return false;
    }
    LivingEntity foundOwner =
        TameableUtil.getCrossWorldTameOwner(serverWorld, this.tameable).orElse(null);
    if (foundOwner == null || foundOwner.isSpectator()) {
      return false;
    }
    boolean isCross = foundOwner.getWorld() != this.tameable.getWorld();
    if (!isCross && this.tameable.squaredDistanceTo(foundOwner) < teleportStartSq.get()) {
      return false;
    }
    this.owner = foundOwner;
    this.crossDimension = isCross;
    return true;
  }

  @Override
  public boolean shouldContinue() {
    return canStart();
  }

  @Override
  public void start() {}

  @Override
  public void stop() {
    this.owner = null;
    this.crossDimension = false;
    this.navigation.stop();
  }

  @Override
  public void tick() {
    LivingEntity cachedOwner = this.owner;
    if (cachedOwner == null) {
      return;
    }
    tryTeleport();
  }

  protected void tryTeleport() {
    LivingEntity cachedOwner = this.owner;
    if (cachedOwner == null) {
      return;
    }
    BlockPos ownerPos = cachedOwner.getBlockPos();
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
    LivingEntity cachedOwner = this.owner;
    if (cachedOwner == null) {
      return false;
    }
    if (isOwnerRange(cachedOwner, x, y, z)) {
      return false;
    }
    World targetWorld = cachedOwner.getWorld();
    if (!this.canTeleportTo(targetWorld, new BlockPos(x, y, z))) {
      return false;
    }
    if (crossDimension) {
      this.tameable.teleport(
          (ServerWorld) targetWorld,
          x + 0.5,
          y,
          z + 0.5,
          Set.of(),
          this.tameable.getYaw(),
          this.tameable.getPitch());
      targetWorld.playSound(
          null,
          x + 0.5,
          y,
          z + 0.5,
          SoundEvents.ENTITY_ENDERMAN_TELEPORT,
          this.tameable.getSoundCategory(),
          1.0f,
          2.0f);
    } else {
      this.tameable.refreshPositionAndAngles(
          x + 0.5, y, z + 0.5, this.tameable.getYaw(), this.tameable.getPitch());
      this.navigation.stop();
    }
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

  protected boolean canTeleportTo(World targetWorld, BlockPos pos) {
    PathNodeType pathNodeType = LandPathNodeMaker.getLandNodeType(targetWorld, pos.mutableCopy());
    if (pathNodeType != PathNodeType.WALKABLE) {
      return false;
    }
    BlockPos blockPos = pos.subtract(this.tameable.getBlockPos());
    return targetWorld.isSpaceEmpty(this.tameable, this.tameable.getBoundingBox().offset(blockPos));
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
