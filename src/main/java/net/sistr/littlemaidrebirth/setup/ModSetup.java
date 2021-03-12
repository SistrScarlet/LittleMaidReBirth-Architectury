package net.sistr.littlemaidrebirth.setup;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.mixin.object.builder.SpawnRestrictionAccessor;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Heightmap;
import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.entity.iff.IFFTag;
import net.sistr.littlemaidrebirth.entity.iff.IFFType;
import net.sistr.littlemaidrebirth.entity.iff.IFFTypeManager;
import net.sistr.littlemaidrebirth.network.Networking;

public class ModSetup implements ModInitializer {

    public static final ItemGroup ITEM_GROUP = FabricItemGroupBuilder
            .create(new Identifier(LittleMaidReBirthMod.MODID, "common"))
            .icon(Items.CAKE::getDefaultStack)
            .build();

    @Override
    public void onInitialize() {
        Networking.INSTANCE.serverInit();

        FabricDefaultAttributeRegistry.register(Registration.LITTLE_MAID_MOB, LittleMaidEntity.createLittleMaidAttributes());
        SpawnRestrictionAccessor.callRegister(Registration.LITTLE_MAID_MOB, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (type, world, spawnReason, pos, random) -> LittleMaidEntity.isValidNaturalSpawn(world, pos));

        IFFTypeManager iffTypeManager = IFFTypeManager.getINSTANCE();
        Registry.ENTITY_TYPE.stream().filter(EntityType::isSummonable).forEach(entityType ->
                iffTypeManager.register(EntityType.getId(entityType),
                        new IFFType(IFFTag.UNKNOWN, entityType)));

    }
}
