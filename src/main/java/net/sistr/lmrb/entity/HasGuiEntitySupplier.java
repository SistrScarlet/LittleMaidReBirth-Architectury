package net.sistr.lmrb.entity;

import net.minecraft.entity.Entity;

public interface HasGuiEntitySupplier<T extends Entity> {

    T getGuiEntity();

}
