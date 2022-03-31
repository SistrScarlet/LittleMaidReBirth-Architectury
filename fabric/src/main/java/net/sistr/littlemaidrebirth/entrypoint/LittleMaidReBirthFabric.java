package net.sistr.littlemaidrebirth.entrypoint;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import net.sistr.littlemaidrebirth.setup.ClientSetup;
import net.sistr.littlemaidrebirth.setup.ModSetup;

public class LittleMaidReBirthFabric implements ModInitializer, ClientModInitializer {

    @Override
    public void onInitialize() {
        LittleMaidReBirthMod.init();
        ModSetup.init();
    }

    @Override
    public void onInitializeClient() {
        ClientSetup.init();
    }
}
