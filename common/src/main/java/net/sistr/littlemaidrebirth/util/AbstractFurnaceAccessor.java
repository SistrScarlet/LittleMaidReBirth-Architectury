package net.sistr.littlemaidrebirth.util;

import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeType;

/**
 * Mixin Accessor
 * */
public interface AbstractFurnaceAccessor {

    RecipeType<? extends AbstractCookingRecipe> getRecipeType_LM();

    boolean isBurningFire_LM();

}
