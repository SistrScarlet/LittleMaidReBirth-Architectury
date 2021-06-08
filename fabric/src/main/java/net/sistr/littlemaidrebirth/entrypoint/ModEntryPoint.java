package net.sistr.littlemaidrebirth.entrypoint;

import net.fabricmc.api.ModInitializer;
import net.sistr.littlemaidrebirth.LittleMaidReBirthMod;
import net.sistr.littlemaidrebirth.setup.ModSetup;

public class ModEntryPoint implements ModInitializer {

    public ModEntryPoint() {
        LittleMaidReBirthMod.init();
    }

    @Override
    public void onInitialize() {
        ModSetup.init();
    }

}
