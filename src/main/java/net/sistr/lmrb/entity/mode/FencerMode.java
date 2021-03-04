package net.sistr.lmrb.entity.mode;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.SwordItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.sistr.lmml.entity.compound.SoundPlayable;
import net.sistr.lmml.resource.util.LMSounds;
import net.sistr.lmrb.entity.FakePlayer;
import net.sistr.lmrb.entity.FakePlayerSupplier;
import net.sistr.lmrb.util.MeleeAttackAccessor;

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
                if (reachSq < squaredDistance || 0 < method_28348() || !this.mob.canSee(target)) {
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
                double reachSq = ReachEntityAttributes
                        .getSquaredAttackRange(((FakePlayerSupplier) mob).getFakePlayer(), 4.5D - 1D);
                reachSq = Math.max(reachSq, 0);
                return reachSq;
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
    public void writeModeData(CompoundTag tag) {

    }

    @Override
    public void readModeData(CompoundTag tag) {

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
