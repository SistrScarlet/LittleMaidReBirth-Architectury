package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.api.mode.IRangedWeapon;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.FakePlayer;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.util.SightUtil;

import java.util.function.Predicate;

public class ArcherMode extends Mode {
    private final LittleMaidEntity mob;
    private final float inaccuracy;
    private final Predicate<Entity> friend;
    private int seeTime;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;
    private int reUseCool;

    public ArcherMode(ModeType<? extends ArcherMode> modeType, String name, LittleMaidEntity mob, float inaccuracy, Predicate<Entity> friend) {
        super(modeType, name);
        this.mob = mob;
        this.inaccuracy = inaccuracy;
        this.friend = friend;
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
    }

    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        double distanceSq = this.mob.squaredDistanceTo(target.getX(), target.getY(), target.getZ());
        boolean canSee = this.mob.getVisibilityCache().canSee(target);
        ItemStack itemStack = this.mob.getMainHandStack();
        Item item = itemStack.getItem();
        float maxRange = (item instanceof IRangedWeapon
                ? ((IRangedWeapon) item).getMaxRange_LMRB(itemStack, this.mob)
                : 16F) * LMRBMod.getConfig().getArcherRangeFactor();
        Vec3d start = this.mob.getCameraPosVec(1F);
        Vec3d end = start.add(this.mob.getRotationVec(1F)
                .multiply(maxRange));
        Box box = new Box(start, end).expand(1D);
        EntityHitResult clear = ProjectileUtil.getEntityCollision(mob.world, this.mob, start, end, box, friend);
        canSee = canSee && clear == null;
        boolean prevCanSee = 0 < this.seeTime;
        //見えなくなるか、見えるようになったら
        if (canSee != prevCanSee) {
            this.seeTime = 0;
        }
        //見えなくなったら
        if (prevCanSee && !canSee) {
            this.strafingClockwise = !this.strafingClockwise;
        }

        if (canSee) {
            ++this.seeTime;
        } else {
            --this.seeTime;
        }

        //レンジ内
        if (distanceSq < maxRange * maxRange) {
            this.mob.getNavigation().stop();
            ++this.strafingTime;
        } else {
            this.strafingTime = -1;
        }

        //レンジ内かつ、視認が20tick以上
        if (20 <= this.strafingTime) {

            if ((double) this.mob.getRandom().nextFloat() < 0.1D) {
                this.strafingClockwise = !this.strafingClockwise;
            }

            this.strafingTime = 0;
        }

        if (maxRange < distanceSq) {
            this.strafingBackwards = false;
        } else if (distanceSq < maxRange * 0.75F) {
            this.strafingBackwards = true;
        }

        this.mob.getMoveControl().strafeTo(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
        this.mob.lookAtEntity(target, 30.0F, 30.0F);

        FakePlayer fakePlayer = this.mob.getFakePlayer();
        Vec3d vec3d = fakePlayer.getCameraPosVec(1F);
        Vec3d targetPos = target.getCameraPosVec(1F);
        double xD = targetPos.x - vec3d.x;
        double yD = targetPos.y - vec3d.y;
        double zD = targetPos.z - vec3d.z;
        double hDist = MathHelper.sqrt((float) (xD * xD + zD * zD));
        float pitch = (float) (-(MathHelper.atan2(yD, hDist) * (180D / Math.PI)));
        pitch += ((this.mob.getRandom().nextFloat() * 2 - 1) * (this.mob.getRandom().nextFloat() * 2 - 1)) * inaccuracy;
        pitch = MathHelper.wrapDegrees(pitch);
        float yaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(zD, xD) * (180D / Math.PI)) - 90.0F);
        fakePlayer.headYaw = yaw;
        fakePlayer.setYaw(yaw);
        fakePlayer.setPitch(pitch);

        //FPがアイテムを構えていないとき
        if (--reUseCool < 0 && !fakePlayer.isUsingItem()) {
            reUseCool = 4;
            //見えているか、見えてない時間が60tick以内
            if (-60 <= this.seeTime) {
                this.mob.setAimingBow(true);

                mob.play(LMSounds.SIGHTING);

                ItemStack stack = fakePlayer.getMainHandStack();
                stack.use(mob.world, fakePlayer, Hand.MAIN_HAND);
            }
            return;
        }

        //見えないなら
        if (!canSee) {
            if (this.seeTime < -60) {
                this.mob.setAimingBow(false);

                fakePlayer.clearActiveItem();
            }
            return;
        }

        //十分に引き絞ったか
        int useCount = fakePlayer.getItemUseTime();
        int interval = (int) ((item instanceof IRangedWeapon
                ? ((IRangedWeapon) item).getInterval_LMRB(itemStack, this.mob)
                : 25) * LMRBMod.getConfig().getArcherPullLengthFactor());
        if (interval <= useCount) {
            //射線チェック、射線に味方が居る場合は撃たない
            //todo 要検証
            var inSightEntities = SightUtil.getInSightEntities(this.mob.world, this.mob,
                    this.mob.getCameraPosVec(1f),
                    this.mob.getRotationVec(1f),
                    maxRange * 2, 2.5f, 2.5f, 2.0f,
                    e -> e instanceof LivingEntity && this.mob.isFriend((LivingEntity) e));
            if (!inSightEntities.isEmpty()) {
                return;
            }

            fakePlayer.stopUsingItem();

            mob.play(LMSounds.SHOOT);
        }

    }

    public void resetTask() {
        this.mob.setAttacking(false);
        this.seeTime = 0;
        this.mob.setAimingBow(false);

        this.mob.getNavigation().stop();

        FakePlayer fakePlayer = this.mob.getFakePlayer();
        fakePlayer.clearActiveItem();
    }

}
