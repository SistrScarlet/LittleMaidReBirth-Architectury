package net.sistr.littlemaidrebirth.util;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;

public class ReachAttributeUtil {

    @ExpectPlatform
    public static void addAttribute(DefaultAttributeContainer.Builder attributeBuilder) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static double getAttackRangeSq(LivingEntity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static double getAttackRange(LivingEntity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static double getRangeSq(LivingEntity entity) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static double getRange(LivingEntity entity) {
        throw new AssertionError();
    }

}
