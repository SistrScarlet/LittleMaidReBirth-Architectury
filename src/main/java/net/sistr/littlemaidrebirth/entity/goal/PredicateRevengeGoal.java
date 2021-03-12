package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.mob.PathAwareEntity;

import java.util.function.Predicate;

public class PredicateRevengeGoal extends RevengeGoal {
    protected final Predicate<LivingEntity> target;

    public PredicateRevengeGoal(PathAwareEntity mob, Predicate<LivingEntity> target, Class<?>... noRevengeTypes) {
        super(mob, noRevengeTypes);
        this.target = target;
    }

    @Override
    public boolean canStart() {
        return super.canStart() && target.test(this.mob.getAttacker());
    }
}
