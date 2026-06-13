package net.sistr.littlemaidrebirth.advancement.criterion;

import com.mojang.serialization.Codec;
import net.minecraft.advancement.criterion.AbstractCriterion;
import net.minecraft.advancement.criterion.TameAnimalCriterion;
import net.minecraft.loot.context.LootContext;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

public class ContractMaidCriterion extends AbstractCriterion<TameAnimalCriterion.Conditions> {

  @Override
  public Codec<TameAnimalCriterion.Conditions> getConditionsCodec() {
    return TameAnimalCriterion.Conditions.CODEC;
  }

  public void trigger(ServerPlayerEntity player, LittleMaidEntity entity) {
    LootContext lootContext = EntityPredicate.createAdvancementEntityLootContext(player, entity);
    this.trigger(player, conditions -> conditions.matches(lootContext));
  }
}
