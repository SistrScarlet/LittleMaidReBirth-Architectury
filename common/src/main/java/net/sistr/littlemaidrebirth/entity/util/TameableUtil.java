package net.sistr.littlemaidrebirth.entity.util;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.passive.TameableEntity;

import java.util.Optional;
import java.util.UUID;

public class TameableUtil {

    /**
     * テイムしたご主人を返す
     * 同じワールドに存在しない場合、emptyで返す
     */
    public static Optional<LivingEntity> getTameOwner(Tameable tameable) {
        return Optional.ofNullable(tameable.getOwner());
    }

    /**
     * テイムしたご主人のUUIDをセットする
     * テイムしたことになる
     */
    public static void setTameOwnerUuid(TameableEntity tameable, UUID id) {
        tameable.setOwnerUuid(id);
    }

    /**
     * テイムしたご主人のUUIDを返す
     * 存在しない場合、emptyで返す
     */
    public static Optional<UUID> getTameOwnerUuid(Tameable tameable) {
        return Optional.ofNullable(tameable.getOwnerUuid());
    }

    public static boolean hasTameOwner(Tameable tameable) {
        return TameableUtil.getTameOwner(tameable).isPresent();
    }

    /**
     * 待機中であるか否かを返す
     */
    public static boolean isWait(TameableEntity tameable) {
        return tameable.isSitting();
    }

    /**
     * 待機状態をセットする
     */
    public static void setWait(TameableEntity tameable, boolean isWait) {
        tameable.setSitting(isWait);
    }

    public static void switchWait(TameableEntity tameable) {
        tameable.setSitting(!tameable.isSitting());
    }

    /**
     * ご主人が同じならtrue
     * ご主人を持っていない場合はfalse
     */
    public static boolean equalTameOwner(Tameable a, Tameable b) {
        var aOwner = TameableUtil.getTameOwner(a);
        var bOwner = TameableUtil.getTameOwner(b);
        if (aOwner.isEmpty() || bOwner.isEmpty()) {
            return false;
        }
        return aOwner.get().equals(bOwner.get());
    }

}
