package net.sistr.littlemaidrebirth.util;

import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import java.util.List;

public interface PlayerInventoryAccessor {
    List<DefaultedList<ItemStack>> getCombinedInventory();
}
