package net.sistr.littlemaidrebirth.advancement.criterion;

import net.minecraft.advancement.criterion.Criteria;
import net.sistr.littlemaidrebirth.LMRBMod;

public class LMRBCriteria {
    public static final ContractMaidCriterion CONTRACT_MAID = new ContractMaidCriterion();
    public static final ResurrectMaidCriterion RESURRECT_MAID = new ResurrectMaidCriterion();

    public static void init() {
        Criteria.register(LMRBMod.MODID + ":contract_maid", CONTRACT_MAID);
        Criteria.register(LMRBMod.MODID + ":resurrect_maid", RESURRECT_MAID);
    }
}
