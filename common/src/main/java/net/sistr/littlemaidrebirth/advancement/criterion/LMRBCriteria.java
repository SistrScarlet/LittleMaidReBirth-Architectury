package net.sistr.littlemaidrebirth.advancement.criterion;

import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.advancement.criterion.TameAnimalCriterion;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.mixin.CriteriaAccessor;

public class LMRBCriteria {
    public static final TameAnimalCriterion CONTRACT_MAID = register("contract_maid", new TameAnimalCriterion());

    public static void init() {

    }

    private static <T extends Criterion<?>> T register(String id, T criterion) {
        var identifier = new Identifier(LMRBMod.MODID, id);
        var values = CriteriaAccessor.getValues();
        if (values.putIfAbsent(identifier, criterion) != null) {
            throw new IllegalArgumentException("Duplicate criterion id " + id);
        }
        return criterion;
    }
}
