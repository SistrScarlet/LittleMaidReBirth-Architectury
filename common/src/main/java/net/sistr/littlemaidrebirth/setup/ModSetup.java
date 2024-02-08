package net.sistr.littlemaidrebirth.setup;

import me.shedaniel.architectury.registry.BiomeModifications;
import me.shedaniel.architectury.registry.CreativeTabs;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.SpawnSettings;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.api.mode.Modes;
import net.sistr.littlemaidrebirth.entity.iff.IFFTag;
import net.sistr.littlemaidrebirth.entity.iff.IFFType;
import net.sistr.littlemaidrebirth.entity.iff.IFFTypeManager;
import net.sistr.littlemaidrebirth.network.Networking;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ModSetup {
    public static final ItemGroup ITEM_GROUP = CreativeTabs
            .create(new Identifier(LMRBMod.MODID, "common"), Items.CAKE::getDefaultStack);

    public static void init() {
        Networking.INSTANCE.init();

        if (LMRBMod.getConfig().isCanSpawn()) {
            registerSpawnSettingLM();
        }

        IFFTypeManager iffTypeManager = IFFTypeManager.getINSTANCE();
        Registry.ENTITY_TYPE.stream()
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
        Set<Identifier> spawnBiomeTags = LMRBMod.getConfig().getMaidSpawnBiomes()
                .stream()
                .map(Identifier::tryParse)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Identifier> spawnExcludeBiomeTags = LMRBMod.getConfig().getMaidSpawnExcludeBiomes()
                .stream()
                .map(Identifier::tryParse)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        BiomeModifications.addProperties((context) -> canSpawnBiome(context, spawnBiomeTags, spawnExcludeBiomeTags),
                (context, mutable) -> mutable.getSpawnProperties()
                        .addSpawn(Registration.LITTLE_MAID_MOB.get().getSpawnGroup(),
                                new SpawnSettings.SpawnEntry(Registration.LITTLE_MAID_MOB.get(),
                                        LMRBMod.getConfig().getSpawnWeight(),
                                        LMRBMod.getConfig().getMinSpawnGroupSize(),
                                        LMRBMod.getConfig().getMaxSpawnGroupSize())));
    }

    private static boolean canSpawnBiome(BiomeModifications.BiomeContext context,
                                         Set<Identifier> spawnBiomeTags,
                                         Set<Identifier> spawnExcludeBiomeTags) {
        return spawnBiomeTags.contains(context.getKey())
                && !spawnExcludeBiomeTags.contains(context.getKey());
    }

}
