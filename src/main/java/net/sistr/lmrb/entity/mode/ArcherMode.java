package net.sistr.lmrb.entity.mode;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.resource.util.LMSounds;
import net.sistr.lmrb.entity.FakePlayer;
import net.sistr.lmrb.entity.FakePlayerSupplier;
import net.sistr.lmrb.entity.AimingPoseable;
import net.sistr.lmrb.entity.InventorySupplier;
import net.sistr.lmrb.util.ModeManager;

//todo クロスボウとかも撃てるように調整
public class ArcherMode implements Mode {
    private final PathAwareEntity mob;
    private final AimingPoseable archer;
    private final FakePlayerSupplier fakePlayer;
    private final double moveSpeedAmp;
    private int attackCooldown;
    private final float maxAttackDistance;
    private int seeTime;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public ArcherMode(PathAwareEntity mob, AimingPoseable archer, FakePlayerSupplier fakePlayer,
                      double moveSpeedAmpIn, int attackCooldownIn, float maxAttackDistanceIn) {
        this.mob = mob;
        this.archer = archer;
        this.fakePlayer = fakePlayer;
        this.moveSpeedAmp = moveSpeedAmpIn;
        this.attackCooldown = attackCooldownIn;
        this.maxAttackDistance = maxAttackDistanceIn * maxAttackDistanceIn;
    }

    @Override
    public void startModeTask() {

    }

    public boolean shouldExecute() {
        return this.mob.getTarget() != null && this.mob.getTarget().isAlive()
                && (!(mob.getMainHandStack().getItem() instanceof RangedWeaponItem) || heldAmmo());
    }

    public boolean heldAmmo() {
        ItemStack weaponStack = mob.getMainHandStack();
        Item weapon = weaponStack.getItem();
        if (!(weapon instanceof RangedWeaponItem)) {
            return false;
        }
        ItemStack ammo = fakePlayer.getFakePlayer().getArrowType(weaponStack);
        return !ammo.isEmpty();
    }

    public boolean shouldContinueExecuting() {
        return this.shouldExecute();
    }

    public void startExecuting() {
        this.mob.setAttacking(true);
        this.archer.setAimingBow(true);
    }

    public void tick() {
        LivingEntity target = this.mob.getTarget();
        if (target == null) {
            return;
        }
        double distanceSq = this.mob.squaredDistanceTo(target.getX(), target.getY(), target.getZ());
        boolean canSee = this.mob.getVisibilityCache().canSee(target);
        boolean hasSeeTime = 0 < this.seeTime;
        if (canSee != hasSeeTime) {
            this.seeTime = 0;
        }

        if (canSee) {
            ++this.seeTime;
        } else {
            --this.seeTime;
        }

        if (distanceSq < this.maxAttackDistance && 20 <= this.seeTime) {
            this.mob.getNavigation().stop();
            ++this.strafingTime;
        } else {
            this.mob.getNavigation().startMovingTo(target, this.moveSpeedAmp);
            this.strafingTime = -1;
        }

        if (20 <= this.strafingTime) {
            if ((double) this.mob.getRandom().nextFloat() < 0.3D) {
                this.strafingClockwise = !this.strafingClockwise;
            }

            if ((double) this.mob.getRandom().nextFloat() < 0.3D) {
                this.strafingBackwards = !this.strafingBackwards;
            }

            this.strafingTime = 0;
        }

        if (this.strafingTime < 0) {
            this.mob.getLookControl().lookAt(target, 30.0F, 30.0F);
        } else {
            if (distanceSq > (double) (this.maxAttackDistance * 0.75F)) {
                this.strafingBackwards = false;
            } else if (distanceSq < (double) (this.maxAttackDistance * 0.25F)) {
                this.strafingBackwards = true;
            }

            this.mob.getMoveControl().strafeTo(this.strafingBackwards ? -0.5F : 0.5F, this.strafingClockwise ? 0.5F : -0.5F);
            this.mob.lookAtEntity(target, 30.0F, 30.0F);
        }

        FakePlayer fakePlayer = this.fakePlayer.getFakePlayer();
        if (!fakePlayer.isUsingItem()) {
            if (this.seeTime >= -60) {
                this.archer.setAimingBow(true);

                if (this.mob instanceof SoundPlayable) {
                    ((SoundPlayable)mob).play(LMSounds.SIGHTING);
                }

                ItemStack stack = fakePlayer.getMainHandStack();
                stack.use(mob.world, fakePlayer, Hand.MAIN_HAND);
            }
            return;
        }

        //見えないなら
        if (!canSee) {
            if (this.seeTime < -60) {
                this.archer.setAimingBow(false);

                fakePlayer.clearActiveItem();
            }
            return;
        }

        int useCount = fakePlayer.getItemUseTime();
        if (20 <= useCount) {
            //簡易誤射チェック、射線にターゲット以外が居る場合は撃たない
            float distance = MathHelper.sqrt(distanceSq);
            EntityHitResult result = ProjectileUtil.getEntityCollision(mob.world, mob,
                    this.mob.getCameraPosVec(1F), target.getCameraPosVec(1F),
                    this.mob.getBoundingBox().expand(distance), entity ->
                            !entity.isSpectator() && entity.isAlive() && entity.isCollidable());
            if (result != null && result.getType() == HitResult.Type.ENTITY) {
                Entity entity = result.getEntity();
                if (entity != target) {
                    return;
                }
            }

            fakePlayer.yaw = this.mob.yaw;
            fakePlayer.pitch = this.mob.pitch;
            fakePlayer.setPos(mob.getX(), mob.getY(), mob.getZ());

            fakePlayer.stopUsingItem();

            if (this.mob instanceof SoundPlayable) {
                ((SoundPlayable)mob).play(LMSounds.SHOOT);
            }
        }

    }

    public void resetTask() {
        this.mob.setAttacking(false);
        this.seeTime = 0;
        this.archer.setAimingBow(false);

        this.mob.getNavigation().stop();

        FakePlayer fakePlayer = this.fakePlayer.getFakePlayer();
        fakePlayer.clearActiveItem();
    }

    @Override
    public void endModeTask() {

    }

    @Override
    public void writeModeData(CompoundTag tag) {

    }

    @Override
    public void readModeData(CompoundTag tag) {

    }

    @Override
    public String getName() {
        return "Archer";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(BowItem.class);
        ModeManager.INSTANCE.register(ArcherMode.class, items);
    }
}
