package net.sistr.littlemaidrebirth.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.world.Heightmap;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.client.MaidModelRenderer;
import net.sistr.littlemaidrebirth.client.MaidSoulRenderer;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.setup.ClientSetup;
import net.sistr.littlemaidrebirth.setup.ModSetup;
import net.sistr.littlemaidrebirth.setup.Registration;
import net.sistr.littlemaidrebirth.util.SpawnRestrictionRegister;

public class LMRBFabric implements ModInitializer, ClientModInitializer {

    @Override
    public void onInitialize() {
        LMRBMod.init();
        ModSetup.init();

        if (LMRBMod.getConfig().isCanSpawn()) {
            SpawnRestrictionRegister.callRegister(null, Registration.LITTLE_MAID_MOB.get(),
                    SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    (type, world, spawnReason, pos, random) -> LittleMaidEntity.isValidNaturalSpawn(world, pos));
        }
    }

    @Override
    public void onInitializeClient() {
        ClientSetup.init();
        //Forge側でうまく登録できないため、ここで登録
        EntityRendererRegistry.register(Registration.LITTLE_MAID_MOB.get(), MaidModelRenderer::new);
        EntityRendererRegistry.register(Registration.MAID_SOUL_ENTITY.get(), MaidSoulRenderer::new);
    }
}
