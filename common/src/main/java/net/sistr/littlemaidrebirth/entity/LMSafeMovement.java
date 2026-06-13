package net.sistr.littlemaidrebirth.entity;

import java.util.function.BiPredicate;
import java.util.function.Supplier;
import net.minecraft.entity.ai.pathing.PathContext;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.sistr.littlemaidrebirth.config.LMRBConfig;

/** メイドさんの安全移動ロジック。 崖や危険ブロック（溶岩・火等）を検知して移動を抑制する。 */
public class LMSafeMovement {
  private final MobEntity mob;
  private final Supplier<LMRBConfig> configSupplier;
  private final Supplier<Float> dangerHeightSupplier;

  public LMSafeMovement(
      MobEntity mob, Supplier<LMRBConfig> configSupplier, Supplier<Float> dangerHeightSupplier) {
    this.mob = mob;
    this.configSupplier = configSupplier;
    this.dangerHeightSupplier = dangerHeightSupplier;
  }

  public Vec3d adjust(Vec3d movement) {
    LMRBConfig config = configSupplier.get();

    if (!config.health.immortal
        && !config.health.nonMobDamageImmunity
        && config.health.enableSafeMove
        && canClipAtLedge()) {
      boolean shouldBackByDamage =
          isDamageSourceEmpty(mob.getBoundingBox())
              && !isDamageSourceEmpty(mob.getBoundingBox().offset(movement.x, 0, movement.z));
      boolean shouldBackByFall =
          !config.health.fallImmunity
              && !isSafeFallHeight(mob.getPos().add(movement.x, 0, movement.z));

      if (shouldBackByDamage || shouldBackByFall) {
        BiPredicate<Double, Double> shouldBackPredicate = (x, z) -> false;
        if (shouldBackByDamage) {
          BiPredicate<Double, Double> finalPredicate = shouldBackPredicate;
          shouldBackPredicate =
              (x, z) ->
                  finalPredicate.test(x, z)
                      || !isDamageSourceEmpty(mob.getBoundingBox().offset(x, 0, z));
        }

        if (shouldBackByFall) {
          float dangerHeight = dangerHeightSupplier.get();
          BiPredicate<Double, Double> finalPredicate = shouldBackPredicate;
          shouldBackPredicate =
              (x, z) ->
                  finalPredicate.test(x, z)
                      || mob.getWorld()
                          .isSpaceEmpty(
                              mob,
                              mob.getBoundingBox()
                                  .offset(x, 0, z)
                                  .stretch(0, -(dangerHeight - mob.fallDistance), 0))
                      || (mob.getWorld()
                              .isSpaceEmpty(
                                  mob,
                                  mob.getBoundingBox()
                                      .offset(x, 0, z)
                                      .stretch(0, -mob.getStepHeight(), 0))
                          && !isDamageSourceEmpty(
                              mob.getBoundingBox().offset(x, 0, z).stretch(0, -dangerHeight, 0)));
        }

        movement = pushBack(movement, shouldBackPredicate);
      }
    }

    return movement;
  }

  private Vec3d pushBack(Vec3d movement, BiPredicate<Double, Double> pushBackPredicate) {
    double dot = 0.05;
    double mX = movement.x;
    double mZ = movement.z;
    while (mX != 0.0 && pushBackPredicate.test(mX, 0d)) {
      if (mX < dot && mX >= -dot) {
        mX = 0.0;
        continue;
      }
      if (mX > 0.0) {
        mX -= dot;
        continue;
      }
      mX += dot;
    }
    while (mZ != 0.0 && pushBackPredicate.test(0d, mZ)) {
      if (mZ < dot && mZ >= -dot) {
        mZ = 0.0;
        continue;
      }
      if (mZ > 0.0) {
        mZ -= dot;
        continue;
      }
      mZ += dot;
    }
    while (mX != 0.0 && mZ != 0.0 && pushBackPredicate.test(mX, mZ)) {
      mX = mX < dot && mX >= -dot ? 0.0 : (mX > 0.0 ? mX - dot : mX + dot);
      if (mZ < dot && mZ >= -dot) {
        mZ = 0.0;
        continue;
      }
      if (mZ > 0.0) {
        mZ -= dot;
        continue;
      }
      mZ += dot;
    }
    return new Vec3d(mX, movement.y, mZ);
  }

  private boolean isDamageSourceEmpty(Box box) {
    int minX = MathHelper.floor(box.minX);
    int maxX = MathHelper.floor(box.maxX);
    int minY = MathHelper.floor(box.minY);
    int maxY = MathHelper.floor(box.maxY);
    int minZ = MathHelper.floor(box.minZ);
    int maxZ = MathHelper.floor(box.maxZ);

    PathContext context = new PathContext(mob.getWorld(), mob);
    for (int x = 0; x < maxX - minX + 1; x++) {
      for (int y = 0; y < maxY - minY + 1; y++) {
        for (int z = 0; z < maxZ - minZ + 1; z++) {
          PathNodeType pathNodeType =
              mob.getNavigation()
                  .getNodeMaker()
                  .getDefaultNodeType(context, minX + x, minY + y, minZ + z);
          if (pathNodeType == PathNodeType.DAMAGE_FIRE
              || pathNodeType == PathNodeType.DAMAGE_OTHER
              || pathNodeType == PathNodeType.LAVA) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private boolean isSafeFallHeight(Vec3d pos) {
    float dangerHeight = dangerHeightSupplier.get();
    BlockHitResult result =
        mob.getWorld()
            .raycast(
                new RaycastContext(
                    pos,
                    pos.subtract(0, dangerHeight - mob.fallDistance + 0.1, 0),
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    mob));
    if (result.getType() == HitResult.Type.MISS) {
      return false;
    }
    Vec3d hitPos = result.getPos();
    if (dangerHeight - mob.fallDistance < pos.y - hitPos.y) {
      return false;
    }
    BlockPos checkPos =
        new BlockPos(MathHelper.floor(pos.x), MathHelper.floor(pos.y - 1), MathHelper.floor(pos.z));
    PathContext context = new PathContext(mob.getWorld(), mob);
    for (int i = 0; i < pos.y - hitPos.y + 1; i++) {
      PathNodeType pathNodeType =
          mob.getNavigation()
              .getNodeMaker()
              .getDefaultNodeType(context, checkPos.getX(), checkPos.getY(), checkPos.getZ());
      if (pathNodeType == PathNodeType.WALKABLE || pathNodeType == PathNodeType.BLOCKED) {
        return true;
      }
      if (pathNodeType == PathNodeType.DAMAGE_FIRE
          || pathNodeType == PathNodeType.DAMAGE_OTHER
          || pathNodeType == PathNodeType.LAVA) {
        return false;
      }
      checkPos = checkPos.down();
    }
    return false;
  }

  private boolean canClipAtLedge() {
    float dangerHeight = dangerHeightSupplier.get();
    float canClipHeight = dangerHeight + 1.0f;
    return mob.isOnGround()
        || mob.fallDistance < canClipHeight
            && !mob.getWorld()
                .isSpaceEmpty(
                    mob, mob.getBoundingBox().stretch(0.0, mob.fallDistance - canClipHeight, 0.0));
  }
}
