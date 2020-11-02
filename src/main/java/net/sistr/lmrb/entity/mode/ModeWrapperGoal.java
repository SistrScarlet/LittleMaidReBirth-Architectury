package net.sistr.lmrb.entity.mode;

import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

//排他Goal
public class ModeWrapperGoal extends Goal {
    private final ModeSupplier owner;

    public ModeWrapperGoal(ModeSupplier owner) {
        this.owner = owner;
        setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (!owner.getMode().isPresent()) return false;
        return owner.getMode().get().shouldExecute();
    }

    @Override
    public boolean shouldContinue() {
        if (!owner.getMode().isPresent()) return false;
        return owner.getMode().get().shouldContinueExecuting();
    }

    @Override
    public void start() {
        if (!owner.getMode().isPresent()) return;
        owner.getMode().get().startExecuting();
    }

    @Override
    public void stop() {
        if (!owner.getMode().isPresent()) return;
        owner.getMode().get().resetTask();
    }

    @Override
    public void tick() {
        if (!owner.getMode().isPresent()) return;
        owner.getMode().get().tick();
    }
}
