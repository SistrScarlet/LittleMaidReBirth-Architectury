package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.stream.Stream;

public class RedstoneTraceGoal extends Goal {
    protected final LittleMaidEntity mob;
    protected final float speed = 0.8f;

    public RedstoneTraceGoal(LittleMaidEntity mob) {
        this.mob = mob;
        this.setControls(EnumSet.of(Control.LOOK, Control.MOVE));
    }

    @Override
    public boolean canStart() {
        return mob.getMainHandStack().getItem() == Items.REDSTONE_TORCH;
    }

    @Override
    public void start() {

    }

    @Override
    public void tick() {
        /*getAroundSignalPoses()
                .forEach(pos -> {
                    float score = getScore(pos);
                    ((ServerWorld) this.mob.world).spawnParticles(
                            new DustParticleEffect(new Vec3f(score, 0, 1 - score), 1.0f),
                            pos.getX() + 0.5f,
                            pos.getY() + 1f,
                            pos.getZ() + 0.5f,
                            1, 0, 0, 0, 0);
                });*/
        var navigation = this.mob.getNavigation();
        if (!navigation.isIdle()) {
            return;
        }
        var mobPos = this.mob.getBlockPos();
        getAroundSignalPoses()
                //現在位置にあるposは除外する。ただし高度は無視
                .filter(pos -> pos.getX() != mobPos.getX() || pos.getZ() != mobPos.getZ())
                .min(Comparator.comparingDouble(pos ->
                        //左55度を0として時計回りに一周回し、角度が浅いposを取る
                        //あと高度が高い位置を優先して取る
                        -MathHelper.subtractAngles(getRelYaw(pos), 55f) + 180f - pos.getY()))
                .ifPresent(pos -> navigation.startMovingAlong(navigation.findPathTo(pos, 0), this.speed));
    }

    protected float getRelYaw(BlockPos pos) {
        float x = (float) (pos.getX() + 0.5f - this.mob.getX());
        float z = (float) (pos.getZ() + 0.5f - this.mob.getZ());
        float yaw = (float) (-MathHelper.atan2(x, z) * (180 / Math.PI));
        float mobYaw = this.mob.getYaw();
        return MathHelper.subtractAngles(mobYaw, yaw);
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

    protected int getEmitPower(BlockPos pos) {
        var state = mob.world.getBlockState(pos);
        return Arrays.stream(Direction.values())
                .mapToInt(direction -> state.getStrongRedstonePower(this.mob.world, pos, direction))
                .max()
                .orElse(0);
    }

}
