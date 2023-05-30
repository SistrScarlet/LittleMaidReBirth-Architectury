package net.sistr.littlemaidrebirth.setup;

import dev.architectury.registry.level.biome.BiomeModifications;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.SpawnSettings;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.api.mode.Modes;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.iff.IFFTag;
import net.sistr.littlemaidrebirth.entity.iff.IFFType;
import net.sistr.littlemaidrebirth.entity.iff.IFFTypeManager;
import net.sistr.littlemaidrebirth.network.Networking;
import net.sistr.littlemaidrebirth.util.SpawnRestrictionRegister;

public class ModSetup {

    public static void init() {
        Networking.INSTANCE.init();

        if (LMRBMod.getConfig().isCanSpawn()) {
            registerSpawnSettingLM();
        }

        IFFTypeManager iffTypeManager = IFFTypeManager.getINSTANCE();
        Registries.ENTITY_TYPE.stream()
                .filter(EntityType::isSummonable)
                .filter(type -> type.getSpawnGroup() != SpawnGroup.MISC)
                .forEach(entityType ->
                        iffTypeManager.register(EntityType.getId(entityType),
                                new IFFType(IFFTag.UNKNOWN, entityType)));
        iffTypeManager.register(EntityType.getId(EntityType.PLAYER), new IFFType(IFFTag.UNKNOWN, EntityType.PLAYER));

        Modes.init();
    }

    private static void registerSpawnSettingLM() {
        BiomeModifications.addProperties(ModSetup::canSpawnBiome,
                (context, mutable) -> mutable.getSpawnProperties()
                        .addSpawn(Registration.LITTLE_MAID_MOB.get().getSpawnGroup(),
                                new SpawnSettings.SpawnEntry(Registration.LITTLE_MAID_MOB.get(),
                                        LMRBMod.getConfig().getSpawnWeight(),
                                        LMRBMod.getConfig().getMinSpawnGroupSize(),
                                        LMRBMod.getConfig().getMaxSpawnGroupSize())));

        SpawnRestrictionRegister.callRegister(Registration.LITTLE_MAID_MOB.get(),
                SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, spawnReason, pos, random) -> LittleMaidEntity.isValidNaturalSpawn(world, pos));
    }

    private static boolean canSpawnBiome(BiomeModifications.BiomeContext context) {
        return context.hasTag(BiomeTags.VILLAGE_DESERT_HAS_STRUCTURE)
                || context.hasTag(BiomeTags.VILLAGE_PLAINS_HAS_STRUCTURE)
                || context.hasTag(BiomeTags.VILLAGE_SAVANNA_HAS_STRUCTURE)
                || context.hasTag(BiomeTags.VILLAGE_SNOWY_HAS_STRUCTURE)
                || context.hasTag(BiomeTags.VILLAGE_TAIGA_HAS_STRUCTURE);
    }

}
