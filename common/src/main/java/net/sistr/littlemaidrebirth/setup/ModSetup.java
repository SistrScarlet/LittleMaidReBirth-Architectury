package net.sistr.littlemaidrebirth.setup;

import dev.architectury.registry.level.biome.BiomeModifications;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.api.mode.Modes;
import net.sistr.littlemaidrebirth.entity.iff.IFFTag;
import net.sistr.littlemaidrebirth.entity.iff.IFFType;
import net.sistr.littlemaidrebirth.entity.iff.IFFTypeManager;
import net.sistr.littlemaidrebirth.network.Networking;

import java.util.List;

public class ModSetup {

    public static void init() {
        Networking.INSTANCE.init();

        if (LMRBMod.getConfig().isCanSpawn()) {
            registerSpawnSettingLM();
        }

        IFFTypeManager iffTypeManager = IFFTypeManager.getINSTANCE();
        Registries.ENTITY_TYPE.stream()
                .filter(EntityType::isSummonable)
                //ファッキン仕様変更によりゴーレム/村人のSpawnGroupがMISCになったため無効
                //IFFのsetup時に非生物系を除外するよう変更
                //.filter(type -> type.getSpawnGroup() != SpawnGroup.MISC)
                .forEach(entityType ->
                        iffTypeManager.register(EntityType.getId(entityType),
                                new IFFType(IFFTag.UNKNOWN, entityType)));
        iffTypeManager.register(EntityType.getId(EntityType.PLAYER), new IFFType(IFFTag.UNKNOWN, EntityType.PLAYER));

        Modes.init();
    }

    private static void registerSpawnSettingLM() {
        var spawnBiomeTags = LMRBMod.getConfig().getMaidSpawnBiomeTags()
                .stream()
                .filter(Identifier::isValid)
                .map(Identifier::new)
                .map(id -> TagKey.of(RegistryKeys.BIOME, id))
                .toList();
        var spawnExcludeBiomeTags = LMRBMod.getConfig().getMaidSpawnExcludeBiomeTags()
                .stream()
                .filter(Identifier::isValid)
                .map(Identifier::new)
                .map(id -> TagKey.of(RegistryKeys.BIOME, id))
                .toList();
        BiomeModifications.addProperties((context) -> canSpawnBiome(context, spawnBiomeTags, spawnExcludeBiomeTags),
                (context, mutable) -> mutable.getSpawnProperties()
                        .addSpawn(Registration.LITTLE_MAID_MOB.get().getSpawnGroup(),
                                new SpawnSettings.SpawnEntry(Registration.LITTLE_MAID_MOB.get(),
                                        LMRBMod.getConfig().getSpawnWeight(),
                                        LMRBMod.getConfig().getMinSpawnGroupSize(),
                                        LMRBMod.getConfig().getMaxSpawnGroupSize())));
    }

    private static boolean canSpawnBiome(BiomeModifications.BiomeContext context,
                                         List<TagKey<Biome>> spawnBiomeTags,
                                         List<TagKey<Biome>> spawnExcludeBiomeTags) {
        for (TagKey<Biome> biomeTag : spawnBiomeTags) {
            if (context.hasTag(biomeTag)) {
                for (TagKey<Biome> excludeBiomeTag : spawnExcludeBiomeTags) {
                    if (context.hasTag(excludeBiomeTag)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

}
