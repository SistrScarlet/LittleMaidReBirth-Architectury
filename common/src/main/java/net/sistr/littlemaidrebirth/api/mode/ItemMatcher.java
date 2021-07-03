package net.sistr.littlemaidrebirth.api.mode;

import net.minecraft.item.ItemStack;

public interface ItemMatcher {
    boolean isMatch(ItemStack stack);
}
