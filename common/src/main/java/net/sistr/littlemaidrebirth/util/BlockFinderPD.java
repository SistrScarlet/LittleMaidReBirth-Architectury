package net.sistr.littlemaidrebirth.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Queues;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

public class BlockFinderPD implements ProcessDivider<BlockPos> {
    private static final Iterable<Direction> DIRECTIONS = ImmutableList.of(Direction.NORTH, Direction.SOUTH,
            Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN);
    private final Queue<BlockPos> seeds;
    private final Predicate<BlockPos> target;
    private final Predicate<BlockPos> linkable;
    private final Iterable<Direction> directions;
    private final Set<BlockPos> searched;
    @Nullable
    private BlockPos result;

    public BlockFinderPD(Iterable<BlockPos> seeds, Predicate<BlockPos> target, Predicate<BlockPos> linkable,
                         Iterable<Direction> directions, int expected) {
        this.seeds = Queues.newArrayDeque(seeds);
        this.target = target;
        this.linkable = linkable;
        this.directions = ImmutableList.copyOf(directions);
        this.searched = new ObjectOpenHashSet<>(expected);
        for (BlockPos seed : seeds) {
            if (this.target.test(seed)) {
                result = seed;
                this.seeds.clear();
                return;
            }
        }
    }

    public BlockFinderPD(Iterable<BlockPos> seeds, Predicate<BlockPos> target, Predicate<BlockPos> linkable, int expected) {
        this(seeds, target, linkable, DIRECTIONS, expected);
    }

    @Override
    public boolean tick() {
        if (isEnd()) {
            return false;
        }
        var seed = seeds.poll();
        //探索済みならスキップ
        if (searched.contains(seed)) {
            return tick();
        }
        //探索済みにする
        searched.add(seed);
        //周囲のマスをシードに追加する
        for (Direction direction : directions) {
            BlockPos linkPos = seed.offset(direction);
            //探索済みならスキップ
            if (searched.contains(linkPos)) {
                continue;
            }
            //ターゲットならリターン
            if (target.test(linkPos)) {
                result = linkPos;
                seeds.clear();
                searched.clear();
                return true;
            }
            //探索可能ならシードに追加
            if (linkable.test(linkPos)) {
                seeds.add(linkPos);
            }
        }
        if (isEnd()) {
            searched.clear();
        }
        return false;
    }

    @Override
    public Optional<BlockPos> getResult() {
        return Optional.ofNullable(result);
    }

    @Override
    public boolean isEnd() {
        return seeds.isEmpty();
    }
}
