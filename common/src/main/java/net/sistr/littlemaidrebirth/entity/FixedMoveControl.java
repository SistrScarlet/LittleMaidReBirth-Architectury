package net.sistr.littlemaidrebirth.entity;

import net.minecraft.entity.ai.control.MoveControl;
import net.minecraft.entity.mob.MobEntity;

/**
 * ストレイフの後に横滑りするのを修正したクラス
 * */
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
        if (this.state == State.WAIT) {
            this.entity.setForwardSpeed(0);
            this.entity.setSidewaysSpeed(0);
            return;
        }
        super.tick();
    }
}
