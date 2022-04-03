package net.sistr.littlemaidrebirth.entrypoint;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import net.sistr.littlemaidrebirth.client.MaidModelRenderer;
import net.sistr.littlemaidrebirth.setup.ClientSetup;
import net.sistr.littlemaidrebirth.setup.ModSetup;
import net.sistr.littlemaidrebirth.setup.Registration;

public class LittleMaidReBirthFabric implements ModInitializer, ClientModInitializer {

    @Override
    public void onInitialize() {
        LittleMaidReBirthMod.init();
        ModSetup.init();
    }

    @Override
    public void onInitializeClient() {
        ClientSetup.init();
        //Forge側でうまく登録できないため、ここで登録
        EntityRendererRegistry.register(Registration.LITTLE_MAID_MOB.get(), MaidModelRenderer::new);
    }
}
