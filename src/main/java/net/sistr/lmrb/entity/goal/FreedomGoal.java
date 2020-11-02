package net.sistr.lmrb.entity.goal;

import net.minecraft.entity.ai.TargetFinder;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.sistr.lmrb.entity.Tameable;

import java.util.EnumSet;

//雇い主が居ない場合も発動する
public class FreedomGoal extends WanderAroundFarGoal {
    private BlockPos centerPos;
    private final Tameable tameable;
    private final double distanceSq;

    public FreedomGoal(PathAwareEntity creature, Tameable tameable, double speedIn, double distance) {
        super(creature, speedIn);
        this.tameable = tameable;
        this.distanceSq = distance * distance;
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        centerPos = null;
        if (tameable.getTameOwnerUuid().isPresent()) {
            if (!tameable.getMovingState().equals(Tameable.FREEDOM)) {
                return false;
            }
            centerPos = tameable.getFollowPos().orElse(null);
            if (centerPos == null) centerPos = this.mob.getBlockPos();
        }

        return super.canStart();
    }

    @Override
    public void tick() {
        super.tick();
        if (centerPos == null) {
            return;
        }
        if (centerPos.getSquaredDistance(mob.getPos(), true) < distanceSq) {
            return;
        }
        mob.getNavigation().stop();
        Vec3d pos = TargetFinder.findTargetTowards(mob, 5, 5,
                new Vec3d(centerPos.getX(), centerPos.getY(), centerPos.getZ()));
        if (pos != null) {
            mob.getNavigation().startMovingTo(centerPos.getX(), centerPos.getY(), centerPos.getZ(), speed);
            return;
        }
        if (mob.world.isSpaceEmpty(mob.getBoundingBox().offset(mob.getPos().multiply(-1)).offset(centerPos))) {
            mob.teleport(centerPos.getX() + 0.5D, centerPos.getY(), centerPos.getZ() + 0.5D);
        }

    }

    @Override
    public void stop() {
        super.stop();
        centerPos = null;
    }
}
