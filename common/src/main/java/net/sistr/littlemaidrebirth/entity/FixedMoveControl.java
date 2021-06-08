package net.sistr.littlemaidrebirth.entity;

import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.ai.pathing.EntityNavigation;
import net.minecraft.entity.ai.pathing.PathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;

public class FixedMoveControl extends MoveControl {

    public FixedMoveControl(MobEntity entity) {
        super(entity);
    }

    @Override
    public void moveTo(double x, double y, double z, double speed) {
        super.moveTo(x, y, z, speed);
        this.forwardMovement = 0;
        this.sidewaysMovement = 0;
        this.entity.setForwardSpeed(0);
        this.entity.setSidewaysSpeed(0);
    }

    @Override
    public void tick() {
        if (this.state == MoveControl.State.STRAFE) {
            float attrSpeed = (float) this.entity.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
            float speed = (float) this.speed * attrSpeed;
            checkStrifeToPos(speed);

            this.entity.setMovementSpeed(speed);
            this.entity.setForwardSpeed(this.forwardMovement);
            this.entity.setSidewaysSpeed(this.sidewaysMovement);
            this.state = MoveControl.State.WAIT;
            return;
        } else if (this.state == State.WAIT) {
            this.entity.setForwardSpeed(0);
            this.entity.setSidewaysSpeed(0);
            return;
        }
        super.tick();
    }

    protected void checkStrifeToPos(float speed) {
        BlockPos strifeToPos = getStrifeToPos(speed, this.forwardMovement, this.sidewaysMovement);
        if (!this.canWalkable(strifeToPos)) {
            this.forwardMovement = 0;
            this.sidewaysMovement = 0;
            //大体の場合、strifeToPos == entityPos
            BlockPos entityPos = entity.getBlockPos();
            if (!strifeToPos.equals(entityPos) && this.canWalkable(entityPos)) {
                Vec2f strife = getPosToStrife(entityPos.getX() + 0.5F, entityPos.getZ() + 0.5F);
                this.forwardMovement = strife.x;
                this.sidewaysMovement = strife.y;
            } else {
                BlockPos.Mutable checkPos = new BlockPos.Mutable();
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        checkPos.set(entity.getX() + i, entity.getY(), entity.getZ() + j);
                        if (canWalkable(checkPos)) {
                            Vec2f strife = getPosToStrife(checkPos.getX() + 0.5F, checkPos.getZ() + 0.5F);
                            this.forwardMovement = strife.x;
                            this.sidewaysMovement = strife.y;
                        }
                    }
                }
            }
        }
    }

    protected BlockPos getStrifeToPos(float speed, float forward, float sideways) {
        float moveAmount = MathHelper.sqrt(forward * forward + sideways * sideways);
        if (moveAmount < 1.0F) {
            moveAmount = 1.0F;
        }

        moveAmount = speed / moveAmount;
        forward *= moveAmount;
        sideways *= moveAmount;
        float sinYaw = MathHelper.sin(this.entity.yaw * (float) (Math.PI / 180D));
        float cosYaw = MathHelper.cos(this.entity.yaw * (float) (Math.PI / 180D));
        float moveX = forward * cosYaw - sideways * sinYaw;
        float moveZ = sideways * cosYaw + forward * sinYaw;
        return new BlockPos(
                MathHelper.floor(this.entity.getX() + moveX),
                MathHelper.floor(this.entity.getY()),
                MathHelper.floor(this.entity.getZ() + moveZ));
    }

    protected Vec2f getPosToStrife(float x, float z) {
        float moveX = x - (float) this.entity.getX();
        float moveZ = z - (float) this.entity.getZ();
        float moveYaw = (float)(MathHelper.atan2(moveX, moveZ) * (180D / Math.PI));
        //エンティティの向いている方向を0度として、移動したい方向を調整する
        moveYaw -= entity.yaw;
        float sideways = -MathHelper.sin(moveYaw * (float) (Math.PI / 180D));
        float forward = -MathHelper.cos(moveYaw * (float) (Math.PI / 180D));
        return new Vec2f(forward, sideways);
    }

    protected boolean canWalkable(BlockPos pos) {
        return canWalkable(pos.getX(), pos.getY(), pos.getZ());
    }

    protected boolean canWalkable(int x, int y, int z) {
        EntityNavigation nav = this.entity.getNavigation();
        PathNodeMaker pathNode = nav.getNodeMaker();
        return pathNode.getDefaultNodeType(this.entity.world, x, y, z) == PathNodeType.WALKABLE;
    }
}
