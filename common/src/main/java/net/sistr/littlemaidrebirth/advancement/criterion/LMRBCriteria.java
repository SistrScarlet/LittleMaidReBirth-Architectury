package net.sistr.littlemaidrebirth.advancement.criterion;

import net.minecraft.advancement.criterion.Criteria;
import net.sistr.littlemaidrebirth.LMRBMod;

public class LMRBCriteria {
  public static final ContractMaidCriterion CONTRACT_MAID =
      Criteria.register(LMRBMod.MODID + ":contract_maid", new ContractMaidCriterion());
  public static final ResurrectMaidCriterion RESURRECT_MAID =
      Criteria.register(LMRBMod.MODID + ":resurrect_maid", new ResurrectMaidCriterion());

  public static void init() {}
}
