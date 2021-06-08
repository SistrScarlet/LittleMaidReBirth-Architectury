package net.sistr.littlemaidrebirth;


import net.sistr.littlemaidmodelloader.register.AttributeRegister;
import net.sistr.littlemaidrebirth.entity.LittleMaidEntity;
import net.sistr.littlemaidrebirth.setup.Registration;

public class LittleMaidReBirthMod {
    public static final String MODID = "littlemaidrebirth";

    public static void init() {
        Registration.init();
        registerAttribute();
    }

    public static void registerAttribute() {
        AttributeRegister.register(Registration.LITTLE_MAID_MOB_BEFORE, LittleMaidEntity::createLittleMaidAttributes);
    }

}
