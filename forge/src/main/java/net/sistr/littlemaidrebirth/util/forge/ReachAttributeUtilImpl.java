package net.sistr.littlemaidrebirth.util.forge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraftforge.common.ForgeMod;

public class ReachAttributeUtilImpl {

    public static void addAttribute(DefaultAttributeContainer.Builder attributeBuilder) {
        attributeBuilder.add(ForgeMod.ENTITY_REACH.get());
    }

    public static double getAttackRangeSq(LivingEntity entity) {
        double reach = getAttackRange(entity);
        return reach * reach;
    }

    public static double getAttackRange(LivingEntity entity) {
        return entity.getAttributeValue(ForgeMod.ENTITY_REACH.get());
    }

    public static double getRangeSq(LivingEntity entity) {
        double reach = getRange(entity);
        return reach * reach;
    }

    public static double getRange(LivingEntity entity) {
        return entity.getAttributeValue(ForgeMod.ENTITY_REACH.get());
    }

}
