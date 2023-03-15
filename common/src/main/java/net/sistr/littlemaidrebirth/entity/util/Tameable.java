package net.sistr.littlemaidrebirth.entity.util;

import net.minecraft.entity.LivingEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * テイム可能であることを示すインターフェイス
 */
public interface Tameable {

    /**
     * テイムしたご主人を返す
     * 同じワールドに存在しない場合、emptyで返す
     */
    Optional<LivingEntity> getTameOwner();

    /**
     * テイムしたご主人のUUIDをセットする
     * テイムしたことになる
     */
    void setTameOwnerUuid(UUID id);

    /**
     * テイムしたご主人のUUIDを返す
     * 存在しない場合、emptyで返す
     */
    Optional<UUID> getTameOwnerUuid();

    /**
     * テイムしたご主人が居るかどうかを返す
     */
    boolean hasTameOwner();

    /**
     * 待機中であるか否かを返す
     */
    boolean isWait();

    /**
     * 待機状態をセットする
     */
    void setWait(boolean isWait);

}
