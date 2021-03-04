package net.sistr.lmrb.entity.goal;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.*;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;
import net.sistr.lmrb.entity.Tameable;

import java.util.EnumSet;

public class EscortGoal<T extends PathAwareEntity & Tameable> extends Goal {
    private final T tameable;
    private final WorldView world;
    private final double speed;
    private final EntityNavigation navigation;
    private final float minDistance;
    private final float maxDistance;
    private final float tpDistance;
    private float oldWaterPathfindingPenalty;
    private final boolean leavesAllowed;
    private int updateCountdownTicks;
    private LivingEntity owner;

    public EscortGoal(T tameable, double speed, float minDistance, float maxDistance, float tpDistance, boolean leavesAllowed) {
        this.tameable = tameable;
        this.world = tameable.world;
        this.speed = speed;
        this.navigation = tameable.getNavigation();
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.tpDistance = tpDistance;
        this.leavesAllowed = leavesAllowed;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        if (!(tameable.getNavigation() instanceof MobNavigation) && !(tameable.getNavigation() instanceof BirdNavigation)) {
            throw new IllegalArgumentException("Unsupported mob type for FollowOwnerGoal");
        }
    }

    public boolean canStart() {
        LivingEntity livingEntity = this.tameable.getTameOwner().orElse(null);
        if (livingEntity == null) {
            return false;
        } else if (livingEntity.isSpectator()) {
            return false;
        } else if (this.tameable.getMovingState() != Tameable.MovingState.ESCORT) {
            return false;
        } else if (this.tameable.squaredDistanceTo(livingEntity) < this.maxDistance * this.maxDistance) {
            return false;
        } else {
            this.owner = livingEntity;
            return true;
        }
    }

    public boolean shouldContinue() {
        if (this.navigation.isIdle()) {
            return false;
        } else if (this.tameable.getMovingState() != Tameable.MovingState.ESCORT) {
            return false;
        } else {
            return this.minDistance * this.minDistance < this.tameable.squaredDistanceTo(this.owner);
        }
    }

    public void start() {
        this.updateCountdownTicks = 0;
        this.oldWaterPathfindingPenalty = this.tameable.getPathfindingPenalty(PathNodeType.WATER);
        this.tameable.setPathfindingPenalty(PathNodeType.WATER, 0.0F);
    }

    public void tick() {
        this.tameable.getLookControl().lookAt(this.owner, 10.0F, this.tameable.getLookPitchSpeed());
        if (--this.updateCountdownTicks <= 0) {
            this.updateCountdownTicks = 10;
            if (!this.tameable.isLeashed() && !this.tameable.hasVehicle()) {
                if (tpDistance * tpDistance <= this.tameable.squaredDistanceTo(this.owner)) {
                    this.tryTeleport();
                } else {
                    this.navigation.startMovingTo(this.owner, this.speed);
                }

            }
        }
    }

    private void tryTeleport() {
        BlockPos blockPos = this.owner.getBlockPos();

        for (int i = 0; i < 10; ++i) {
            int x = this.getRandomInt(-3, 3);
            int y = this.getRandomInt(-1, 1);
            int z = this.getRandomInt(-3, 3);
            boolean teleported = this.tryTeleportTo(blockPos.getX() + x, blockPos.getY() + y, blockPos.getZ() + z);
            if (teleported) {
                return;
            }
        }

    }

    private int getRandomInt(int min, int max) {
        return this.tameable.getRandom().nextInt(max - min + 1) + min;
    }

    private boolean tryTeleportTo(int x, int y, int z) {
        if (Math.abs(x - this.owner.getX()) < 2.0D && Math.abs(z - this.owner.getZ()) < 2.0D) {
            return false;
        } else if (!this.canTeleportTo(new BlockPos(x, y, z))) {
            return false;
        } else {
            this.tameable.refreshPositionAndAngles(x + 0.5D, y, z + 0.5D, this.tameable.yaw, this.tameable.pitch);
            this.navigation.stop();
            return true;
        }
    }

    private boolean canTeleportTo(BlockPos pos) {
        PathNodeType pathNodeType = LandPathNodeMaker.getLandNodeType(this.world, pos.mutableCopy());
        if (pathNodeType != PathNodeType.WALKABLE) {
            return false;
        } else {
            BlockState blockState = this.world.getBlockState(pos.down());
            if (!this.leavesAllowed && blockState.getBlock() instanceof LeavesBlock) {
                return false;
            } else {
                BlockPos blockPos = pos.subtract(this.tameable.getBlockPos());
                return this.world.isSpaceEmpty(this.tameable, this.tameable.getBoundingBox().offset(blockPos));
            }
        }
    }

    public void stop() {
        this.owner = null;
        this.navigation.stop();
        this.tameable.setPathfindingPenalty(PathNodeType.WATER, this.oldWaterPathfindingPenalty);
    }

}
