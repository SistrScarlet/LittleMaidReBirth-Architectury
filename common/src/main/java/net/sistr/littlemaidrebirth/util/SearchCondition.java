package net.sistr.littlemaidrebirth.util;

import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SearchCondition implements Predicate<BlockPos> {
  private final Predicate<BlockPos> predicate;

  private SearchCondition(Predicate<BlockPos> predicate) {
    this.predicate = predicate;
  }

  @Override
  public boolean test(BlockPos pos) {
    return predicate.test(pos);
  }

  public static Builder forMob(LivingEntity mob) {
    return new Builder(mob, null, mob.getWorld());
  }

  public static Builder forPosition(BlockPos center, World world, float entityHeight) {
    return new Builder(null, center.toImmutable(), world).entityHeight(entityHeight);
  }

  public static class Builder {
    @Nullable private final LivingEntity mob;
    @Nullable private final BlockPos center;
    private final World world;
    private double maxDistance = 6;
    private float entityHeight = 1.35f;

    private Builder(@Nullable LivingEntity mob, @Nullable BlockPos center, World world) {
      this.mob = mob;
      this.center = center;
      this.world = world;
      if (mob != null) {
        this.entityHeight = mob.getHeight();
      }
    }

    public Builder maxDistance(double maxDistance) {
      this.maxDistance = maxDistance;
      return this;
    }

    Builder entityHeight(float entityHeight) {
      this.entityHeight = entityHeight;
      return this;
    }

    public SearchCondition build() {
      Predicate<BlockPos> distance = buildDistance();
      World w = world;
      int heightBlocks = MathHelper.ceil(entityHeight);
      Predicate<BlockPos> reachable =
          pos -> isPassable(w, pos) && isNearWalkableFloor(w, pos, heightBlocks);
      return new SearchCondition(distance.and(reachable));
    }

    private Predicate<BlockPos> buildDistance() {
      if (mob != null) {
        LivingEntity m = mob;
        double dist = maxDistance;
        return pos -> pos.isWithinDistance(m.getPos(), dist);
      } else {
        BlockPos c = center;
        double dist = maxDistance;
        return pos -> pos.isWithinDistance(c, dist);
      }
    }

    private static boolean isPassable(World world, BlockPos pos) {
      BlockState state = world.getBlockState(pos);
      return state.isAir()
          || state.canPathfindThrough(world, pos, NavigationType.LAND)
          || (state.getBlock() instanceof DoorBlock
              && ((DoorBlock) state.getBlock()).getBlockSetType().canOpenByHand());
    }

    private static boolean isNearWalkableFloor(World world, BlockPos pos, int heightBlocks) {
      for (int dy = 0; dy < heightBlocks; dy++) {
        BlockPos floorPos = pos.down(dy);
        PathNodeType nodeType = LandPathNodeMaker.getLandNodeType(world, floorPos.mutableCopy());
        if (nodeType == PathNodeType.WALKABLE) {
          return true;
        }
      }
      return false;
    }
  }
}
