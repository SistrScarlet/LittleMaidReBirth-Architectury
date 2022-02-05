package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.function.Predicate;

public class StartPredicateGoalWrapper<T extends Goal> extends Goal {
    private final T goal;
    private final Predicate<T> predicate;

    public StartPredicateGoalWrapper(T goal, Predicate<T> predicate) {
        this.goal = goal;
        this.predicate = predicate;
    }

    @Override
    public boolean canStart() {
        return predicate.test(goal) && goal.canStart();
    }

    @Override
    public boolean shouldContinue() {
        return predicate.test(goal) && goal.shouldContinue();
    }

    @Override
    public boolean canStop() {
        return goal.canStop();
    }

    @Override
    public void start() {
        goal.start();
    }

    @Override
    public void stop() {
        goal.stop();
    }

    @Override
    public void tick() {
        goal.tick();
    }

    @Override
    public void setControls(EnumSet<Control> controls) {
        goal.setControls(controls);
    }

    @Override
    public EnumSet<Control> getControls() {
        return goal.getControls();
    }
}
