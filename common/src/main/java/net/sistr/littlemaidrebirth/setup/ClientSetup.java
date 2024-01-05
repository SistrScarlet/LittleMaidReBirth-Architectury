package net.sistr.littlemaidrebirth.setup;

import me.shedaniel.architectury.registry.MenuRegistry;
import net.sistr.littlemaidrebirth.client.LittleMaidScreen;

public class ClientSetup {

    public static void init() {
        MenuRegistry.registerScreenFactory(Registration.LITTLE_MAID_SCREEN_HANDLER.get(), LittleMaidScreen::new);
    }

}
