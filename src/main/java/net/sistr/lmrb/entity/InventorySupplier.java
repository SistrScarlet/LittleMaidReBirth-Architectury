package net.sistr.lmrb.entity;

import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.CompoundTag;

public interface InventorySupplier {

    Inventory getInventory();

    void writeInventory(CompoundTag tag);

    void readInventory(CompoundTag tag);

}
