package net.sistr.littlemaidrebirth.advancement.criterion;

import com.google.gson.JsonObject;
import net.minecraft.advancement.criterion.TameAnimalCriterion;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;

public class ContractMaidCriterion extends TameAnimalCriterion {
    private static final Identifier ID = new Identifier(LMRBMod.MODID, "contract_maid");

    @Override
    public Identifier getId() {
        return ID;
    }

    public void trigger(ServerPlayerEntity player, LittleMaidEntity entity) {
        super.trigger(player, entity);
    }

    @Override
    public Conditions conditionsFromJson(JsonObject jsonObject, EntityPredicate.Extended extended, AdvancementEntityPredicateDeserializer advancementEntityPredicateDeserializer) {
        EntityPredicate.Extended extended2 = EntityPredicate.Extended.getInJson(jsonObject, "entity", advancementEntityPredicateDeserializer);
        return new CMConditions(extended, extended2);
    }

    public static class CMConditions extends Conditions {

        public CMConditions(EntityPredicate.Extended player, EntityPredicate.Extended entity) {
            super(player, entity);
        }

        @Override
        public Identifier getId() {
            return ID;
        }
    }
}
