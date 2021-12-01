package net.sistr.littlemaidrebirth;


import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.setup.Registration;

public class LittleMaidReBirthMod {
    public static final String MODID = "littlemaidrebirth";

    public static void init() {
        Registration.init();
        registerAttribute();
    }

    public static void registerAttribute() {
        EntityAttributeRegistry.register(() -> Registration.LITTLE_MAID_MOB_BEFORE, LittleMaidEntity::createLittleMaidAttributes);
    }

}
