package net.sistr.littlemaidrebirth.entity;

import net.sistr.littlemaidrebirth.LMRBMod;

public abstract class LMFakePlayerWrapperEntity<T extends LittleMaidEntity> extends FakePlayerWrapperEntity<T> {

    public LMFakePlayerWrapperEntity(T origin) {
        super(origin);
    }

    @Override
    protected void pickupItems() {
        if (LMRBMod.getConfig().isCanPickupItemByNoOwnerLM()
                || getOrigin().getTameOwnerUuid().isPresent()) {
            super.pickupItems();
        }
    }

}
