package net.sistr.littlemaidrebirth.neoforge.client;

import me.shedaniel.autoconfig.AutoConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.sistr.littlemaidrebirth.config.LMRBConfig;

// Screen型を参照するラムダを含むため、クラス自体を分離してサーバー側でのクラスロードを防ぐ
public class ClientConfigScreenSetup {

    public static void register(ModContainer container) {
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (mc, parent) -> AutoConfig.getConfigScreen(LMRBConfig.class, parent).get());
    }
}
