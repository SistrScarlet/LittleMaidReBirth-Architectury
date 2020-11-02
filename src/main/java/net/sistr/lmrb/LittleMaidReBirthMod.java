package net.sistr.lmrb;

import net.fabricmc.api.ModInitializer;
import net.sistr.lmrb.setup.ModSetup;

public class LittleMaidReBirthMod implements ModInitializer {

    public static final String MODID = "littlemaidrebirth";

    @Override
    public void onInitialize() {
        ModSetup.init();
    }
}
