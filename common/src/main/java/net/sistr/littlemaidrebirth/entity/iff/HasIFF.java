package net.sistr.littlemaidrebirth.entity.iff;

import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;

import java.util.List;
import java.util.Optional;

/**
 * MobがIFFを持っていることを示すインターフェイス
 */
public interface HasIFF {

    /**
     * エンティティを識別し、結果が存在すれば返す
     */
    Optional<IFFTag> identify(LivingEntity target);

    /**
     * IFFリストをセットする
     */
    void setIFFs(List<IFF> iffs);

    List<IFF> getIFFs();

    /**
     * IFFの状態を書き出す
     */
    void writeIFF(NbtCompound nbt);

    /**
     * IFFの状態を読み込む
     */
    void readIFF(NbtCompound nbt);

}
