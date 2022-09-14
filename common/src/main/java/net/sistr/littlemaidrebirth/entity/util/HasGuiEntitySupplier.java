package net.sistr.littlemaidrebirth.entity.util;

import net.minecraft.entity.Entity;

public interface HasGuiEntitySupplier<T extends Entity> {

    T getGuiEntity();

}
