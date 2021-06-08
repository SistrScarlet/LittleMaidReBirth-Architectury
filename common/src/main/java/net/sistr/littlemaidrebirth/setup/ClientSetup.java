package net.sistr.littlemaidrebirth.setup;

import me.shedaniel.architectury.registry.MenuRegistry;
import me.shedaniel.architectury.registry.entity.EntityRenderers;
import net.fabricmc.api.ClientModInitializer;
import net.sistr.littlemaidrebirth.client.LittleMaidScreen;
import net.sistr.littlemaidrebirth.client.MaidModelRenderer;
import net.sistr.littlemaidrebirth.network.Networking;

public class ClientSetup {

    public static void init() {
        MenuRegistry.registerScreenFactory(Registration.LITTLE_MAID_SCREEN_HANDLER.get(), LittleMaidScreen::new);

        EntityRenderers.register(Registration.LITTLE_MAID_MOB.get(), MaidModelRenderer::new);
    }

}
