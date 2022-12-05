package net.sistr.littlemaidrebirth.entity.util;

import net.minecraft.nbt.NbtCompound;

/**
 * 契約可能であることを示すインターフェイス
 */
public interface Contractable {

    /**
     * 現在契約状態であるかを返す
     */
    boolean isContract();

    /**
     * 契約状態をセットする
     */
    void setContract(boolean isContract);

    /**
     * ストライキ中であるかを返す
     */
    boolean isStrike();

    /**
     * ストライキ状態をセットする
     */
    void setStrike(boolean strike);

    /**
     * 契約状態をNBTに書き出す
     */
    void writeContractable(NbtCompound nbt);

    /**
     * 契約状態をNBTから読み込む
     */
    void readContractable(NbtCompound nbt);

}
