package net.sistr.littlemaidrebirth.item;

import net.minecraft.item.Item;
import net.minecraft.item.SpawnEggItem;
import net.sistr.littlemaidrebirth.setup.ModSetup;
import net.sistr.littlemaidrebirth.setup.Registration;

public class LittleMaidSpawnEggItem extends SpawnEggItem {

    public LittleMaidSpawnEggItem() {
        super(Registration.LITTLE_MAID_MOB_BEFORE, 0xFFFFFF, 0x804000,
                new Item.Settings().group(ModSetup.ITEM_GROUP));
    }

}
