package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;

public abstract class AbstractFencerMode<T> extends AbstractBattleMode<T> {
    protected int maxRecalcPathCool = LMRBMod.getConfig().movement.pathRecalcInterval;
    protected int recalcPathCool = 0;
    protected float speed;

    protected AbstractFencerMode(
            MobEntity mob, ModeType<? extends Mode> modeType, String name, float speed) {
        super(mob, modeType, name);
        this.speed = speed;
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
    }

    @Override
    public void tick() {
        tickLookFor();
        recalcPathCool = Math.max(0, recalcPathCool - 1);

        // 距離が遠かったら接近
        if (!isClose(getBoundingDistanceSq(this.target))) {
            if (recalcPathCool > 0) return;
            recalcPathCool = maxRecalcPathCool;
            tickToMove();
        } else {
            preTryAttackTick();
            // 距離が近かったら攻撃
            if (canAttack()) {
                attack();
            }
        }
    }

    protected void tickLookFor() {
        this.mob.getLookControl().lookAt(target, 30.0F, 30.0F);
    }

    protected void tickToMove() {
        var nav = this.mob.getNavigation();

        nav.startMovingTo(this.target, this.speed);
    }

    protected void preTryAttackTick() {
        this.mob.getNavigation().stop();
    }

    protected boolean canAttack() {
        return this.mob.getVisibilityCache().canSee(this.target);
    }

    protected abstract void attack();

    protected abstract boolean isClose(double distanceSq);

    protected double getBoundingDistanceSq(Entity target) {
        double distance =
                this.mob.distanceTo(target) - (this.mob.getWidth() + target.getWidth()) / 2;
        return distance * distance;
    }

    @Override
    public BattleModeType getBattleModeType() {
        return BattleModeType.SWORD;
    }
}
