package net.sistr.littlemaidrebirth.setup;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendereregistry.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import net.sistr.littlemaidrebirth.client.MaidModelRenderer;
import net.sistr.littlemaidrebirth.client.LittleMaidScreen;
import net.sistr.littlemaidrebirth.network.Networking;

public class ClientSetup implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Networking.INSTANCE.clientInit();

        ScreenRegistry.register(Registration.LITTLE_MAID_SCREEN_HANDLER, LittleMaidScreen::new);

        EntityRendererRegistry.INSTANCE.register(Registration.LITTLE_MAID_MOB, (a, b) -> new MaidModelRenderer(a));
    }
}