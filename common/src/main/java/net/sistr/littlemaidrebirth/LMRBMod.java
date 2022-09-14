package net.sistr.littlemaidrebirth;


import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.sistr.littlemaidrebirth.advancement.criterion.LMRBCriteria;
import net.sistr.littlemaidrebirth.config.LMRBConfig;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.setup.Registration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LMRBMod {
    public static final String MODID = "littlemaidrebirth";
    public static final Logger LOGGER = LogManager.getLogger();
    private static ConfigHolder<LMRBConfig> CONFIG_HOLDER;

    public static void init() {
        AutoConfig.register(LMRBConfig.class, GsonConfigSerializer::new);
        CONFIG_HOLDER = AutoConfig.getConfigHolder(LMRBConfig.class);

        Registration.init();
        registerAttribute();

        LMRBCriteria.init();
    }

    public static void registerAttribute() {
        EntityAttributeRegistry.register(Registration.LITTLE_MAID_MOB, LittleMaidEntity::createLittleMaidAttributes);
    }

    public static LMRBConfig getConfig() {
        return CONFIG_HOLDER.getConfig();
    }
}
