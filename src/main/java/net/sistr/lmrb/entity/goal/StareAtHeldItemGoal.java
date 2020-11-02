package net.sistr.lmrb.entity.goal;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;

import java.util.EnumSet;
import java.util.Set;

public class StareAtHeldItemGoal extends Goal {
    protected final PathAwareEntity mob;
    protected final Set<Item> items;
    protected PlayerEntity stareAt;

    public StareAtHeldItemGoal(PathAwareEntity mob, Set<Item> items) {
        this.mob = mob;
        this.items = items;
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
        return items.contains(player.getMainHandStack().getItem()) || items.contains(player.getOffHandStack().getItem());
    }

    @Override
    public void tick() {
        mob.getLookControl().lookAt(stareAt, 30F, 30F);
    }
}
