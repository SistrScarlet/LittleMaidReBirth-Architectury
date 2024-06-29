package net.sistr.littlemaidrebirth.advancement.criterion;

import net.minecraft.advancement.criterion.Criterion;
import net.minecraft.advancement.criterion.TameAnimalCriterion;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LMRBMod;

public class LMRBCriteria {
    public static final TameAnimalCriterion CONTRACT_MAID = register("contract_maid", new TameAnimalCriterion());

    public static void init() {

    }

    public static <T extends Criterion<?>> T register(String id, T criterion) {
        return Registry.register(Registries.CRITERION, Identifier.of(LMRBMod.MODID, id), criterion);
    }
}
