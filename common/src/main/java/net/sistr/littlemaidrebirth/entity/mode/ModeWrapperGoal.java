package net.sistr.littlemaidrebirth.entity.mode;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * モードを実行するGoalクラス
 * */
public class ModeWrapperGoal<T extends LivingEntity & HasMode> extends Goal {
    protected final T owner;

    public ModeWrapperGoal(T owner) {
        this.owner = owner;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (owner.getMode().isEmpty()) return false;
        return owner.getMode().get().shouldExecute();
    }

    @Override
    public boolean shouldContinue() {
        if (owner.getMode().isEmpty()) return false;
        return owner.getMode().get().shouldContinueExecuting();
    }

    @Override
    public void start() {
        if (owner.getMode().isEmpty()) return;
        owner.getMode().get().startExecuting();
    }

    @Override
    public void stop() {
        if (owner.getMode().isEmpty()) return;
        owner.getMode().get().resetTask();
    }

    @Override
    public void tick() {
        if (owner.getMode().isEmpty()) return;
        owner.getMode().get().tick();
    }
}
