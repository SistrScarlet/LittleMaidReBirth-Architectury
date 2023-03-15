package net.sistr.littlemaidrebirth.entity.util;

import net.minecraft.inventory.Inventory;
import net.minecraft.nbt.NbtCompound;

/**
 * インベントリを持っていることを示すインターフェイス
 */
public interface HasInventory {

    /**
     * インベントリを返す
     */
    Inventory getInventory();

    /**
     * インベントリの状態をNBTに書き出す
     */
    void writeInventory(NbtCompound nbt);

    /**
     * インベントリ状態をNBTから読み込む
     */
    void readInventory(NbtCompound nbt);

}
