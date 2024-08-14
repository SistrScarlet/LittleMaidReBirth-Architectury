package net.sistr.littlemaidrebirth.entity.goal;

import net.minecraft.entity.passive.TameableEntity;
import net.sistr.littlemaidrebirth.entity.util.HasMovingMode;
import net.sistr.littlemaidrebirth.entity.util.MovingMode;

public class HasMMTeleportTameOwnerGoal
        <T extends TameableEntity
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
