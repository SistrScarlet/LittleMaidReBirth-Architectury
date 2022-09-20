package net.sistr.littlemaidrebirth.entity.util;

import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;

public interface InventorySupplier {

    Inventory getInventory();

    void writeInventory(NbtCompound nbt);

    void readInventory(NbtCompound nbt);

}