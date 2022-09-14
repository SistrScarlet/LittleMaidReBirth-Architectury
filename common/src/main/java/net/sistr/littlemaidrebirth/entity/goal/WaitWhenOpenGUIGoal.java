package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.sistr.littlemaidrebirth.entity.util.HasGuiEntitySupplier;
import net.sistr.littlemaidrebirth.entity.util.Tameable;

import java.util.EnumSet;

public class WaitWhenOpenGUIGoal<T extends PathAwareEntity, M extends ScreenHandler & HasGuiEntitySupplier<T>> extends Goal {
    private final T mob;
    private final Tameable tameable;
    private final Class<? extends M> screenHandler;

    public WaitWhenOpenGUIGoal(T mob, Tameable tameable, Class<? extends M> screenHandler) {
        this.mob = mob;
        this.tameable = tameable;
        this.screenHandler = screenHandler;
        setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        return tameable.getTameOwner()
                .filter(owner -> owner instanceof PlayerEntity)
                .map(owner -> ((PlayerEntity) owner).currentScreenHandler)
                .filter(screen -> this.screenHandler.isAssignableFrom(screen.getClass()))
                .map(screen -> screenHandler.cast(screen).getGuiEntity())
                .filter(guiEntity -> mob == guiEntity)
                .isPresent();
    }

    @Override
    public boolean shouldContinue() {
        return tameable.getTameOwner()
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
        tameable.getTameOwner().ifPresent(owner ->
                this.mob.getLookControl().lookAt(owner.getCameraPosVec(1F)));
    }
}
