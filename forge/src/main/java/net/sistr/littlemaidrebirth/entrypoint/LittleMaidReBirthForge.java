package net.sistr.littlemaidrebirth.entrypoint;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import net.sistr.littlemaidrebirth.client.MaidModelRenderer;
import net.sistr.littlemaidrebirth.config.forge.LMRBForgeConfig;
import net.sistr.littlemaidrebirth.setup.ClientSetup;
import net.sistr.littlemaidrebirth.setup.ModSetup;
import net.sistr.littlemaidrebirth.setup.Registration;

@Mod(LittleMaidReBirthMod.MODID)
public class LittleMaidReBirthForge {

    public LittleMaidReBirthForge() {
        EventBuses.registerModEventBus(LittleMaidReBirthMod.MODID, FMLJavaModLoadingContext.get().getModEventBus());
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, LMRBForgeConfig.COMMON_CONFIG);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, LMRBForgeConfig.CLIENT_CONFIG);

        LittleMaidReBirthMod.init();
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::modInit);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientInit);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::renderInit);
    }

    public void modInit(FMLCommonSetupEvent event) {
        ModSetup.init();
    }

    public void clientInit(FMLClientSetupEvent event) {
        ClientSetup.init();
    }

    //ClientSetupよりこちらの方が実行が早いため、ClientSetupからArchitecturyのメソッド登録しようとすると無視される
    public void renderInit(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Registration.LITTLE_MAID_MOB.get(), MaidModelRenderer::new);
    }

}
