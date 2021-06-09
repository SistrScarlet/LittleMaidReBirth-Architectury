package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeManager;
import net.sistr.littlemaidrebirth.entity.FakePlayer;
import net.sistr.littlemaidrebirth.entity.FakePlayerSupplier;
import net.sistr.littlemaidrebirth.util.MeleeAttackAccessor;
import net.sistr.littlemaidrebirth.util.ReachAttributeUtil;

//基本的にはMeleeAttackGoalのラッパー
//ただしFakePlayerに殴らせるようにしている
public class FencerMode<T extends PathAwareEntity & FakePlayerSupplier> implements Mode {
    protected final T mob;
    protected final MeleeAttackGoal melee;

    public FencerMode(T mob, double speed, boolean memory) {
        this.mob = mob;
        this.melee = new MeleeAttackGoal(mob, speed, memory) {
            @Override
            protected void attack(LivingEntity target, double squaredDistance) {
                double reachSq = this.getSquaredMaxAttackDistance(target);
                if (reachSq < squaredDistance || 0 < getCooldown() || !this.mob.canSee(target)) {
                    return;
                }
                this.mob.getNavigation().stop();

                this.mob.swingHand(Hand.MAIN_HAND);
                if (this.mob instanceof SoundPlayable) {
                    ((SoundPlayable) mob).play(LMSounds.ATTACK);
                }

                FakePlayer fake = FencerMode.this.mob.getFakePlayer();
                fake.attack(target);
                if (target instanceof MobEntity && ((MobEntity) target).getTarget() == fake) {
                    ((MobEntity) target).setTarget(mob);
                }
                if (target.getAttacker() == fake) {
                    target.setAttacker(mob);
                }
                ((MeleeAttackAccessor) melee).setCool_LM(
                        MathHelper.ceil(fake.getAttackCooldownProgressPerTick() + 0.5F) + 5);
            }

            @Override
            protected double getSquaredMaxAttackDistance(LivingEntity entity) {
                return ReachAttributeUtil.getAttackRangeSq(((FakePlayerSupplier) mob).getFakePlayer());
            }
        };
    }

    @Override
    public void startModeTask() {
    }

    //敵が生きていたら発動
    @Override
    public boolean shouldExecute() {
        return melee.canStart();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return melee.shouldContinue();
    }

    @Override
    public void startExecuting() {
        melee.start();
    }

    @Override
    public void tick() {
        melee.tick();
    }

    @Override
    public void resetTask() {
        melee.stop();
    }

    @Override
    public void endModeTask() {

    }

    @Override
    public void writeModeData(NbtCompound nbt) {

    }

    @Override
    public void readModeData(NbtCompound nbt) {

    }

    @Override
    public String getName() {
        return "Fencer";
    }

    static {
        ModeManager.ModeItems items = new ModeManager.ModeItems();
        items.add(SwordItem.class);
        items.add(AxeItem.class);
        ModeManager.INSTANCE.register(FencerMode.class, items);
    }

}
