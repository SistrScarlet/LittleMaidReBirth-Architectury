package net.sistr.littlemaidrebirth.entrypoint;

import net.fabricmc.api.ClientModInitializer;
import net.sistr.littlemaidrebirth.setup.ClientSetup;

public class ClientEntryPoint implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientSetup.init();
    }
}
