package net.sistr.littlemaidrebirth.api.mode;

import net.minecraft.item.ItemStack;

/**
 * アイテムが条件にマッチするかをチェックするインターフェイス
 */
public interface ItemMatcher {
    boolean isMatch(ItemStack stack);
}
