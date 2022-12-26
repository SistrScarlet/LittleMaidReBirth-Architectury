package net.sistr.littlemaidrebirth.util;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * ブロック探索ロジック
 * */
public class BlockFinder {

    public static void seedFill(BlockPos seed, final int maxCount,
                                Predicate<BlockPos> fillable, Consumer<BlockPos> filler,
                                EnumSet<Direction> directions) {
        Queue<BlockPos> seeds = Queues.newArrayDeque();
        seeds.add(seed);

        int count = 0;
        while (!seeds.isEmpty()) {
            if (maxCount < count++) {
                break;
            }
            seed = seeds.poll();
            //塗りつぶせない場合break
            if (!fillable.test(seed)) {
                break;
            }
            //塗りつぶす
            filler.accept(seed);
            //周囲のマスをシードに追加する
            for (Direction direction : directions) {
                //塗りつぶせるならシードに追加
                if (fillable.test(seed)) {
                    seeds.add(seed.offset(direction));
                }
            }
        }

    }

    //多分動かん
    public static Optional<BlockPos> findTarget(BlockPos centerPos, int range,
                                                Predicate<BlockPos> target, Predicate<BlockPos> linkable) {
        //hashsetの生成コスト的にclearの方が良い
        Set<BlockPos> checked = Sets.newHashSet(centerPos);
        Set<BlockPos> nowChecked = Sets.newHashSet();
        for (int i = 0; i < range; i++) {
            for (BlockPos checkedPos : checked) {
                for (Direction direction : Direction.values()) {
                    int num = checkedPos.getComponentAlongAxis(direction.getAxis())
                            - centerPos.getComponentAlongAxis(direction.getAxis());
                    //0の場合は両方向にやる
                    if (0 <= num) {
                        BlockPos checkPos = checkedPos.offset(direction);
                        if (target.test(checkPos))
                            return Optional.of(checkPos);
                        if (linkable.test(checkPos))
                            nowChecked.add(checkPos);
                    }
                    if (num <= 0) {
                        BlockPos checkPos = checkedPos.offset(direction.getOpposite());
                        if (target.test(checkPos))
                            return Optional.of(checkPos);
                        if (linkable.test(checkPos))
                            nowChecked.add(checkPos);
                    }

                }
            }
            checked.clear();
            checked.addAll(nowChecked);
            nowChecked.clear();
        }
        return Optional.empty();
    }

    public static Optional<BlockPos> findHorizonPos(World world, BlockPos centerPos, int layer, int horizon, Predicate<BlockPos> target) {
        //垂直方向に5ブロック調査
        for (int l = 0; l < layer; l++) {
            BlockPos checkPos = centerPos.offset(layer % 2 == 0 ? Direction.UP : Direction.DOWN, layer >> 1);
            Optional<BlockPos> optional = findLayer(world, checkPos, horizon, target);
            if (optional.isPresent()) {
                return optional;
            }
        }
        return Optional.empty();
    }

    public static Optional<BlockPos> findLayer(World world, BlockPos centerPos, int horizon, Predicate<BlockPos> target) {
        Set<BlockPos> prevSearched = Sets.newHashSet(centerPos);
        Set<BlockPos> allSearched = Sets.newHashSet();
        //水平方向に16ブロック調査
        for (int k = 0; k < horizon; k++) {
            Optional<BlockPos> optional = findHorizon(world, prevSearched, allSearched, target);
            if (optional.isPresent()) {
                return optional;
            }
        }
        return Optional.empty();
    }

    public static Optional<BlockPos> findHorizon(World world, Set<BlockPos> prevSearched, Set<BlockPos> allSearched,
                                                 Predicate<BlockPos> target) {
        Set<BlockPos> nowSearched = Sets.newHashSet();
        //前回調査地点を起点にする
        for (BlockPos pos : prevSearched) {
            //起点に隣接する水平四ブロックを調査
            for (int i = 0; i < 4; i++) {
                Direction d = Direction.fromHorizontal(i);
                BlockPos checkPos = pos.offset(d);
                //既に調査済みの地点は除外
                if (allSearched.contains(checkPos)) {
                    continue;
                }
                //使用不能なかまどは除外し、次回検索時の起点に加える
                if (!target.test(checkPos)) {
                    nowSearched.add(checkPos);
                    continue;
                }
                //六面埋まったブロックは除外し、これを起点とした調査も打ち切る
                if (!isTouchAir(world, checkPos)) {
                    allSearched.add(checkPos);
                    nowSearched.remove(checkPos);
                    continue;
                }
                //除外されなければ値を返す
                return Optional.of(checkPos);
            }
        }
        //次回調査用
        allSearched.addAll(nowSearched);
        prevSearched.clear();
        prevSearched.addAll(nowSearched);
        return Optional.empty();
    }

    public static boolean isTouchAir(World world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            if (isAir(world, pos, dir)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAir(World world, BlockPos pos, Direction dir) {
        return world.isAir(pos.offset(dir));
    }

    /**
     * 特定ブロックを探索する。
     * 近い地点から探索していく。
     * 微妙かもしらん…
     *
     * @param seed       開始地点
     * @param target     探索対象
     * @param linkable   探索可能ブロック
     * @param directions 探索方向
     * @param maxCount   最大探索回数
     */
    public static Optional<BlockPos> searchTargetBlock(BlockPos seed,
                                                       Predicate<BlockPos> target, Predicate<BlockPos> linkable,
                                                       Collection<Direction> directions, int maxCount) {
        //ターゲットならリターン
        if (target.test(seed)) {
            return Optional.of(seed);
        }
        //このキューにはシードを入れる。シードは重複する場合がある
        Queue<BlockPos> seeds = Queues.newArrayDeque();

        seeds.add(seed);
        //探索した座標を記録する
        Set<BlockPos> searched = new ObjectOpenHashSet<>(maxCount);

        int count = 0;
        //シードが空っぽになるか、カウント以下ならループ
        while (!seeds.isEmpty() && count++ < maxCount) {
            seed = seeds.poll();
            //探索済みならスキップ
            if (searched.contains(seed)) {
                continue;
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
                    return Optional.of(linkPos);
                }
                //探索可能ならシードに追加
                if (linkable.test(linkPos)) {
                    seeds.add(linkPos);
                }
            }
        }
        return Optional.empty();
    }

}
