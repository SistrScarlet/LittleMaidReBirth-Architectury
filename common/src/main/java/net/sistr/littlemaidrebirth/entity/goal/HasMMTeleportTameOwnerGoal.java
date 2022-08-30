package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.mob.PathAwareEntity;
import net.sistr.littlemaidrebirth.entity.HasMovingMode;
import net.sistr.littlemaidrebirth.entity.MovingMode;
import net.sistr.littlemaidrebirth.entity.Tameable;

public class HasMMTeleportTameOwnerGoal
        <T extends PathAwareEntity
                & Tameable
                & HasMovingMode>
        extends TeleportTameOwnerGoal<T> {

    public HasMMTeleportTameOwnerGoal(T tameable, float teleportStart) {
        super(tameable, teleportStart);
    }

    @Override
    public boolean canStart() {
        return this.tameable.getMovingMode() == MovingMode.ESCORT && super.canStart();
    }
}
