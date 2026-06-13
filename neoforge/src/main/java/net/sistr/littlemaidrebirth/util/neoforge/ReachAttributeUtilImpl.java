package net.sistr.littlemaidrebirth.util.neoforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.player.PlayerEntity;

public class ReachAttributeUtilImpl {

    public static void addAttribute(DefaultAttributeContainer.Builder attributeBuilder) {
        // NeoForge 1.21.1 では reach がバニラ属性 (entity_interaction_range) に統合され、
        // 旧 NeoForgeMod.ENTITY_REACH は廃止。fabric 同様に固定値運用とする。
    }

    public static double getAttackRangeSq(LivingEntity entity) {
        double reach = getAttackRange(entity);
        return reach * reach;
    }

    public static double getAttackRange(LivingEntity entity) {
        return 3;
    }

    public static double getRangeSq(LivingEntity entity) {
        double reach = getRange(entity);
        return reach * reach;
    }

    public static double getRange(LivingEntity entity) {
        double base;
        if (entity instanceof PlayerEntity) {
            base = ((PlayerEntity) entity).isCreative() ? 5 : 4.5;
        } else {
            base = 4.5;
        }
        return base;
    }
}
