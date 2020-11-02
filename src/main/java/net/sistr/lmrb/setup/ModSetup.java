package net.sistr.lmrb.setup;

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.mixin.object.builder.SpawnRestrictionAccessor;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;
import net.sistr.lmrb.LittleMaidReBirthMod;
import net.sistr.lmrb.entity.LittleMaidEntity;

public class ModSetup {

    public static final ItemGroup ITEM_GROUP = FabricItemGroupBuilder
            .create(new Identifier(LittleMaidReBirthMod.MODID, "common"))
            .icon(Items.CAKE::getDefaultStack)
            .build();

    public static void init() {
        Registration.init();

        FabricDefaultAttributeRegistry.register(Registration.LITTLE_MAID_MOB, LittleMaidEntity.createLittleMaidAttributes());
        SpawnRestrictionAccessor.callRegister(Registration.LITTLE_MAID_MOB, SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, LittleMaidEntity::isValidNaturalSpawn);
    }
}
