package net.sistr.littlemaidrebirth.advancement.criterion;

import com.google.gson.JsonObject;
import net.minecraft.advancement.criterion.TameAnimalCriterion;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.predicate.entity.LootContextPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

public class ResurrectMaidCriterion extends TameAnimalCriterion {
    private static final Identifier ID = new Identifier(LMRBMod.MODID, "resurrect_maid");

    @Override
    public Identifier getId() {
        return ID;
    }

    public void trigger(ServerPlayerEntity player, LittleMaidEntity entity) {
        super.trigger(player, entity);
    }

    @Override
    public Conditions conditionsFromJson(JsonObject jsonObject, LootContextPredicate lootContextPredicate, AdvancementEntityPredicateDeserializer advancementEntityPredicateDeserializer) {
        var lootContextPredicate2 = EntityPredicate.contextPredicateFromJson(jsonObject, "entity", advancementEntityPredicateDeserializer);
        return new RMConditions(lootContextPredicate, lootContextPredicate2);
    }

    public static class RMConditions extends Conditions {

        public RMConditions(LootContextPredicate player, LootContextPredicate entity) {
            super(player, entity);
        }

        @Override
        public Identifier getId() {
            return ID;
        }
    }
}
