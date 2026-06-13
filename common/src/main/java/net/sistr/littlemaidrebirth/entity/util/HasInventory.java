package net.sistr.littlemaidrebirth.entity.util;

import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;

/** インベントリを持っていることを示すインターフェイス */
public interface HasInventory {

  /** インベントリを返す */
  Inventory getInventory();

  /** インベントリの状態をNBTに書き出す */
  void writeInventory(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup);

  /** インベントリ状態をNBTから読み込む */
  void readInventory(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup);
}
