package net.sistr.lmrb.mixin;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeType;
import net.sistr.lmrb.util.AbstractFurnaceAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class MixinAbstractFurnaceBlockEntity implements AbstractFurnaceAccessor {

    @Shadow @Final protected RecipeType<? extends AbstractCookingRecipe> recipeType;

    @Shadow protected abstract boolean isBurning();

    @Override
    public RecipeType<? extends AbstractCookingRecipe> getRecipeType_LM() {
        return this.recipeType;
    }

    @Override
    public boolean isBurningFire_LM() {
        return isBurning();
    }


}
