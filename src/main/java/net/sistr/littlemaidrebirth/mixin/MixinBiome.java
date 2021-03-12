package net.sistr.littlemaidrebirth.mixin;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeEffects;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.SpawnSettings;
import net.sistr.littlemaidrebirth.setup.Registration;
import net.sistr.littlemaidrebirth.util.ExtendSpawnSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Biome.class)
public class MixinBiome {

    @Inject(at = @At("RETURN"), method = "<init>")
    public void onConstruct(Biome.Weather weather, Biome.Category category, float depth, float scale,
                            BiomeEffects effects, GenerationSettings generationSettings, SpawnSettings spawnSettings, CallbackInfo ci) {
        if (category == Biome.Category.NONE || category == Biome.Category.OCEAN || category == Biome.Category.RIVER
                || category == Biome.Category.THEEND || category == Biome.Category.NETHER) {
            return;
        }
        ((ExtendSpawnSettings)spawnSettings).addSpawnEntry_LM(Registration.LITTLE_MAID_MOB.getSpawnGroup(),
                new SpawnSettings.SpawnEntry(Registration.LITTLE_MAID_MOB, 5, 1, 3));
    }

}
