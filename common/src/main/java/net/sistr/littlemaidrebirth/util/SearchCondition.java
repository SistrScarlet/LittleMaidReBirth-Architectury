package net.sistr.littlemaidrebirth.util;

import java.util.function.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.util.math.BlockPos;
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

  public static Builder forPosition(BlockPos center, World world) {
    return new Builder(null, center.toImmutable(), world);
  }

  public static class Builder {
    @Nullable private final LivingEntity mob;
    @Nullable private final BlockPos center;
    private final World world;
    private int maxYDiff = 2;
    private double maxDistance = 6;
    @Nullable private Predicate<BlockPos> passable;

    private Builder(@Nullable LivingEntity mob, @Nullable BlockPos center, World world) {
      this.mob = mob;
      this.center = center;
      this.world = world;
    }

    public Builder maxYDiff(int maxYDiff) {
      this.maxYDiff = maxYDiff;
      return this;
    }

    public Builder maxDistance(double maxDistance) {
      this.maxDistance = maxDistance;
      return this;
    }

    public Builder passable(Predicate<BlockPos> passable) {
      this.passable = passable;
      return this;
    }

    public SearchCondition build() {
      Predicate<BlockPos> spatial = buildSpatial();
      Predicate<BlockPos> pass = passable != null ? passable : defaultPassable();
      return new SearchCondition(spatial.and(pass));
    }

    private Predicate<BlockPos> buildSpatial() {
      if (mob != null) {
        LivingEntity m = mob;
        int yDiff = maxYDiff;
        double dist = maxDistance;
        return pos ->
            Math.abs(pos.getY() - m.getY()) < yDiff && pos.isWithinDistance(m.getPos(), dist);
      } else {
        BlockPos c = center;
        int yDiff = maxYDiff;
        double dist = maxDistance;
        return pos -> Math.abs(pos.getY() - c.getY()) < yDiff && pos.isWithinDistance(c, dist);
      }
    }

    private Predicate<BlockPos> defaultPassable() {
      World w = world;
      return pos -> {
        BlockState state = w.getBlockState(pos);
        return state.canPathfindThrough(w, pos, NavigationType.LAND)
            || (state.getBlock() instanceof DoorBlock
                && ((DoorBlock) state.getBlock()).getBlockSetType().canOpenByHand());
      };
    }
  }
}
