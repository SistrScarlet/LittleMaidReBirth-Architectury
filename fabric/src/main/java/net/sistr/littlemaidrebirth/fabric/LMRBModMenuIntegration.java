package net.sistr.littlemaidrebirth.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import net.sistr.littlemaidrebirth.config.LMRBConfig;

public class LMRBModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        //return LMRBClothConfigBuilder::getConfigScreen;
        return parent -> AutoConfig.getConfigScreen(LMRBConfig.class, parent).get();
    }
}
