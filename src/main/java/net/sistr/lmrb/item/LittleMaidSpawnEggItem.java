package net.sistr.lmrb.item;

import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.SpawnEggItem;
import net.sistr.lmrb.setup.ModSetup;
import net.sistr.lmrb.setup.Registration;

public class LittleMaidSpawnEggItem extends SpawnEggItem {

    public LittleMaidSpawnEggItem() {
        super(Registration.LITTLE_MAID_MOB, 0xFFFFFF, 0x804000,
                new FabricItemSettings().group(ModSetup.ITEM_GROUP));
    }

}
