package net.sistr.littlemaidrebirth.fabric;

import me.shedaniel.architectury.registry.entity.EntityRenderers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.world.Heightmap;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.client.MaidModelRenderer;
import net.sistr.littlemaidrebirth.client.MaidSoulRenderer;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.setup.ClientSetup;
import net.sistr.littlemaidrebirth.setup.ModSetup;
import net.sistr.littlemaidrebirth.setup.Registration;

public class LMRBFabric implements ModInitializer, ClientModInitializer {

    @Override
    public void onInitialize() {
        LMRBMod.init();
        ModSetup.init();

        SpawnRestriction.register(Registration.LITTLE_MAID_MOB.get(),
                SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, spawnReason, pos, random) -> LittleMaidEntity.isValidNaturalSpawn(world, pos));
    }

    @Override
    public void onInitializeClient() {
        ClientSetup.init();
        //Forge側でうまく登録できないため、ここで登録
        EntityRenderers.register(Registration.LITTLE_MAID_MOB.get(), MaidModelRenderer::new);
        EntityRenderers.register(Registration.MAID_SOUL_ENTITY.get(), MaidSoulRenderer::new);
    }
}
