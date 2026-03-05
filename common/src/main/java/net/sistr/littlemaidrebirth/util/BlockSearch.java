package net.sistr.littlemaidrebirth.util;

import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class BlockSearch {
  private static final Direction[] DIRECTIONS = Direction.values();

  private final Queue<BlockPos> seeds;
  private final Predicate<BlockPos> target;
  private final Predicate<BlockPos> linkable;
  private final Set<BlockPos> searched;
  private final int maxCount;
  private int count;
  @Nullable private BlockPos result;

  public BlockSearch(
      BlockPos seed, Predicate<BlockPos> target, Predicate<BlockPos> linkable, int maxCount) {
    this.seeds = Queues.newArrayDeque();
    this.target = target;
    this.linkable = linkable;
    this.searched = new ObjectOpenHashSet<>(Math.min(maxCount, 256));
    this.maxCount = maxCount;
    if (target.test(seed)) {
      this.result = seed;
    } else {
      this.seeds.add(seed);
    }
  }

  public Optional<BlockPos> tick(int budget) {
    if (result != null) {
      return Optional.of(result);
    }
    for (int i = 0; i < budget && !seeds.isEmpty() && count < maxCount; i++) {
      BlockPos current = seeds.poll();
      if (searched.contains(current)) {
        continue;
      }
      searched.add(current);
      count++;

      for (Direction direction : DIRECTIONS) {
        BlockPos next = current.offset(direction);
        if (searched.contains(next)) {
          continue;
        }
        if (target.test(next)) {
          result = next;
          seeds.clear();
          return Optional.of(result);
        }
        if (linkable.test(next)) {
          seeds.add(next);
        }
      }
    }
    return Optional.empty();
  }

  public boolean isFinished() {
    return result != null || seeds.isEmpty() || count >= maxCount;
  }

  public Optional<BlockPos> getResult() {
    return Optional.ofNullable(result);
  }
}
