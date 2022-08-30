package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import java.util.EnumSet;
import java.util.function.Predicate;

public class StareAtHeldItemGoal<T extends PathAwareEntity> extends Goal {
    protected final T mob;
    protected final Predicate<ItemStack> targetItem;
    protected PlayerEntity stareAt;

    public StareAtHeldItemGoal(T mob, Predicate<ItemStack> targetItem) {
        this.mob = mob;
        this.targetItem = targetItem;
        setControls(EnumSet.of(Control.LOOK));
    }

    @Override
    public boolean canStart() {
        stareAt = mob.world.getClosestPlayer(mob, 4);
        return stareAt != null && isHeldTargetItem(stareAt);
    }

    @Override
    public boolean shouldContinue() {
        return isHeldTargetItem(stareAt);
    }

    public boolean isHeldTargetItem(PlayerEntity player) {
        return targetItem.test(player.getMainHandStack()) || targetItem.test(player.getOffHandStack());
    }

    @Override
    public void tick() {
        mob.getLookControl().lookAt(stareAt, 30F, 30F);
    }
}
