package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.sistr.littlemaidrebirth.entity.util.GuiEntitySupplier;
import net.sistr.littlemaidrebirth.entity.util.TameableUtil;

import java.util.EnumSet;

//todo 実装
public class WaitWhenOpenGUIGoal<T extends TameableEntity, M extends ScreenHandler & GuiEntitySupplier<T>> extends Goal {
    private final T mob;
    private final Class<? extends M> screenHandler;

    public WaitWhenOpenGUIGoal(T mob, Class<? extends M> screenHandler) {
        this.mob = mob;
        this.screenHandler = screenHandler;
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        return TameableUtil.getTameOwner(mob)
                .filter(owner -> owner instanceof PlayerEntity)
                .map(owner -> ((PlayerEntity) owner).currentScreenHandler)
                .filter(screen -> this.screenHandler.isAssignableFrom(screen.getClass()))
                .map(screen -> screenHandler.cast(screen).getGuiEntity())
                .filter(guiEntity -> mob == guiEntity)
                .isPresent();
    }

    @Override
    public boolean shouldContinue() {
        return TameableUtil.getTameOwner(mob)
                .filter(owner -> owner instanceof PlayerEntity)
                .map(owner -> ((PlayerEntity) owner).currentScreenHandler)
                .filter(screen -> this.screenHandler.isAssignableFrom(screen.getClass()))
                .isPresent();
    }

    @Override
    public void start() {
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        super.tick();
        TameableUtil.getTameOwner(mob).ifPresent(owner ->
                this.mob.getLookControl().lookAt(owner.getCameraPosVec(1F)));
    }
}
