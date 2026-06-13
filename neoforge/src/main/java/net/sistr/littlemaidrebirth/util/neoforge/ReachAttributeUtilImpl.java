package net.sistr.littlemaidrebirth.util.neoforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.neoforged.neoforge.common.NeoForgeMod;

public class ReachAttributeUtilImpl {

  public static void addAttribute(DefaultAttributeContainer.Builder attributeBuilder) {
    attributeBuilder.add(NeoForgeMod.ENTITY_REACH);
  }

  public static double getAttackRangeSq(LivingEntity entity) {
    double reach = getAttackRange(entity);
    return reach * reach;
  }

  public static double getAttackRange(LivingEntity entity) {
    return entity.getAttributeValue(NeoForgeMod.ENTITY_REACH);
  }

  public static double getRangeSq(LivingEntity entity) {
    double reach = getRange(entity);
    return reach * reach;
  }

  public static double getRange(LivingEntity entity) {
    return entity.getAttributeValue(NeoForgeMod.ENTITY_REACH);
  }
}
