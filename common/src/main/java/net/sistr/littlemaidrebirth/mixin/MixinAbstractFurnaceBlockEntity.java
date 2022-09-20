package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.recipe.AbstractCookingRecipe;
import net.minecraft.recipe.RecipeType;
import net.minecraft.util.math.BlockPos;
import net.sistr.littlemaidrebirth.util.AbstractFurnaceAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class MixinAbstractFurnaceBlockEntity implements AbstractFurnaceAccessor {
    private RecipeType<? extends AbstractCookingRecipe> recipeType;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(BlockEntityType blockEntityType, BlockPos pos, BlockState state, RecipeType recipeType, CallbackInfo ci) {
        this.recipeType = recipeType;
    }

    @Shadow
    protected abstract boolean isBurning();

    @Override
    public RecipeType<? extends AbstractCookingRecipe> getRecipeType_LM() {
        return this.recipeType;
    }

    @Override
    public boolean isBurningFire_LM() {
        return isBurning();
    }


}
