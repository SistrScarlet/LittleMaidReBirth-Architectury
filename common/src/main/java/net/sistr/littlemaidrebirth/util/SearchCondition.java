package net.sistr.littlemaidrebirth.util;

import java.util.function.Predicate;
import net.minecraft.entity.ai.pathing.PathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.MobEntity;
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

  /** 距離をメイドさんの現在位置から測定する */
  public static Builder forMob(MobEntity mob) {
    return new Builder(mob, null);
  }

  /** 距離を指定位置から測定する（パスファインディングはメイドさん基準） */
  public static Builder forMob(MobEntity mob, BlockPos center) {
    return new Builder(mob, center.toImmutable());
  }

  public static class Builder {
    private final MobEntity mob;
    @Nullable private final BlockPos center;
    private double maxDistance = 6;

    private Builder(MobEntity mob, @Nullable BlockPos center) {
      this.mob = mob;
      this.center = center;
    }

    public Builder maxDistance(double maxDistance) {
      this.maxDistance = maxDistance;
      return this;
    }

    public SearchCondition build() {
      Predicate<BlockPos> distance = buildDistance();
      MobEntity m = mob;
      World w = m.getWorld();
      PathNodeMaker nodeMaker = m.getNavigation().getNodeMaker();
      int heightBlocks = MathHelper.ceil(m.getHeight());
      Predicate<BlockPos> reachable =
          pos ->
              isPassable(nodeMaker, w, pos) && isNearWalkableFloor(nodeMaker, w, pos, heightBlocks);
      return new SearchCondition(distance.and(reachable));
    }

    private Predicate<BlockPos> buildDistance() {
      double dist = maxDistance;
      if (center != null) {
        BlockPos c = center;
        return pos -> pos.isWithinDistance(c, dist);
      } else {
        MobEntity m = mob;
        return pos -> pos.isWithinDistance(m.getPos(), dist);
      }
    }

    private static boolean isPassable(PathNodeMaker nodeMaker, World world, BlockPos pos) {
      PathNodeType type = nodeMaker.getDefaultNodeType(world, pos.getX(), pos.getY(), pos.getZ());
      return type != PathNodeType.BLOCKED;
    }

    private static boolean isNearWalkableFloor(
        PathNodeMaker nodeMaker, World world, BlockPos pos, int heightBlocks) {
      for (int dy = 0; dy < heightBlocks; dy++) {
        BlockPos floorPos = pos.down(dy);
        PathNodeType type =
            nodeMaker.getDefaultNodeType(world, floorPos.getX(), floorPos.getY(), floorPos.getZ());
        if (type == PathNodeType.WALKABLE) {
          return true;
        }
      }
      return false;
    }
  }
}
