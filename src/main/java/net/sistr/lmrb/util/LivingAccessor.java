package net.sistr.lmrb.util;

import net.minecraft.entity.damage.DamageSource;

public interface LivingAccessor {

    void applyEquipmentAttributes_LM();

    void tickActiveItemStack_LM();

    boolean blockedByShield_LM(DamageSource source);
}
