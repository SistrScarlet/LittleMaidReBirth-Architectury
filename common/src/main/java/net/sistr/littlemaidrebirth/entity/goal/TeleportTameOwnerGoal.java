package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.sistr.littlemaidrebirth.entity.util.Tameable;

public class TeleportTameOwnerGoal<T extends PathAwareEntity & Tameable> extends Goal {
    protected final T tameable;
    protected final World world;
    protected final float teleportStartSq;
    private final EntityNavigation navigation;
    private LivingEntity owner;
    private int updateCountdownTicks;

    public TeleportTameOwnerGoal(T tameable, float teleportStart) {
        this.tameable = tameable;
        this.world = tameable.world;
        this.teleportStartSq = teleportStart * teleportStart;
        this.navigation = tameable.getNavigation();
    }

    @Override
    public boolean canStart() {
        LivingEntity tameOwner = this.tameable.getTameOwner().orElse(null);
        if (tameOwner == null) {
            return false;
        } else if (tameOwner.isSpectator()) {
            return false;
        } else if (this.tameable.isWait()) {
            return false;
        } else if (this.tameable.squaredDistanceTo(tameOwner) < teleportStartSq) {
            return false;
        } else {
            this.owner = tameOwner;
            return true;
        }
    }

    public boolean shouldContinue() {
        if (this.tameable.isWait()) {
            return false;
        } else {
            return teleportStartSq < this.tameable.squaredDistanceTo(this.owner);
        }
    }

    @Override
    public void start() {
        this.updateCountdownTicks = 0;
    }

    @Override
    public void stop() {
        this.owner = null;
        this.navigation.stop();
    }

    @Override
    public void tick() {
        this.tameable.getLookControl().lookAt(this.owner, 10.0f, this.tameable.getMaxLookPitchChange());
        if (--this.updateCountdownTicks > 0) {
            return;
        }
        this.updateCountdownTicks = 10;
        tryTeleport();
    }

    private void tryTeleport() {
        BlockPos ownerPos = this.owner.getBlockPos();
        for (int i = 0; i < 10; ++i) {
            int x = this.getRandomInt(-3, 3);
            int y = this.getRandomInt(-1, 1);
            int z = this.getRandomInt(-3, 3);
            boolean bl = this.tryTeleportTo(ownerPos.getX() + x, ownerPos.getY() + y, ownerPos.getZ() + z);
            if (!bl) continue;
            return;
        }
    }

    private boolean tryTeleportTo(int x, int y, int z) {
        if (isOwnerRange(this.owner, x, y, z)) {
            return false;
        }
        if (Math.abs((double) x - this.owner.getX()) < 2.0 && Math.abs((double) z - this.owner.getZ()) < 2.0) {
            return false;
        }
        if (!this.canTeleportTo(new BlockPos(x, y, z))) {
            return false;
        }
        this.tameable.refreshPositionAndAngles((double) x + 0.5, y, (double) z + 0.5, this.tameable.getYaw(), this.tameable.getPitch());
        this.navigation.stop();
        return true;
    }

    private boolean isOwnerRange(Entity owner, int x, int y, int z) {
        final Vec3d ownerPos = owner.getPos();
        final Vec3d entityPos = new Vec3d(x + 0.5, y, z + 0.5).subtract(ownerPos);
        final Vec3d ownerRot = owner.getRotationVec(1F).multiply(4);
        final double dot = entityPos.dotProduct(ownerRot);
        final double range = 4;
        //プレイヤー位置を原点としたアイテムの位置と、プレイヤーの向きの内積がプラス
        //かつ内積の大きさが4m以下
        return 0 < dot && dot < range * range;
    }

    private boolean canTeleportTo(BlockPos pos) {
        PathNodeType pathNodeType = LandPathNodeMaker.getLandNodeType(this.world, pos.mutableCopy());
        if (pathNodeType != PathNodeType.WALKABLE) {
            return false;
        }
        BlockPos blockPos = pos.subtract(this.tameable.getBlockPos());
        return this.world.isSpaceEmpty(this.tameable, this.tameable.getBoundingBox().offset(blockPos));
    }

    private int getRandomInt(int min, int max) {
        return this.tameable.getRandom().nextInt(max - min + 1) + min;
    }

}
