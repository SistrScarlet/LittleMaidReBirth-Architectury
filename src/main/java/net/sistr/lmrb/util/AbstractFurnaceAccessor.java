package net.sistr.lmrb.util;

import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeType;

public interface AbstractFurnaceAccessor {

    RecipeType<? extends AbstractCookingRecipe> getRecipeType_LM();

    boolean isBurningFire_LM();

}
