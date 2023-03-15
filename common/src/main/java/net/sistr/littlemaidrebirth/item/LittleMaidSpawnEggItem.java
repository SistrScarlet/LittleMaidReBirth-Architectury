package net.sistr.littlemaidrebirth.item;

import dev.architectury.core.item.ArchitecturySpawnEggItem;
import net.minecraft.item.Item;
import net.sistr.littlemaidrebirth.setup.ModSetup;
import net.sistr.littlemaidrebirth.setup.Registration;

public class LittleMaidSpawnEggItem extends ArchitecturySpawnEggItem {

    public LittleMaidSpawnEggItem() {
        super(Registration.LITTLE_MAID_MOB, 0xFFFFFF, 0x804000,
                new Item.Settings().arch$tab(ModSetup.ITEM_GROUP));
    }

}
