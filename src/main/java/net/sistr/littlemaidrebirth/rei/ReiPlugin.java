package net.sistr.littlemaidrebirth.rei;

import me.shedaniel.rei.api.RecipeHelper;
import me.shedaniel.rei.api.plugins.REIPluginV0;
import net.minecraft.util.Identifier;
import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;

public class ReiPlugin implements REIPluginV0 {
    @Override
    public Identifier getPluginIdentifier() {
        return new Identifier(LittleMaidReBirthMod.MODID, "rei_plugin");
    }

    @Override
    public void registerPluginCategories(RecipeHelper recipeHelper) {

    }


}