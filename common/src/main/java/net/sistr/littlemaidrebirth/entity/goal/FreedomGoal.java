package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

import java.util.EnumSet;

//雇い主が居ない場合も実行される
//todo 移動範囲は大体±１０ブロックの正方形で、起点から約１７ブロック以上離れると起点に転移します。
//todo 移動範囲などをコンフィグ化
public class FreedomGoal<T extends LittleMaidEntity> extends WanderAroundFarGoal {
    private final T maid;
    private final double distance;
    private final double distanceSq;
    private BlockPos freedomPos;
    private int reCalcCool;

    public FreedomGoal(T mob, double speedIn, double distance) {
        super(mob, speedIn);
        this.maid = mob;
        this.distance = distance;
        this.distanceSq = distance * distance;
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        return !TameableUtil.isWait(maid)
                && maid.getNavigation().isIdle()
                && maid.getMovingMode() == MovingMode.FREEDOM
                && super.canStart();
    }

    @Override
    public void start() {
        super.start();
        freedomPos = this.maid.getFreedomPos().orElse(null);
    }

    @Override
    public void tick() {
        super.tick();

        //自由行動の起点が存在する場合、起点から自由行動範囲の外に行かないようにする

        if (freedomPos == null) {
            return;
        }
        if (freedomPos.getSquaredDistance(mob.getPos()) < distanceSq) {
            return;
        }
        if (0 < --reCalcCool) {
            return;
        }
        reCalcCool = getTickCount(20);
        //freedomPosを目指して移動
        Path path = mob.getNavigation().findPathTo(
                freedomPos.getX(), freedomPos.getY(), freedomPos.getZ(), MathHelper.floor(distance * 0.5));
        if (path != null && path.getEnd() != null && path.getEnd().getManhattanDistance(freedomPos) < distance) {
            mob.getNavigation().startMovingAlong(path, speed);
            return;
        }
        mob.getNavigation().stop();
        //移動しても着きそうにない場合はTP
        if (mob.isOnGround()
                && mob.getWorld().isSpaceEmpty(
                mob.getBoundingBox()
                        .offset(mob.getPos().multiply(-1))
                        .offset(freedomPos))
        ) {
            mob.teleport(freedomPos.getX() + 0.5D, freedomPos.getY(), freedomPos.getZ() + 0.5D);
        }

    }

    @Override
    public void stop() {
        super.stop();
        freedomPos = null;
        reCalcCool = 0;
    }
}
