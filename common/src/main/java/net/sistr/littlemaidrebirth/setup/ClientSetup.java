package net.sistr.littlemaidrebirth.setup;

import dev.architectury.registry.level.entity.EntityRendererRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import net.sistr.littlemaidrebirth.client.LittleMaidScreen;
import net.sistr.littlemaidrebirth.client.MaidModelRenderer;

public class ClientSetup {

    public static void init() {
        MenuRegistry.registerScreenFactory(Registration.LITTLE_MAID_SCREEN_HANDLER.get(), LittleMaidScreen::new);

        EntityRendererRegistry.register(Registration.LITTLE_MAID_MOB.get(), MaidModelRenderer::new);
    }

}
