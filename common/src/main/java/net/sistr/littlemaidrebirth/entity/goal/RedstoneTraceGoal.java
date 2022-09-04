package net.sistr.littlemaidrebirth.entity.goal;

import com.google.common.collect.Lists;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.MovingMode;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

//todo 180度ターン時に首がグリッとなるのがこわい
public class RedstoneTraceGoal extends Goal {
    protected final LittleMaidEntity mob;
    protected final float speed;
    protected final List<BlockPos> aroundSignalPos = Lists.newArrayList();
    protected int recalcTimer;

    public RedstoneTraceGoal(LittleMaidEntity mob, float speed) {
        this.mob = mob;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.LOOK, Control.MOVE));
    }

    @Override
    public boolean canStart() {
        //実行に失敗した場合、遅延される
        if (0 < recalcTimer) {
            recalcTimer--;
            return false;
        }
        if (mob.isWait()
                || mob.getMovingMode() != MovingMode.TRACER
                || !this.mob.getNavigation().isIdle()) {
            return false;
        }
        this.aroundSignalPos.clear();
        getAroundSignalPoses()
                //現在位置にあるposは除外する。ただし高度は無視
                //getBlockPos()で判定してもいいが、実装的に動作しない場合があり得るので安全のためこちらに
                .filter(pos -> MathHelper.floor(this.mob.getX()) != pos.getX()
                        || MathHelper.floor(this.mob.getZ()) != pos.getZ())
                .forEach(this.aroundSignalPos::add);
        //このタイマーは実行完了時にリセットされる
        //そのため、連続実行時は遅延無し
        recalcTimer = 20;
        return !this.aroundSignalPos.isEmpty();
    }

    @Override
    public boolean shouldContinue() {
        return !mob.isWait()
                && mob.getMovingMode() == MovingMode.TRACER
                && !this.mob.getNavigation().isIdle();
    }

    @Override
    public void start() {
        this.aroundSignalPos
                .stream()
                .min(Comparator.comparingDouble(pos ->
                        //左55度を0として時計回りに一周回し、角度が浅いposを取る
                        //あと高度が高い位置を優先して取る
                        -MathHelper.subtractAngles(getRelYaw(pos), 55f) + 180f - pos.getY()))
                .ifPresent(pos -> {
                    var navigation = this.mob.getNavigation();
                    if (!navigation.startMovingAlong(navigation.findPathTo(pos, 0), this.speed)) {
                        navigation.stop();
                    }
                });
    }

    @Override
    public void stop() {
        this.recalcTimer = 0;
    }

    protected Stream<BlockPos> getAroundSignalPoses() {
        return BlockPos.stream(
                        this.mob.getBlockPos().add(4, 2, 4),
                        this.mob.getBlockPos().add(-4, -2, -4))
                .map(BlockPos::toImmutable)
                .filter(this::isEmitSignal);
    }

    protected boolean isEmitSignal(BlockPos pos) {
        var state = mob.world.getBlockState(pos);
        return Arrays.stream(Direction.values())
                .anyMatch(direction -> 0 < state.getStrongRedstonePower(this.mob.world, pos, direction));
    }

    protected float getRelYaw(BlockPos pos) {
        float x = (float) (pos.getX() + 0.5f - this.mob.getX());
        float z = (float) (pos.getZ() + 0.5f - this.mob.getZ());
        float yaw = (float) (-MathHelper.atan2(x, z) * (180 / Math.PI));
        float mobYaw = this.mob.getYaw();
        return MathHelper.subtractAngles(mobYaw, yaw);
    }

}
