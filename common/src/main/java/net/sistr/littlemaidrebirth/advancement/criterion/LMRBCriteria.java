package net.sistr.littlemaidrebirth.advancement.criterion;

import net.minecraft.advancement.criterion.Criteria;

public class LMRBCriteria {
    public static final ContractMaidCriterion CONTRACT_MAID =
            Criteria.register(new ContractMaidCriterion());
    public static final ResurrectMaidCriterion RESURRECT_MAID =
            Criteria.register(new ResurrectMaidCriterion());

    public static void init() {}
}
