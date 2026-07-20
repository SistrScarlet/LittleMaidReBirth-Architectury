package net.sistr.littlemaidrebirth.neoforge;

import net.minecraft.entity.SpawnLocationTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.sistr.littlemaidrebirth.LMRBMod;
import net.sistr.littlemaidrebirth.advancement.criterion.LMRBCriteria;
import net.sistr.littlemaidrebirth.client.key.LMKeys;
import net.sistr.littlemaidrebirth.client.renderer.MaidModelRenderer;
import net.sistr.littlemaidrebirth.client.renderer.MaidSoulRenderer;
import net.sistr.littlemaidrebirth.client.screen.LittleMaidScreen;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.neoforge.client.ClientConfigScreenSetup;
import net.sistr.littlemaidrebirth.setup.ModSetup;
import net.sistr.littlemaidrebirth.setup.Registration;

@Mod(LMRBMod.MODID)
public class LMRBNeoForge {

    public LMRBNeoForge(IEventBus modBus, ModContainer container) {
        LMRBMod.init();

        if (FMLEnvironment.dist.isClient()) {
            ClientConfigScreenSetup.register(container);
        }

        modBus.addListener(this::registerCriteria);
        modBus.addListener(this::modInit);
        modBus.addListener(this::spawnRestrictionInit);
        modBus.addListener(this::clientInit);
        modBus.addListener(this::menuScreenInit);
        modBus.addListener(this::renderInit);
    }

    public void registerCriteria(RegisterEvent event) {
        event.register(
                RegistryKeys.CRITERION,
                helper -> {
                    helper.register(
                            Identifier.of(LMRBMod.MODID, "contract_maid"),
                            LMRBCriteria.CONTRACT_MAID);
                    helper.register(
                            Identifier.of(LMRBMod.MODID, "resurrect_maid"),
                            LMRBCriteria.RESURRECT_MAID);
                });
    }

    public void modInit(FMLCommonSetupEvent event) {
        ModSetup.init();
    }

    public void spawnRestrictionInit(RegisterSpawnPlacementsEvent event) {
        event.register(
                Registration.LITTLE_MAID_MOB.get(),
                SpawnLocationTypes.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, spawnReason, pos, random) ->
                        LittleMaidEntity.isValidNaturalSpawn(world, pos),
                RegisterSpawnPlacementsEvent.Operation.OR);
    }

    public void clientInit(FMLClientSetupEvent event) {
        LMKeys.init();
    }

    // FMLClientSetupEvent より後に発火するため、スクリーン登録はこちらで行う
    public void menuScreenInit(RegisterMenuScreensEvent event) {
        event.register(Registration.LITTLE_MAID_SCREEN_HANDLER.get(), LittleMaidScreen::new);
    }

    // ClientSetupよりこちらの方が実行が早いため、ClientSetupからArchitecturyのメソッド登録しようとすると無視される
    public void renderInit(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Registration.LITTLE_MAID_MOB.get(), MaidModelRenderer::new);
        event.registerEntityRenderer(Registration.MAID_SOUL_ENTITY.get(), MaidSoulRenderer::new);
    }
}
