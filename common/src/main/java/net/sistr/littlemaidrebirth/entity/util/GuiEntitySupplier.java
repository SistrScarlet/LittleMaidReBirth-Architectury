package net.sistr.littlemaidrebirth.entity.util;

import net.minecraft.entity.Entity;

/**
 * 開いているGUIのエンティティを取得するインターフェイス
 */
public interface GuiEntitySupplier<T extends Entity> {

    T getGuiEntity();

}
