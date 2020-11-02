package net.sistr.lmrb.entity.goal;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;
import net.sistr.lmrb.entity.Tameable;

import java.util.EnumSet;

public class EscortGoal extends Goal {
    private final PathAwareEntity escort;
    private final Tameable tameable;
    private final EntityNavigation navigator;
    private final float minDistanceSq;
    private final float maxDistanceSq;
    private final float teleportDistanceSq;
    private final double speed;

    private int timeToRecalcPath;
    private float oldWaterCost;
    private Entity owner;

    public EscortGoal(PathAwareEntity escort, Tameable tameable, float minDistance, float maxDistance, float teleportDistance, double speed) {
        this.escort = escort;
        this.tameable = tameable;
        this.navigator = escort.getNavigation();
        this.minDistanceSq = maxDistance * maxDistance;
        this.maxDistanceSq = minDistance * minDistance;
        this.teleportDistanceSq = teleportDistance * teleportDistance;
        this.speed = speed;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (!this.tameable.getMovingState().equals(Tameable.ESCORT)) {
            return false;
        }
        LivingEntity owner = this.tameable.getTameOwner().orElse(null);
        if (owner == null) {
            return false;
        }
        if (owner.isSpectator()) {
            return false;
        }
        if (owner.squaredDistanceTo(this.escort) < this.maxDistanceSq) {
            return false;
        }
        this.owner = owner;
        return true;
    }

    @Override
    public boolean shouldContinue() {
        if (this.navigator.isIdle()) {
            return false;
        }
        if (!this.tameable.getMovingState().equals(Tameable.ESCORT)) {
            return false;
        }
        return this.minDistanceSq < owner.squaredDistanceTo(this.escort);
    }

    @Override
    public void start() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.escort.getPathfindingPenalty(PathNodeType.WATER);
        this.escort.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
    }

    public void tick() {
        this.escort.getLookControl().lookAt(this.owner, 10.0F, (float) this.escort.getLookPitchSpeed());
        if (--this.timeToRecalcPath <= 0) {
            this.timeToRecalcPath = 10;
            if (!this.escort.isLeashed() && !this.escort.hasVehicle()) {
                double distanceSq = this.escort.squaredDistanceTo(this.owner);
                if (teleportDistanceSq < distanceSq) {
                    this.tryTeleport();
                } else {
                    this.navigator.startMovingTo(this.owner, speed);
                }

            }
        }
    }

    private void tryTeleport() {
        BlockPos blockpos = this.owner.getBlockPos();

        for (int i = 0; i < 10; ++i) {
            int x = this.getRandomInt(-3, 3);
            int y = this.getRandomInt(-1, 1);
            int z = this.getRandomInt(-3, 3);
            if (this.tryTeleport(blockpos.getX() + x, blockpos.getY() + y, blockpos.getZ() + z)) {
                return;
            }
        }

    }

    private boolean tryTeleport(int x, int y, int z) {
        if (Math.abs(x - this.owner.getX()) < 2 && Math.abs(z - this.owner.getZ()) < 2) {
            return false;
        }
        if (!this.canTeleport(new BlockPos(x, y, z))) {
            return false;
        }
        this.escort.updatePositionAndAngles(x + 0.5, y, z + 0.5, this.escort.yaw, this.escort.pitch);
        this.navigator.stop();
        return true;
    }

    private boolean canTeleport(BlockPos pos) {
        PathNodeType type = LandPathNodeMaker.getLandNodeType(this.escort.world, pos.mutableCopy());
        if (type != PathNodeType.WALKABLE) {
            return false;
        }
        BlockState blockstate = this.escort.world.getBlockState(pos.down());
        if (blockstate.getBlock() instanceof LeavesBlock) {
            return false;
        }
        BlockPos blockpos = pos.subtract(this.escort.getBlockPos());
        return this.escort.world.isSpaceEmpty(this.escort, this.escort.getBoundingBox().offset(blockpos));
    }

    private int getRandomInt(int min, int max) {
        return this.escort.getRandom().nextInt(max - min + 1) + min;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.navigator.stop();
        this.escort.setPathfindingPenalty(PathNodeType.WATER, this.oldWaterCost);
    }

}
