package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeType;
import net.sistr.littlemaidrebirth.util.AbstractFurnaceAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class MixinAbstractFurnaceBlockEntity implements AbstractFurnaceAccessor {
    @Unique
    private RecipeType<? extends AbstractCookingRecipe> lmrbRecipeType;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(BlockEntityType blockEntityType, RecipeType recipeType, CallbackInfo ci) {
        this.lmrbRecipeType = recipeType;
    }

    @Shadow
    protected abstract boolean isBurning();

    @Override
    public RecipeType<? extends AbstractCookingRecipe> getRecipeType_LM() {
        return this.lmrbRecipeType;
    }

    @Override
    public boolean isBurningFire_LM() {
        return isBurning();
    }


}
