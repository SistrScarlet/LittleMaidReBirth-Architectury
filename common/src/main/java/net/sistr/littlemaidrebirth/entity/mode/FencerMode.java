package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.math.MathHelper;
import net.sistr.littlemaidmodelloader.resource.util.LMSounds;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.api.mode.Mode;
import net.sistr.littlemaidrebirth.api.mode.ModeType;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.mixin.MeleeAttackGoalAccessor;
import net.sistr.littlemaidrebirth.util.ReachAttributeUtil;

//基本的にはMeleeAttackGoalのラッパー
public class FencerMode extends Mode {
    protected final LittleMaidEntity mob;
    protected final MeleeAttackGoal melee;

    public FencerMode(ModeType<? extends FencerMode> modeType, String name, LittleMaidEntity mob, double speed, boolean memory) {
        super(modeType, name);
        this.mob = mob;
        this.melee = new MeleeAttackGoal(mob, speed, memory) {

            @Override
            protected boolean canAttack(LivingEntity target) {
                return this.isCooledDown()
                        && this.isInAttackRange(target)
                        && this.mob.getVisibilityCache().canSee(target);
            }

            private double getAttackRadiusSq() {
                return ReachAttributeUtil.getAttackRangeSq(mob)
                        * LMRBMod.getConfig().getFencerRangeFactor();
            }

            private boolean isInAttackRange(LivingEntity target) {
                return this.mob.squaredDistanceTo(target) < this.getAttackRadiusSq();
            }

            @Override
            protected void resetCooldown() {
                double attackSpeed = this.mob.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED);
                int cool = MathHelper.ceil(1 / attackSpeed * 20
                        / LMRBMod.getConfig().getFencerAttackRateFactor());
                ((MeleeAttackGoalAccessor) melee).setCooldown(cool);
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

    @Override
    public boolean isBattleMode() {
        return true;
    }
}
