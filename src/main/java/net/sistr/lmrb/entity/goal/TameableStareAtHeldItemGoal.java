package net.sistr.lmrb.entity.goal;

import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.item.Item;
import net.sistr.lmrb.entity.Tameable;

import java.util.Set;

public class TameableStareAtHeldItemGoal extends StareAtHeldItemGoal {
    protected final Tameable tameable;
    protected final boolean isTamed;

    public TameableStareAtHeldItemGoal(PathAwareEntity mob, Tameable tameable, boolean isTamed, Set<Item> items) {
        super(mob, items);
        this.tameable = tameable;
        this.isTamed = isTamed;
    }

    @Override
    public boolean canStart() {
        return tameable.getTameOwner().isPresent() == isTamed && super.canStart();
    }
}
