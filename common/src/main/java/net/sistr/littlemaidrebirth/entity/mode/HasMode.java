package net.sistr.littlemaidrebirth.entity.mode;

import java.util.Optional;
import net.minecraft.nbt.NbtCompound;
import net.sistr.littlemaidrebirth.api.mode.Mode;

/** Mobがモードを保持していることを示すインターフェイス */
public interface HasMode {

    /** 現在実行中のModeを返す */
    Optional<Mode> getMode();

    /** モードの情報をNBTに書き出す */
    void writeModeData(NbtCompound nbt);

    /** モードの情報をNBTから読み込む */
    void readModeData(NbtCompound nbt);
}
