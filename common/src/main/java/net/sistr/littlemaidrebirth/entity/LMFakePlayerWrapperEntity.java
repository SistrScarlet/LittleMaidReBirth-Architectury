package net.sistr.littlemaidrebirth.entity;

import net.sistr.littlemaidrebirth.config.LMRBConfig;

public abstract class LMFakePlayerWrapperEntity<T extends LittleMaidEntity> extends FakePlayerWrapperEntity<T> {

    public LMFakePlayerWrapperEntity(T origin) {
        super(origin);
    }

    @Override
    protected void pickupItems() {
        if (LMRBConfig.canPickupItemByNoOwnerLM() || getOrigin().getTameOwnerUuid().isPresent()) {
            super.pickupItems();
        }
    }

}
