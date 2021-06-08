package net.sistr.littlemaidrebirth.util.fabric;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.player.PlayerEntity;

public class ReachAttributeUtilImpl {

    public static void addAttribute(DefaultAttributeContainer.Builder attributeBuilder) {
        attributeBuilder.add(ReachEntityAttributes.REACH)
                .add(ReachEntityAttributes.ATTACK_RANGE);
    }

    public static double getAttackRangeSq(LivingEntity entity) {
        double reach = getAttackRange(entity);
        return reach * reach;
    }

    public static double getAttackRange(LivingEntity entity) {
        double base;
        if (entity instanceof PlayerEntity) {
            base = ((PlayerEntity) entity).isCreative() ? 5 : 4.5;
        } else {
            base = 4.5;
        }
        return base + entity.getAttributeValue(ReachEntityAttributes.ATTACK_RANGE);
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
        return base + entity.getAttributeValue(ReachEntityAttributes.REACH);
    }

}
