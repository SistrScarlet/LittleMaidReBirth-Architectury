package net.sistr.littlemaidrebirth.advancement.criterion;

import net.sistr.littlemaidrebirth.mixin.CriteriaInvoker;

public class LMRBCriteria {
    public static final ContractMaidCriterion CONTRACT_MAID = CriteriaInvoker.invokeRegister(new ContractMaidCriterion());

    public static void init() {

    }
}
