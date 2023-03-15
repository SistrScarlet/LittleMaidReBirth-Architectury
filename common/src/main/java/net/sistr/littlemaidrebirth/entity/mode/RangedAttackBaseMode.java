package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

import java.util.Optional;
import java.util.function.Predicate;

public abstract class RangedAttackBaseMode extends Mode {
    protected final LittleMaidEntity mob;
    protected int seeTime;
    protected boolean strafingClockwise;
    protected boolean strafingBackwards;
    protected int strafingTime = -1;

    public RangedAttackBaseMode(ModeType<? extends RangedAttackBaseMode> modeType,
                                String name, LittleMaidEntity mob) {
        super(modeType, name);
        this.mob = mob;
    }

    public boolean shouldExecute() {
        return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
    }

    public boolean shouldContinueExecuting() {
        return this.shouldExecute();
    }

    public void startExecuting() {
        this.mob.setAttacking(true);
        this.mob.setAimingBow(true);
        this.mob.play(LMSounds.FIND_TARGET_N);
        this.mob.getNavigation().stop();
    }

    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        double distanceSq = this.mob.squaredDistanceTo(target.getX(), target.getY(), target.getZ());
        boolean canSee = this.mob.getVisibilityCache().canSee(target);
        ItemStack itemStack = this.mob.getMainHandStack();
        float maxRange = getMaxRange(itemStack);
        boolean prevCanSee = 0 < this.seeTime;
        //見えなくなるか、見えるようになったら
        if (canSee != prevCanSee) {
            this.seeTime = 0;
        }
        //見えなくなったら
        if (prevCanSee && !canSee) {
            this.strafingTime = 0;
            this.strafingClockwise = !this.strafingClockwise;
        }

        if (canSee) {
            ++this.seeTime;
        } else {
            --this.seeTime;
        }

        //レンジ内
        if (distanceSq < maxRange * maxRange) {
            ++this.strafingTime;
        } else {
            this.strafingTime = 0;
        }

        //1秒ごとに10%の確率で反転
        if (20 <= this.strafingTime) {
            if ((double) this.mob.getRandom().nextFloat() < 0.1D) {
                this.strafingClockwise = !this.strafingClockwise;
            }
            this.strafingTime = 0;
        }

        if (maxRange * maxRange < distanceSq) {
            this.strafingBackwards = false;
        } else if (distanceSq < maxRange * maxRange * 0.75F) {
            this.strafingBackwards = true;
        }

        this.mob.getMoveControl().strafeTo(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
        this.mob.lookAtEntity(target, 30.0F, 30.0F);

        tickRangedAttack(target, itemStack, canSee, distanceSq, maxRange);
    }

    protected abstract void tickRangedAttack(LivingEntity target, ItemStack itemStack, boolean canSee, double distanceSq, float maxRange);

    protected abstract float getMaxRange(ItemStack itemStack);

    protected Optional<EntityHitResult> raycastShootLine(LivingEntity target, float maxRange, Predicate<Entity> predicate) {
        var targetAt = target.getEyePos();
        var toTargetVec = targetAt.subtract(this.mob.getEyePos()).normalize();
        Vec3d start = this.mob.getCameraPosVec(1F);
        Vec3d end = start.add(toTargetVec.multiply(maxRange));
        Box box = new Box(start, end).expand(1D);
        var result = ProjectileUtil.getEntityCollision(mob.world, this.mob, start, end, box, predicate);
        return Optional.ofNullable(result);
    }

    public void resetTask() {
        this.mob.setAttacking(false);
        this.mob.setAimingBow(false);
        this.seeTime = 0;
    }
}
