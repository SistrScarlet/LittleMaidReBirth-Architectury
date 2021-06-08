package net.sistr.littlemaidrebirth.setup;

import me.shedaniel.architectury.registry.BiomeModifications;
import me.shedaniel.architectury.registry.CreativeTabs;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import net.sistr.littlemaidrebirth.config.LMRBConfig;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.iff.IFFTag;
import net.sistr.littlemaidrebirth.entity.iff.IFFType;
import net.sistr.littlemaidrebirth.entity.iff.IFFTypeManager;
import net.sistr.littlemaidrebirth.mixin.SpawnRestrictionAccessor;
import net.sistr.littlemaidrebirth.network.Networking;

public class ModSetup {
    public static final ItemGroup ITEM_GROUP = CreativeTabs
            .create(new Identifier(LittleMaidReBirthMod.MODID, "common"), Items.CAKE::getDefaultStack);

    public static void init() {
        Networking.INSTANCE.init();

        if (LMRBConfig.canSpawnLM()) registerSpawnSettingLM();

        IFFTypeManager iffTypeManager = IFFTypeManager.getINSTANCE();
        Registry.ENTITY_TYPE.stream().filter(EntityType::isSummonable).forEach(entityType ->
                iffTypeManager.register(EntityType.getId(entityType),
                        new IFFType(IFFTag.UNKNOWN, entityType)));
    }

    private static void registerSpawnSettingLM() {
        BiomeModifications.addProperties(context -> canSpawnBiome(context.getProperties().getCategory()),
                (context, mutable) -> mutable.getSpawnProperties()
                        .addSpawn(Registration.LITTLE_MAID_MOB.get().getSpawnGroup(),
                                new SpawnSettings.SpawnEntry(Registration.LITTLE_MAID_MOB.get(),
                                        LMRBConfig.getSpawnWeightLM(),
                                        LMRBConfig.getMinSpawnGroupSizeLM(),
                                        LMRBConfig.getMaxSpawnGroupSizeLM())));
        SpawnRestrictionAccessor.callRegister(Registration.LITTLE_MAID_MOB.get(),
                SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, spawnReason, pos, random) -> LittleMaidEntity.isValidNaturalSpawn(world, pos));
    }

    private static boolean canSpawnBiome(Biome.Category category) {
        return category != Biome.Category.NONE
                && category != Biome.Category.OCEAN
                && category != Biome.Category.RIVER
                && category != Biome.Category.THEEND
                && category != Biome.Category.NETHER;
    }

}
