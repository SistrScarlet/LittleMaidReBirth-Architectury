package net.sistr.littlemaidrebirth.forge;

import me.shedaniel.architectury.platform.forge.EventBuses;
import me.shedaniel.architectury.registry.entity.EntityRenderers;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.world.Heightmap;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.client.MaidModelRenderer;
import net.sistr.littlemaidrebirth.client.MaidSoulRenderer;
import net.sistr.littlemaidrebirth.config.LMRBConfig;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.setup.ClientSetup;
import net.sistr.littlemaidrebirth.setup.ModSetup;
import net.sistr.littlemaidrebirth.setup.Registration;

@Mod(LMRBMod.MODID)
public class LMRBForge {

    public LMRBForge() {
        EventBuses.registerModEventBus(LMRBMod.MODID, FMLJavaModLoadingContext.get().getModEventBus());

        LMRBMod.init();

        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY,
                () -> (client, parent) -> AutoConfig.getConfigScreen(LMRBConfig.class, parent).get());

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::modInit);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientInit);
    }

    public void modInit(FMLCommonSetupEvent event) {
        ModSetup.init();
        SpawnRestriction.register(Registration.LITTLE_MAID_MOB.get(),
                SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, spawnReason, pos, random) -> LittleMaidEntity.isValidNaturalSpawn(world, pos));
    }

    public void clientInit(FMLClientSetupEvent event) {
        ClientSetup.init();
        renderInit();
    }

    //ClientSetupよりこちらの方が実行が早いため、ClientSetupからArchitecturyのメソッド登録しようとすると無視される
    public void renderInit() {
        EntityRenderers.register(Registration.LITTLE_MAID_MOB.get(), MaidModelRenderer::new);
        EntityRenderers.register(Registration.MAID_SOUL_ENTITY.get(), MaidSoulRenderer::new);
    }

}
