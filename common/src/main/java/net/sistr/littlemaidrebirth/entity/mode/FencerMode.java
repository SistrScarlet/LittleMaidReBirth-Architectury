package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidmodelloader.entity.compound.SoundPlayable;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.FakePlayer;
import net.sistr.littlemaidrebirth.entity.util.HasFakePlayer;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.util.MeleeAttackAccessor;
import net.sistr.littlemaidrebirth.util.ReachAttributeUtil;

//基本的にはMeleeAttackGoalのラッパー
//ただしFakePlayerに殴らせるようにしている
//todo 戦闘ロジック高度化
public class FencerMode extends Mode {
    protected final LittleMaidEntity mob;
    protected final MeleeAttackGoal melee;

    public FencerMode(ModeType<? extends FencerMode> modeType, String name, LittleMaidEntity mob, double speed, boolean memory) {
        super(modeType, name);
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
                return ReachAttributeUtil.getAttackRangeSq(((HasFakePlayer) mob).getFakePlayer())
                        * LMRBMod.getConfig().getFencerRangeFactor();
            }
        };
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
        this.mob.play(LMSounds.FIND_TARGET_N);
    }

    @Override
    public void tick() {
        melee.tick();
    }

    @Override
    public void resetTask() {
        melee.stop();
    }

}
