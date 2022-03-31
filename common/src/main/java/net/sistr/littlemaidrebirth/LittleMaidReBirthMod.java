package net.sistr.littlemaidrebirth;


import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.setup.Registration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LittleMaidReBirthMod {
    public static final String MODID = "littlemaidrebirth";
    public static final Logger LOGGER = LogManager.getLogger();

    public static void init() {
        Registration.init();
        registerAttribute();
    }

    public static void registerAttribute() {
        EntityAttributeRegistry.register(Registration.LITTLE_MAID_MOB::get, LittleMaidEntity::createLittleMaidAttributes);
    }

}
